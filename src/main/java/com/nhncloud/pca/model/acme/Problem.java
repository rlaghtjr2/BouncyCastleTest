package com.nhncloud.pca.model.acme;

import lombok.Getter;

import java.util.List;

import com.nhncloud.pca.constant.acme.ProblemType;

@Getter
public class Problem {
    private String type;
    private String detail;
    private Integer status;
    private List<Subproblems> subproblems;

    public class Subproblems {
        private String type;
        private String detail;
        private Identifier identifier;
    }

    public Problem(ProblemType problemType, String detail, Integer status) {
        this.type = problemType.getType();
        this.detail = detail;
        this.status = status;
    }
}
