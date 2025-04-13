import axios from "axios";

const axiosInstance = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
});

axiosInstance.interceptors.request.use((config) => {
  const user = JSON.parse(localStorage.getItem("user"));
  if (user) {
    config.headers.Authorization = `Bearer ${user.token}`;
  }

  // For media requests, ensure proper headers
  if (config.url && config.url.includes("/api/media/")) {
    config.responseType = "blob";
  }

  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403) {
      console.error("Authentication error:", error);
    }

    // Log CORS errors specifically
    if (error.message && error.message.includes("CORS")) {
      console.error("CORS Error:", error);
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
