import axios from "axios";
export const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
  timeout: 20000,
});
export const errorMessage = (e) =>
  e.response?.data?.errors?.join("; ") ||
  e.response?.data?.message ||
  "Unable to connect. Please try again.";
