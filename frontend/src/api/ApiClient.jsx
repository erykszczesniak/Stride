import axios from "axios";

const BASE_API_URL = process.env.REACT_APP_API_URL || '/api/v1';

const apiClient = axios.create({
  baseURL: BASE_API_URL,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add request interceptor for auth token if needed
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;
