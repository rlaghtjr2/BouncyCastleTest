package com.nhncloud.pca.constant.ca;

public enum CaType {
    ROOT("ROOT"),
    INTERMEDIATE("INTERMEDIATE");

    private final String type;

    CaType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
