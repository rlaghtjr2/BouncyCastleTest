package com.nhncloud.pca.model.acme;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAccountBinding {
    private String kid; // Key Identifier
    private String hmac; // HMAC value
    private String alg; // Algorithm (e.g., "HS256")
}