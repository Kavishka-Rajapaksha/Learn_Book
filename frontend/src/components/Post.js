import React, { useState, useEffect } from "react";
import axiosInstance from "../utils/axios";

function Post({ post, onPostDeleted, onPostUpdated }) {
  const [showMenu, setShowMenu] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(post.content);
  const [editImages, setEditImages] = useState([]);
  const [editPreviewUrls, setEditPreviewUrls] = useState([]);
  const [deleting, setDeleting] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [mediaUrls, setMediaUrls] = useState({});
  const user = JSON.parse(localStorage.getItem("user"));

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  // Construct full URL for relative paths
  const getFullUrl = (url) => {
    if (!url) return "";
    if (url.startsWith("http://") || url.startsWith("https://")) return url;
    return `${window.location.protocol}//${window.location.hostname}:8081${url}`;
  };

  // Fetch media as a blob and create object URL
  const getMediaUrl = async (mediaId, originalUrl) => {
    try {
      const response = await axiosInstance.get(`/api/media/${mediaId}`, {
        responseType: "blob",
      });
      if (response.data instanceof Blob) {
        return URL.createObjectURL(response.data);
      }
      console.warn(`Invalid blob for media ${mediaId}, using fallback URL`);
      return getFullUrl(originalUrl);
    } catch (error) {
      console.error(`Error loading media ${mediaId}:`, error);
      return getFullUrl(originalUrl); // Fallback to original URL
    }
  };

  // Load all media URLs
  useEffect(() => {
    const newMediaUrls = {};
    const loadMedia = async () => {

      // Handle video
      if (post.videoUrl) {
        const mediaId = post.videoUrl.split("/").pop();
        newMediaUrls.video = await getMediaUrl(mediaId, post.videoUrl);
      }

      // Handle images
      if (post.imageUrls?.length) {
        const imagePromises = post.imageUrls.map(async (url) => {
          const mediaId = url.split("/").pop();
          const mediaUrl = await getMediaUrl(mediaId, url);
          return { mediaId, mediaUrl };
        });
        const resolvedImages = await Promise.all(imagePromises);
        resolvedImages.forEach(({ mediaId, mediaUrl }) => {
          newMediaUrls[mediaId] = mediaUrl;
        });
      }

      setMediaUrls(newMediaUrls);
    };

    loadMedia();

    // Cleanup object URLs
    return () => {
      Object.values(newMediaUrls).forEach((url) => {
        if (url.startsWith("blob:")) URL.revokeObjectURL(url);
      });
    };
  }, [post.videoUrl, post.imageUrls]);

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this post?")) return;

    try {
      setDeleting(true);
      await axiosInstance.delete(`/api/posts/${post.id}?userId=${user.id}`);
      onPostDeleted?.(post.id);
      setShowMenu(false);
    } catch (error) {
      console.error("Error deleting post:", error);
      alert("Failed to delete post. Please try again.");
    } finally {
      setDeleting(false);
    }
  };

  const handleEdit = () => {
    setIsEditing(true);
    setShowMenu(false);
    setEditContent(post.content);
    setEditImages([]);
    setEditPreviewUrls([]);
  };

  const handleImageChange = (e) => {
    const files = Array.from(e.target.files);
    setEditImages(files);
    const urls = files.map((file) => URL.createObjectURL(file));
    setEditPreviewUrls(urls);
  };

  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    if (!editContent.trim() && editImages.length === 0) return;

    try {
      setUpdating(true);
      const formData = new FormData();
      formData.append("userId", user.id);
      formData.append("content", editContent);
      editImages.forEach((image) => formData.append("images", image));

      const response = await axiosInstance.put(`/api/posts/${post.id}`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      onPostUpdated?.(response.data);
      setIsEditing(false);

      // Clean up preview URLs
      editPreviewUrls.forEach((url) => URL.revokeObjectURL(url));
    } catch (error) {
      console.error("Error updating post:", error);
      alert(error.response?.data || "Failed to update post. Please try again.");
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md mb-4 p-4 relative">
      {user && user.id === post.userId && (
        <div className="absolute top-4 right-4">
          <button
            onClick={() => setShowMenu(!showMenu)}
            className="p-1 hover:bg-gray-100 rounded-full"
            disabled={deleting || updating}
          >
            <svg
              className="w-6 h-6 text-gray-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"
              />
            </svg>
          </button>
          {showMenu && (
            <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg z-50">
              <button
                onClick={handleEdit}
                className="w-full text-left px-4 py-2 text-sm text-blue-600 hover:bg-gray-100"
              >
                Edit Post
              </button>
              <button
                onClick={handleDelete}
                disabled={deleting}
                className={`w-full text-left px-4 py-2 text-sm ${
                  deleting ? "text-gray-400" : "text-red-600 hover:bg-gray-100"
                }`}
              >
                {deleting ? "Deleting..." : "Delete Post"}
              </button>
            </div>
          )}
        </div>
      )}

      <div className="flex items-center mb-4">
        <div className="w-10 h-10 rounded-full bg-gray-300 flex items-center justify-center mr-3">
          {post.userProfilePicture ? (
            <img
              src={post.userProfilePicture}
              alt={post.userName}
              className="w-full h-full rounded-full object-cover"
            />
          ) : (
            <span className="text-lg font-semibold">
              {post.userName?.charAt(0)}
            </span>
          )}
        </div>
        <div>
          <h3 className="font-semibold">{post.userName || "Unknown User"}</h3>
          <p className="text-sm text-gray-500">{formatDate(post.createdAt)}</p>
        </div>
      </div>

      {isEditing ? (
        <form onSubmit={handleUpdateSubmit} className="mt-4">
          <textarea
            className="w-full p-3 border rounded-lg mb-4 focus:outline-none focus:ring-2 focus:ring-blue-500"
            rows="3"
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            disabled={updating}
          />
          {editPreviewUrls.length > 0 && (
            <div className="mb-4 flex flex-wrap gap-2">
              {editPreviewUrls.map((url, index) => (
                <img
                  key={index}
                  src={url}
                  alt={`Preview ${index}`}
                  className="w-24 h-24 object-cover rounded-lg"
                />
              ))}
            </div>
          )}
          <div className="flex justify-between items-center">
            <input
              type="file"
              multiple
              accept="image/*"
              onChange={handleImageChange}
              className="hidden"
              id="edit-image-input"
              disabled={updating}
            />
            <label
              htmlFor="edit-image-input"
              className="cursor-pointer text-blue-500 hover:text-blue-600"
            >
              Add Images
            </label>
            <div className="space-x-2">
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded"
                disabled={updating}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={
                  updating || (!editContent.trim() && editImages.length === 0)
                }
                className={`px-4 py-2 rounded text-white ${
                  updating || (!editContent.trim() && editImages.length === 0)
                    ? "bg-gray-400 cursor-not-allowed"
                    : "bg-blue-500 hover:bg-blue-600"
                }`}
              >
                {updating ? "Updating..." : "Save Changes"}
              </button>
            </div>
          </div>
        </form>
      ) : (
        <>
          <p className="mb-4">{post.content}</p>

          {post.videoUrl && (
            <div className="mb-4">
              <video
                src={mediaUrls.video || getFullUrl(post.videoUrl)}
                className="max-h-96 w-full object-contain"
                controls
                playsInline
                preload="metadata"
                onError={(e) => {
                  console.error("Video loading error:", e);
                  e.target.src = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPlZpZGVvIEZhaWxlZCB0byBMb2FkPC90ZXh0Pjwvc3ZnPg==";
                }}
              />
            </div>
          )}

          {post.imageUrls?.map((url, index) => {
            const mediaId = url.split("/").pop();
            return (
              <img
                key={index}
                src={mediaUrls[mediaId] || getFullUrl(url)}
                alt={`Post image ${index + 1}`}
                className="max-h-96 object-contain mb-4 w-full"
                onError={(e) => {
                  console.error("Image failed to load:", url);
                  e.target.src = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIyNCIgZmlsbD0iIzY2NiIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPkltYWdlIEZhaWxlZCB0byBMb2FkPC90ZXh0Pjwvc3ZnPg==";
                }}
              />
            );
          })}

          <div className="flex items-center space-x-6 border-t pt-4">
            <button className="flex items-center space-x-2 text-gray-500 hover:text-red-500">
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                />
              </svg>
              <span>{post.likes}</span>
            </button>
            <button className="flex items-center space-x-2 text-gray-500 hover:text-blue-500">
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                />
              </svg>
              <span>{post.comments?.length || 0}</span>
            </button>
            <button className="flex items-center space-x-2 text-gray-500 hover:text-green-500">
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"
                />
              </svg>
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default Post;