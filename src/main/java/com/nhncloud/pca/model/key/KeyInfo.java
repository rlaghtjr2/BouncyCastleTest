package com.nhncloud.pca.model.key;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyInfo {
    private String algorithm;
    private Integer keySize;
}
