import React, { useState, useEffect } from "react";
import axiosInstance from "../utils/axios";

function CreatePost({ onPostCreated }) {
  const [content, setContent] = useState("");
  const [images, setImages] = useState([]);
  const [video, setVideo] = useState(null);
  const [previewUrls, setPreviewUrls] = useState([]);
  const [videoPreviewUrl, setVideoPreviewUrl] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleImageChange = (e) => {
    const files = Array.from(e.target.files);
    setImages(files);
    const urls = files.map((file) => URL.createObjectURL(file));
    setPreviewUrls(urls);
    setError("");
  };

  const handleVideoChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Check file size (max 15MB)
    if (file.size > 15 * 1024 * 1024) {
      setError("Video size must be less than 15MB");
      return;
    }

    // Check file type
    if (!["video/mp4", "video/quicktime"].includes(file.type)) {
      setError("Only MP4 and QuickTime videos are supported");
      return;
    }

    // Create a URL for preview
    const videoURL = URL.createObjectURL(file);

    // Create a video element to check duration
    const videoElement = document.createElement("video");
    videoElement.preload = "metadata";

    videoElement.onloadedmetadata = () => {
      window.URL.revokeObjectURL(videoElement.src);

      if (videoElement.duration > 30) {
        setError("Video must be 30 seconds or less");
        return;
      }

      // Add a timestamp to the file name to avoid caching issues
      const timestamp = new Date().getTime();
      const newFile = new File([file], `video-${timestamp}-${file.name}`, {
        type: file.type,
      });

      setVideo(newFile);
      setVideoPreviewUrl(videoURL);
      // Clear any images when video is selected
      setImages([]);
      setPreviewUrls([]);
      setError("");
    };

    videoElement.onerror = () => {
      setError("Could not load video. Please try a different file.");
      window.URL.revokeObjectURL(videoElement.src);
    };

    videoElement.src = videoURL;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setProgress(0);
    setIsLoading(true);

    const formData = new FormData();
    const user = JSON.parse(localStorage.getItem("user"));

    if (!user || !user.id) {
      setError("Please log in to create a post");
      setIsLoading(false);
      return;
    }

    formData.append("userId", user.id);
    formData.append("content", content);

    if (video) {
      formData.append("video", video);
      console.log("Adding video to post:", video.name, video.size, video.type);
    } else if (images.length > 0) {
      images.forEach((image) => formData.append("images", image));
      console.log("Adding images to post:", images.length);
    }

    try {
      const response = await axiosInstance.post("/api/posts", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setProgress(percentCompleted);
        },
      });

      console.log("Post created successfully:", response.data);

      // Reset form
      setContent("");
      setImages([]);
      setVideo(null);
      setPreviewUrls([]);
      setVideoPreviewUrl("");
      setProgress(0);

      if (onPostCreated) {
        onPostCreated(response.data);
      }

      // Clean up preview URLs
      previewUrls.forEach((url) => URL.revokeObjectURL(url));
      if (videoPreviewUrl) URL.revokeObjectURL(videoPreviewUrl);

      // Show success message
      alert("Post created successfully!");
    } catch (error) {
      console.error("Error creating post:", error);
      let errorMessage = "Failed to create post. Please try again.";

      if (error.response) {
        console.log("Error response:", error.response);
        errorMessage = error.response.data || errorMessage;
      }

      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  // Clean up preview URLs on unmount
  useEffect(() => {
    return () => {
      previewUrls.forEach((url) => URL.revokeObjectURL(url));
      if (videoPreviewUrl) URL.revokeObjectURL(videoPreviewUrl);
    };
  }, [previewUrls, videoPreviewUrl]);

  return (
    <div className="bg-white rounded-lg shadow-md p-4 mb-4">
      <h2 className="text-xl font-semibold mb-4">Create Post</h2>
      {error && <p className="text-red-500 mb-4">{error}</p>}
      <form onSubmit={handleSubmit}>
        <textarea
          className="w-full p-3 border rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-blue-500"
          rows="3"
          placeholder="What's on your mind?"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={isLoading}
        />

        <div className="mb-4 flex flex-wrap gap-2">
          {previewUrls.map((url, index) => (
            <img
              key={index}
              src={url}
              alt={`Preview ${index}`}
              className="w-24 h-24 object-cover rounded-lg"
            />
          ))}
          {videoPreviewUrl && (
            <video
              src={videoPreviewUrl}
              className="w-full max-h-96 object-contain"
              controls
            />
          )}
        </div>

        <div className="flex justify-between items-center">
          <div className="flex gap-4">
            <label className="cursor-pointer">
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={handleImageChange}
                className="hidden"
                disabled={!!video || isLoading}
              />
              <svg
                className="w-8 h-8 text-blue-500 hover:text-blue-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
            </label>

            <label className="cursor-pointer">
              <input
                type="file"
                accept="video/mp4,video/quicktime"
                onChange={handleVideoChange}
                className="hidden"
                disabled={images.length > 0 || isLoading}
              />
              <svg
                className="w-8 h-8 text-blue-500 hover:text-blue-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"
                />
              </svg>
            </label>
          </div>

          <button
            type="submit"
            disabled={isLoading || (!content && !video && images.length === 0)}
            className={`px-6 py-2 rounded-lg font-semibold text-white
              ${
                isLoading || (!content && !video && images.length === 0)
                  ? "bg-gray-300 cursor-not-allowed"
                  : "bg-blue-500 hover:bg-blue-600"
              }`}
          >
            {isLoading ? "Posting..." : "Post"}
          </button>
        </div>
      </form>

      {/* Add progress bar when uploading */}
      {isLoading && progress > 0 && (
        <div className="w-full h-2 bg-gray-200 rounded-full mb-4">
          <div
            className="h-full bg-blue-500 rounded-full transition-all duration-300"
            style={{ width: `${progress}%` }}
          ></div>
          <p className="text-sm text-center mt-1">Uploading: {progress}%</p>
        </div>
      )}
    </div>
  );
}

export default CreatePost;
