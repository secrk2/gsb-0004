package cn.gov.sfj.jiaowutong.service;

import cn.gov.sfj.jiaowutong.common.ApiException;
import cn.gov.sfj.jiaowutong.domain.ObjStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 矫正对象状态机。非法流转一律拒绝，且错误信息必须说明原因。
 */
public final class StateMachine {

    private static final Map<ObjStatus, Set<ObjStatus>> ALLOWED = Map.of(
            ObjStatus.INTAKE, EnumSet.of(ObjStatus.ACTIVE),
            ObjStatus.ACTIVE, EnumSet.of(ObjStatus.LEAVE, ObjStatus.WARNED, ObjStatus.REVOKED, ObjStatus.TERMINATED),
            ObjStatus.LEAVE, EnumSet.of(ObjStatus.ACTIVE, ObjStatus.TERMINATED),
            ObjStatus.WARNED, EnumSet.of(ObjStatus.ACTIVE, ObjStatus.REVOKED, ObjStatus.TERMINATED),
            ObjStatus.REVOKED, EnumSet.of(ObjStatus.TERMINATED),
            ObjStatus.TERMINATED, EnumSet.noneOf(ObjStatus.class)
    );

    private StateMachine() {
    }

    public static boolean canTransit(ObjStatus from, ObjStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** 非法流转的面向用户解释 */
    public static String explain(ObjStatus from, ObjStatus to) {
        if (from == to) {
            return "未发生状态变更：对象当前已是「" + from.getLabel() + "」，无需重复操作";
        }
        if (from == ObjStatus.TERMINATED) {
            return "非法回退被拦截：「解除」是终态，矫正关系已经终止，不能再变更为「" + to.getLabel() + "」";
        }
        if (to == ObjStatus.INTAKE) {
            return "流程不可逆：「入矫登记」是起始环节，不能从「" + from.getLabel() + "」退回入矫登记";
        }
        if (from == ObjStatus.INTAKE) {
            return "非法回退被拦截：入矫登记后必须先宣告入矫、转入「在矫」，不能直接变为「" + to.getLabel() + "」";
        }
        if (from == ObjStatus.REVOKED) {
            return "非法回退被拦截：对象已「收监」，收监后只能「解除」，不能变更为「" + to.getLabel() + "」";
        }
        String allowed = ALLOWED.get(from).stream().map(ObjStatus::getLabel)
                .collect(Collectors.joining("、"));
        return "非法回退被拦截：当前状态「" + from.getLabel() + "」不允许变更为「" + to.getLabel()
                + "」，允许的下一状态为：" + allowed;
    }

    public static Set<ObjStatus> nextOf(ObjStatus status) {
        return ALLOWED.getOrDefault(status, Set.of());
    }
}
