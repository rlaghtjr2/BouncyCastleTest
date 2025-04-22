package com.nhncloud.pca.constant;

public enum CaType {
    ROOT("ROOT"),
    SUB("SUB");

    private final String type;

    CaType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
