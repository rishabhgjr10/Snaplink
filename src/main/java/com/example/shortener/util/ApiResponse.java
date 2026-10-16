package com.example.shortener.util;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int status;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data, int status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public static <T> ApiResponse<T> success(T data, String message, int status) {
        return new ApiResponse<T>(true, message, data, status);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return new ApiResponse<T>(false, message, null, status);
    }
}
