package com.nhncloud.pca.model.acme;

import java.util.List;

import com.nhncloud.pca.constant.acme.ProblemType;

import lombok.Getter;

@Getter
public class Problem {
    private String type;
    private String detail;
    private List<Subproblems> subproblems;

    public class Subproblems {
        private String type;
        private String detail;
        private Identifier identifier;
    }

    public Problem(ProblemType problemType) {
        this.type = problemType.getType();
        this.detail = problemType.getDescription();
    }
}
