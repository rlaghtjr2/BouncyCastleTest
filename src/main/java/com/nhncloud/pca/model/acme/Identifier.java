package com.nhncloud.pca.model.acme;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Identifier {
    public static final String DNS = "dns";

    private String type = DNS;
    private String value;
}
