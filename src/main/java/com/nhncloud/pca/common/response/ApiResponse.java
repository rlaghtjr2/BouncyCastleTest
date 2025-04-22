package com.nhncloud.pca.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private ApiResponseHeader header;
    private T body;

    public static <T> ApiResponse success(T body) {
        return success("success", body);
    }

    public static <T> ApiResponse success(String resultMessage, T body) {
        ApiResponseHeader header = new ApiResponseHeader(true, 0, resultMessage);
        return new ApiResponse<>(header, body);
    }

    public static ApiResponse fail(int resultCode, String resultMessage) {
        ApiResponseHeader header = new ApiResponseHeader(false, resultCode, resultMessage);
        return new ApiResponse<>(header, null);
    }
}
