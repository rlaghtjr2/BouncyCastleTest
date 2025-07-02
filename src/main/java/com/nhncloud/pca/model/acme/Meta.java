package com.nhncloud.pca.model.acme;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Meta {
    private String termsofService;
    private String website = "https://fmn.nhnent.com";
    private List<String> caaIdentities;
    private boolean externalAccountRequired = false;
}
