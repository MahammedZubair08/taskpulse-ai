package com.zubair.taskpulse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response wrapper.
 * 
 * Provides a consistent response format for all API endpoints.
 * Includes success/failure status, message, and optional data payload.
 * 
 * @param <T> Type of data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * HTTP status code.
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * Response message describing the result.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Whether the operation was successful.
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Optional data payload.
     * Only included if present (NonNull).
     */
    @JsonProperty("data")
    private T data;

    /**
     * Timestamp when the response was generated.
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Create a successful response with data.
     * 
     * @param message response message
     * @param data response data
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .success(true)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create a successful response without data.
     * 
     * @param message response message
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .success(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create an error response.
     * 
     * @param status HTTP status code
     * @param message error message
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(Integer status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .success(false)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Create an error response with data.
     * 
     * @param status HTTP status code
     * @param message error message
     * @param data error details or payload
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(Integer status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .success(false)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
