package com.zhuxiang.service.common;

public enum ViewingMode {
    SELF_SERVICE_LOCK("自助看房"),
    LANDLORD_HOSTED("房东陪同"),
    PLATFORM_HOSTED("平台陪同");

    private final String label;

    ViewingMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
