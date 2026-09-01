/** Error envelope produced by the backend GlobalExceptionHandler. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export interface UploadResult {
  url: string;
  fileName: string;
  size: number;
}
