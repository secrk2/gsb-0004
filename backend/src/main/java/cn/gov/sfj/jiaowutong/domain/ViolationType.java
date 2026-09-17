package cn.gov.sfj.jiaowutong.domain;

public enum ViolationType {
    BOUNDARY("越界"),
    CHECKIN_MISS("未按时报到"),
    WARNING("训诫"),
    OTHER("其他违规");

    private final String label;

    ViolationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
