package com.example.backend.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostResponse {
    private String id;
    private String userId;
    private String userName;
    private String userProfilePicture;
    private String content;
    private String videoUrl;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> mediaIds = new ArrayList<>();
    private int likes;
    private List<String> comments = new ArrayList<>();
    private LocalDateTime createdAt;

    public PostResponse() {
    }

    public PostResponse(Post post) {
        this.id = post.getId();
        this.userId = post.getUserId();
        this.content = post.getContent();
        this.videoUrl = post.getVideoUrl();
        this.imageUrls = post.getImageUrls();
        this.mediaIds = post.getMediaIds();
        this.likes = post.getLikes();
        this.comments = post.getComments();
        this.createdAt = post.getCreatedAt();
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserProfilePicture() {
        return userProfilePicture;
    }

    public void setUserProfilePicture(String userProfilePicture) {
        this.userProfilePicture = userProfilePicture;
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