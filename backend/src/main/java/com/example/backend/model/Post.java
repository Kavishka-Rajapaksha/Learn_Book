package com.example.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    private String userId;
    private String content;
    private String videoUrl;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> mediaIds = new ArrayList<>(); // Store GridFS IDs
    private int likes = 0;
    private List<String> comments = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();

    public Post() {
    }

    // Constructor for PostResponse
    public Post(String id, String userId, String content, String videoUrl, List<String> imageUrls,
            List<String> mediaIds, int likes, List<String> comments, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.videoUrl = videoUrl;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.mediaIds = mediaIds != null ? mediaIds : new ArrayList<>();
        this.likes = likes;
        this.comments = comments != null ? comments : new ArrayList<>();
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<String> getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(List<String> mediaIds) {
        this.mediaIds = mediaIds;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}