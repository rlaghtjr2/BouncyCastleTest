package com.nhncloud.pca.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseHeader {
    @JsonProperty(value = "isSuccessful")
    private boolean successful;
    private int resultCode;
    private String resultMessage;
}
