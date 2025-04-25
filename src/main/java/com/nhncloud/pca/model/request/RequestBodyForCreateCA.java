package com.nhncloud.pca.model.request;

import lombok.Data;

import java.util.List;

import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.subject.SubjectInfo;

@Data
public class RequestBodyForCreateCA {
    private String name;
    private String description;
    private KeyInfo keyInfo;
    private Integer period;
    private Long rootCaId;
    private List<String> altName;
    private List<String> ip;
    private SubjectInfo subjectInfo;
}
