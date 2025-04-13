package com.example.backend.controller;

import com.example.backend.model.PostResponse;
import com.example.backend.service.PostService;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSBucket;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class PostController {
    private static final Logger logger = Logger.getLogger(PostController.class.getName());
    private final PostService postService;
    private final GridFSBucket gridFSBucket;

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
            ObjectId objectId = new ObjectId(mediaId);

            // Check if file exists before opening stream
            if (gridFSBucket.find(new org.bson.Document("_id", objectId)).first() == null) {
                logger.warning("Media not found with ID: " + mediaId);
                return ResponseEntity.notFound().build();
            }

            GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(objectId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; // Increased buffer size
            int bytesRead;

            while ((bytesRead = downloadStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Get metadata from the grid fs file
            org.bson.Document metadata = downloadStream.getGridFSFile().getMetadata();
            String filename = downloadStream.getGridFSFile().getFilename();
            downloadStream.close();
            byte[] data = outputStream.toByteArray();

            // Get content type from metadata
            String contentType = null;
            try {
                if (metadata != null) {
                    contentType = metadata.getString("contentType");
                }
            } catch (Exception e) {
                logger.warning("Failed to get content type: " + e.getMessage());
            }

            if (contentType == null) {
                // Try to determine content type from filename
                if (filename != null && (filename.endsWith(".mp4") || filename.contains("mp4"))) {
                    contentType = "video/mp4";
                } else if (filename != null && (filename.endsWith(".mov") || filename.contains("quicktime"))) {
                    contentType = "video/quicktime";
                } else if (filename != null && (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
                    contentType = "image/jpeg";
                } else if (filename != null && filename.endsWith(".png")) {
                    contentType = "image/png";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            logger.info("Serving media: " + mediaId + " with content type: " + contentType);

            // Don't set Access-Control-Allow-Origin here to avoid duplicate headers
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(data.length)
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .header("Accept-Ranges", "bytes")
                    .body(new ByteArrayResource(data));
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid media ID format: " + mediaId);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.severe("Error retrieving media " + mediaId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }
}