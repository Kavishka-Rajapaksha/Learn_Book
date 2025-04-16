package com.example.backend.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.model.PostResponse;
import com.example.backend.service.PostService;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
public class PostController {
    private static final Logger logger = Logger.getLogger(PostController.class.getName());
    private final PostService postService;
    private final GridFSBucket gridFSBucket;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Autowired
    public PostController(PostService postService, GridFSBucket gridFSBucket) {
        this.postService = postService;
        this.gridFSBucket = gridFSBucket;
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestParam("userId") String userId,
            @RequestParam("content") String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "video", required = false) MultipartFile video) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest().body("User ID is required");
            }

            logger.info("Creating post for user: " + userId);
            if (video != null) {
                logger.info("Video included: " + video.getOriginalFilename() +
                        ", size: " + video.getSize() +
                        ", contentType: " + video.getContentType());
            }

            PostResponse post = postService.createPost(userId, content, images, video);
            logger.info("Post created successfully with ID: " + post.getId());
            return ResponseEntity.ok(post);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid request data: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.severe("Error creating post: " + e.getMessage());
            e.printStackTrace(); // Log full stack trace
            return ResponseEntity.badRequest().body("Failed to create post: " + e.getMessage());
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @GetMapping("/posts/user/{userId}")
    public ResponseEntity<List<PostResponse>> getUserPosts(@PathVariable String userId) {
        return ResponseEntity.ok(postService.getUserPosts(userId));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable String postId,
            @RequestParam String userId) {
        try {
            postService.deletePost(postId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<?> updatePost(
            @PathVariable String postId,
            @RequestParam String userId,
            @RequestParam String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        try {
            PostResponse post = postService.updatePost(postId, userId, content, images);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<Resource> getMedia(@PathVariable String mediaId) {
        try {
            logger.info("Fetching media with ID: " + mediaId);

            // Check if file exists in local storage first
            Path localFilePath = Paths.get("backend", "uploads", mediaId);
            if (Files.exists(localFilePath)) {
                logger.info("Found media in local storage: " + localFilePath);
                byte[] data = Files.readAllBytes(localFilePath);
                String contentType = determineContentType(localFilePath.getFileName().toString(), null);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(data.length);
                headers.setCacheControl(CacheControl.noCache().getHeaderValue());
                headers.setPragma("no-cache");
                headers.setExpires(0L);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

                return ResponseEntity
                        .status(HttpStatus.OK)
                        .headers(headers)
                        .body(new ByteArrayResource(data));
            }

            // If not in local storage, fallback to GridFS
            // Validate ObjectId format
            if (!ObjectId.isValid(mediaId)) {
                logger.warning("Invalid media ID format: " + mediaId);
                return ResponseEntity.badRequest().build();
            }

            ObjectId objectId = new ObjectId(mediaId);
            GridFSFile file = gridFSBucket.find(new org.bson.Document("_id", objectId)).first();

            if (file == null) {
                logger.warning("Media not found with ID: " + mediaId);
                return ResponseEntity.notFound().build();
            }

            try (GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(objectId);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = downloadStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                byte[] data = outputStream.toByteArray();

                String contentType = determineContentType(file.getFilename(), file.getMetadata());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentLength(data.length);
                headers.setCacheControl(CacheControl.noCache().getHeaderValue());
                headers.setPragma("no-cache");
                headers.setExpires(0L);
                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

                return ResponseEntity
                        .status(HttpStatus.OK)
                        .headers(headers)
                        .body(new ByteArrayResource(data));
            }
        } catch (IOException e) {
            logger.severe("Error reading file from local storage: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid media ID: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.severe("Error retrieving media: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filename, org.bson.Document metadata) {
        // Try to get from metadata first
        if (metadata != null && metadata.containsKey("contentType")) {
            return metadata.getString("contentType");
        }

        if (metadata != null && metadata.getString("type") != null) {
            switch (metadata.getString("type")) {
                case "image":
                    return "image/jpeg";
                case "video":
                    return "video/mp4";
            }
        }

        // Fallback to filename extension
        if (filename != null) {
            filename = filename.toLowerCase();
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (filename.endsWith(".png")) {
                return "image/png";
            }
            if (filename.endsWith(".mp4")) {
                return "video/mp4";
            }
            if (filename.endsWith(".mov")) {
                return "video/quicktime";
            }
            if (filename.endsWith(".gif")) {
                return "image/gif";
            }
            if (filename.endsWith(".webp")) {
                return "image/webp";
            }
        }

        // Default fallback
        return "application/octet-stream";
    }
}