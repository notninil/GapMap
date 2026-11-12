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
// Quiz drafts live in sessionStorage; never leave one behind for the next person on this device.
export function clearDrafts() {
  try {
    for (const key of Object.keys(sessionStorage))
      if (key.startsWith("gapmap-")) sessionStorage.removeItem(key);
  } catch {}
}
