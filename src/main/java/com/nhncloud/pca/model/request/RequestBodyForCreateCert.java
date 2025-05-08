package com.nhncloud.pca.model.request;

import lombok.Data;

import java.util.List;

import com.nhncloud.pca.model.key.KeyInfo;
import com.nhncloud.pca.model.subject.SubjectInfo;

@Data
public class RequestBodyForCreateCert {
    private KeyInfo keyInfo;
    private Integer period;
    private SubjectInfo subjectInfo;
    private List<String> altName;
    private List<String> ip;
}
