package cn.gov.sfj.jiaowutong.domain;

public enum UserRole {
    SUPERVISOR("监管员"),
    OFFICER("司法所干警"),
    OBJECT("矫正对象");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
