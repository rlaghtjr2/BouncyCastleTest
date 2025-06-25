package com.nhncloud.pca.exception;


import lombok.Getter;

import org.springframework.http.HttpStatus;

import com.nhncloud.pca.constant.acme.ProblemType;

@Getter
public class AcmeProblemException extends RuntimeException {
    private final ProblemType problemType;
    private final String detail;
    private final HttpStatus status;
    private final String replayNonce;

    public AcmeProblemException(ProblemType problemType, String detail, HttpStatus status, String replayNonce) {
        super(problemType.getDescription());
        this.problemType = problemType;
        this.detail = detail;
        this.status = status;
        this.replayNonce = replayNonce;
    }

    public ProblemType getProblemType() {
        return problemType;
    }
}
