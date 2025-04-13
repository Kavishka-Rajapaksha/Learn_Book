package com.example.backend.service;

import com.example.backend.model.Post;
import com.example.backend.model.PostResponse;
import com.example.backend.model.User;
import com.example.backend.repository.PostRepository;
import com.example.backend.repository.UserRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final GridFSBucket gridFSBucket;

    private static final int MAX_VIDEO_SIZE_MB = 15; // 15MB
    private static final List<String> ALLOWED_VIDEO_TYPES = List.of("video/mp4", "video/quicktime");
    private static final int MAX_VIDEO_DURATION_SECONDS = 30;

    @Autowired
    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            MongoTemplate mongoTemplate) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
        this.gridFSBucket = GridFSBuckets.create(mongoTemplate.getDb(), "media");
    }

    private User getUserDetails(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private PostResponse convertToPostResponse(Post post) {
        PostResponse response = new PostResponse(post);
        User user = getUserDetails(post.getUserId());
        response.setUserName(user.getFirstName() + " " + user.getLastName());
        response.setUserProfilePicture(user.getProfilePicture());
        return response;
    }

    public PostResponse createPost(String userId, String content, List<MultipartFile> images, MultipartFile video) {
        if ((video == null && (images == null || images.isEmpty())) && content.isEmpty()) {
            throw new IllegalArgumentException("Post must have content, images, or a video");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setLikes(0);
        post.setComments(new ArrayList<>());
        List<String> mediaIds = new ArrayList<>();

        try {
            // Handle video upload
            if (video != null && !video.isEmpty()) {
                validateVideo(video);
                String videoId = saveMedia(video, "video");
                mediaIds.add(videoId);
                post.setVideoUrl("/api/media/" + videoId); // URL for retrieval
            }

            // Handle image uploads
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    if (!image.getContentType().startsWith("image/")) {
                        throw new IllegalArgumentException("Only image files are supported");
                    }
                    String imageId = saveMedia(image, "image");
                    mediaIds.add(imageId);
                }
                post.setImageUrls(mediaIds.stream()
                        .map(id -> "/api/media/" + id)
                        .collect(Collectors.toList()));
            }

            post.setMediaIds(mediaIds); // Store GridFS IDs
            Post savedPost = postRepository.save(post);
            return convertToPostResponse(savedPost);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save media: " + e.getMessage());
        }
    }

    private void validateVideo(MultipartFile video) {
        if (!ALLOWED_VIDEO_TYPES.contains(video.getContentType())) {
            throw new IllegalArgumentException(
                    "Invalid video format. Allowed formats: " + String.join(", ", ALLOWED_VIDEO_TYPES));
        }
        if (video.getSize() > MAX_VIDEO_SIZE_MB * 1024 * 1024) {
            throw new IllegalArgumentException("Video size must be less than " + MAX_VIDEO_SIZE_MB + "MB");
        }
        // Note: Duration validation requires FFmpeg or external library.
        // For simplicity, assuming frontend enforces 30s limit.
        // If needed, reintroduce JAVE or use FFmpeg CLI via ProcessBuilder.
    }

    private String saveMedia(MultipartFile file, String type) throws IOException {
        GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(new org.bson.Document("type", type));
        ObjectId fileId = gridFSBucket.uploadFromStream(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "media_" + type,
                file.getInputStream(),
                options);
        return fileId.toHexString();
    }

    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return posts.stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }

    public List<PostResponse> getUserPosts(String userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return posts.stream()
                .map(this::convertToPostResponse)
                .collect(Collectors.toList());
    }

    public void deletePost(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own posts");
        }

        // Delete associated media from GridFS
        if (post.getMediaIds() != null) {
            for (String mediaId : post.getMediaIds()) {
                try {
                    gridFSBucket.delete(new ObjectId(mediaId));
                } catch (Exception e) {
                    System.err.println("Failed to delete media: " + mediaId);
                }
            }
        }

        postRepository.deleteById(postId);
    }

    public PostResponse updatePost(String postId, String userId, String content, List<MultipartFile> images) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        post.setContent(content);
        List<String> mediaIds = new ArrayList<>(post.getMediaIds() != null ? post.getMediaIds() : new ArrayList<>());

        try {
            if (images != null && !images.isEmpty()) {
                // Delete old media
                if (!mediaIds.isEmpty()) {
                    for (String mediaId : mediaIds) {
                        try {
                            gridFSBucket.delete(new ObjectId(mediaId));
                        } catch (Exception e) {
                            System.err.println("Failed to delete old media: " + mediaId);
                        }
                    }
                    mediaIds.clear();
                }

                // Save new images
                for (MultipartFile image : images) {
                    if (!image.getContentType().startsWith("image/")) {
                        throw new IllegalArgumentException("Only image files are supported");
                    }
                    String imageId = saveMedia(image, "image");
                    mediaIds.add(imageId);
                }
                post.setImageUrls(mediaIds.stream()
                        .map(id -> "/api/media/" + id)
                        .collect(Collectors.toList()));
            }

            post.setMediaIds(mediaIds);
            Post updatedPost = postRepository.save(post);
            return convertToPostResponse(updatedPost);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update media: " + e.getMessage());
        }
    }
}