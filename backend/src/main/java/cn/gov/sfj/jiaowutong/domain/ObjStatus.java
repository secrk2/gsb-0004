package cn.gov.sfj.jiaowutong.domain;

/**
 * 矫正对象状态机：
 * 入矫登记 -> 在矫 -> 请假外出 -> 在矫
 *                 \-> 训诫 -> 在矫
 *                 \-> 收监 -> 解除
 * 在矫/请假外出/训诫 -> 解除
 * 解除为终态。任何逆向回退均由 ObjectService 拒绝并给出原因。
 */
public enum ObjStatus {
    INTAKE("入矫登记"),
    ACTIVE("在矫"),
    LEAVE("请假外出"),
    WARNED("训诫"),
    REVOKED("收监"),
    TERMINATED("解除");

    private final String label;

    ObjStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
