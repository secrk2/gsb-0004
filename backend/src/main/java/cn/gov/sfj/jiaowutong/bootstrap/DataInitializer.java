package cn.gov.sfj.jiaowutong.bootstrap;

import cn.gov.sfj.jiaowutong.domain.ObjStatus;
import cn.gov.sfj.jiaowutong.domain.UserRole;
import cn.gov.sfj.jiaowutong.domain.ViolationType;
import cn.gov.sfj.jiaowutong.model.*;
import cn.gov.sfj.jiaowutong.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 开箱数据：3 个司法所；监管员/干警/矫正对象三类账号；
 * 入矫、在矫、请假外出、训诫、收监、解除各状态对象；
 * 今日应报到、逾期未报、越界/违规红点、历史轨迹。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JudicialOfficeRepository officeRepository;
    private final AppUserRepository userRepository;
    private final CorrectionObjectRepository objectRepository;
    private final CheckInRepository checkInRepository;
    private final ViolationRepository violationRepository;
    private final LocationPointRepository pointRepository;
    private final LocationIngestRepository ingestRepository;
    private final StatusLogRepository statusLogRepository;
    private final LeaveRequestRepository leaveRepository;
    private final NameViewLogRepository nameViewLogRepository;
    private final PasswordEncoder passwordEncoder;

    // 虚构辖区坐标（清河区一带）
    private static final double CITY_LAT = 32.0312;
    private static final double CITY_LNG = 118.7866;

    @Override
    public void run(String... args) {
        if (officeRepository.count() > 0) {
            return;
        }
        log.info("=== 矫务通：初始化预置数据 ===");

        JudicialOffice east = office("JD001", "城东司法所", "城东片区");
        JudicialOffice west = office("JD002", "城西司法所", "城西片区");
        JudicialOffice north = office("JD003", "江北司法所", "江北片区");

        String pwd = passwordEncoder.encode("123456");

        // 工作人员账号
        user("jiandu", pwd, "周正安", UserRole.SUPERVISOR, null, null);
        user("jing1", pwd, "王秉坤", UserRole.OFFICER, east, null);
        user("jing2", pwd, "李文静", UserRole.OFFICER, west, null);
        user("jing3", pwd, "张守义", UserRole.OFFICER, north, null);

        // 矫正对象（每所4人，覆盖全部6种状态）
        List<SeedObj> seeds = List.of(
                new SeedObj("0001", "张卫国", east, ObjStatus.ACTIVE, "交通肇事罪", 220, -150, 500),
                new SeedObj("0002", "李秀英", east, ObjStatus.LEAVE, "故意伤害罪", -180, 320, 500),
                new SeedObj("0003", "陈志强", east, ObjStatus.WARNED, "危险驾驶罪", 90, 410, 500),
                new SeedObj("0004", "赵敏", east, ObjStatus.INTAKE, "盗窃罪", 350, -80, 500),

                new SeedObj("0005", "刘德华", west, ObjStatus.ACTIVE, "寻衅滋事罪", -300, 260, 500),
                new SeedObj("0006", "孙丽华", west, ObjStatus.REVOKED, "诈骗罪", 140, -330, 500),
                new SeedObj("0007", "周建国", west, ObjStatus.TERMINATED, "盗窃罪", -260, -190, 500),
                new SeedObj("0008", "吴桂芳", west, ObjStatus.ACTIVE, "妨害公务罪", 400, 120, 500),

                new SeedObj("0009", "郑海燕", north, ObjStatus.ACTIVE, "开设赌场罪", -120, -380, 500),
                new SeedObj("0010", "王立新", north, ObjStatus.LEAVE, "交通肇事罪", 230, 300, 500),
                new SeedObj("0011", "钱多多", north, ObjStatus.INTAKE, "非法经营罪", -340, 90, 500),
                new SeedObj("0012", "冯大鹏", north, ObjStatus.ACTIVE, "聚众斗殴罪", 60, -260, 500)
        );

        List<CorrectionObject> objs = new ArrayList<>();
        for (SeedObj s : seeds) {
            CorrectionObject o = new CorrectionObject();
            o.setCode("JWT2026-" + s.no);
            o.setFullName(s.name);
            o.setOffice(s.office);
            o.setStatus(s.status);
            o.setCrimeType(s.crime);
            o.setSentenceStart(LocalDate.now().minusMonths(8).plusDays(Long.parseLong(s.no) % 20));
            o.setSentenceEnd(LocalDate.now().plusMonths(10 - Long.parseLong(s.no) % 6));
            o.setPhone("138****" + (1000 + (int) (Long.parseLong(s.no) * 37 % 9000)));
            o.setAddress(s.office.getName().replace("司法所", "新村") + (20 + Integer.parseInt(s.no)) + "号");
            o.setHomeLat(CITY_LAT + s.dy / 111_320.0);
            o.setHomeLng(CITY_LNG + s.dx / (111_320.0 * Math.cos(Math.toRadians(CITY_LAT))));
            o.setFenceRadiusMeters(s.fence);
            o.setGuardian(List.of("长子", "配偶", "父亲", "女儿", "母亲").get(Integer.parseInt(s.no) % 5));
            CorrectionObject saved = objectRepository.save(o);
            objs.add(saved);

            // 对象端账号：obj0001 / 123456
            user("obj" + s.no, pwd, s.name + "（对象端）", UserRole.OBJECT, s.office, saved.getId());

            seedStatusPath(saved, s.status);
        }

        CorrectionObject o1 = byNo(objs, "0001");
        CorrectionObject o2 = byNo(objs, "0002");
        CorrectionObject o3 = byNo(objs, "0003");
        CorrectionObject o5 = byNo(objs, "0005");
        CorrectionObject o8 = byNo(objs, "0008");
        CorrectionObject o9 = byNo(objs, "0009");
        CorrectionObject o10 = byNo(objs, "0010");
        CorrectionObject o12 = byNo(objs, "0012");

        // 请假单
        leave(o2, "随妻赴邻市复诊", "江州市第一人民医院", LocalDate.now().minusDays(1), LocalDate.now().plusDays(2), "APPROVED", "王秉坤");
        leave(o10, "处理老家房屋修缮", "安庆市怀宁县", LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), "APPROVED", "张守义");

        // 报到：今日应报到 / 已报到 / 逾期未报
        checkIn(o1, LocalDate.now().minusDays(1), "DONE", Instant.now().minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
        checkIn(o1, LocalDate.now(), "DUE", null, null);
        checkIn(o2, LocalDate.now(), "DUE", null, null);
        checkIn(o3, LocalDate.now(), "DUE", null, null);
        checkIn(o5, LocalDate.now(), "DUE", null, null);
        checkIn(o8, LocalDate.now().minusDays(1), "DUE", null, null); // 逾期未报
        checkIn(o8, LocalDate.now(), "DUE", null, null);
        checkIn(o9, LocalDate.now(), "DONE", Instant.now().minus(3, ChronoUnit.HOURS));
        checkIn(o9, LocalDate.now(), "DUE", null, null);
        checkIn(o10, LocalDate.now(), "DUE", null, null);
        checkIn(o12, LocalDate.now().minusDays(2), "DUE", null, null); // 逾期
        checkIn(o12, LocalDate.now(), "DUE", null, null);

        // 违规红点
        violation(o5, ViolationType.BOUNDARY,
                "定位偏离法定活动范围 860 米（围栏半径 500 米），疑似越界", false,
                o5.getHomeLat() + 860.0 / 111_320, o5.getHomeLng(), Instant.now().minus(5, ChronoUnit.HOURS));
        violation(o12, ViolationType.CHECKIN_MISS, "连续两日未按要求报到，电话联系未果", false,
                null, null, Instant.now().minus(1, ChronoUnit.DAYS));
        violation(o3, ViolationType.WARNING, "训诫处理：定位多次迟报，当面训诫", false,
                null, null, Instant.now().minus(3, ChronoUnit.DAYS));
        // 一条已处理的历史越界（用于红点消除后的对比）
        violation(o1, ViolationType.BOUNDARY, "上月就医途中短暂偏离，核实后已处理", true,
                null, null, Instant.now().minus(20, ChronoUnit.DAYS));

        // 轨迹：0001 在家附近正常活动
        int seq = 0;
        double[][] o1Walk = {{0, 0}, {15, -20}, {40, 10}, {-25, 35}, {10, -45}, {-50, -10}, {20, 25}, {5, 5}};
        for (double[] d : o1Walk) {
            Instant at = Instant.now().minus(36 - seq * 5L, ChronoUnit.HOURS);
            seedPoint(o1, d[0], d[1], at, "seed-1-" + (++seq));
        }
        // 0005 轨迹逐步越界（断网期间多个点未及时上报，恢复后补报）
        double[][] o5Walk = {{20, -30}, {-40, 20}, {60, 50}, {320, 200}, {560, 420}, {700, 610}};
        seq = 0;
        for (double[] d : o5Walk) {
            Instant at = Instant.now().minus(30 - seq * 5L, ChronoUnit.HOURS);
            String source = seq < 3 ? "ONLINE" : "OFFLINE_SYNC";
            seedPoint(o5, d[0], d[1], at, source, "seed-5-" + (++seq));
        }

        // 全名查阅留痕示例
        NameViewLog nv = new NameViewLog();
        nv.setObject(o1);
        nv.setViewerName("周正安");
        nv.setViewerRole(UserRole.SUPERVISOR.name());
        nv.setReason("月度重点对象核查，核对矫正方案");
        nameViewLogRepository.save(nv);

        log.info("=== 预置数据完成：3 司法所 / 4 工作人员 / 12 对象 / 报到+违规+轨迹 ===");
    }

    private void seedStatusPath(CorrectionObject o, ObjStatus status) {
        Instant t = o.getCreatedAt().minus(60, ChronoUnit.DAYS);
        if (status != ObjStatus.INTAKE) {
            statusLog(o, null, ObjStatus.INTAKE, "系统建档", "入矫登记完成", t);
            statusLog(o, ObjStatus.INTAKE, ObjStatus.ACTIVE, o.getOffice().getName() + "干警",
                    "宣告入矫，纳入社区矫正", t.plus(1, ChronoUnit.DAYS));
        } else {
            statusLog(o, null, ObjStatus.INTAKE, "系统建档", "入矫登记完成，待宣告", t);
        }
        switch (status) {
            case LEAVE -> statusLog(o, ObjStatus.ACTIVE, ObjStatus.LEAVE, "监管员审批",
                    "批准请假外出，详见请假单", Instant.now().minus(1, ChronoUnit.DAYS));
            case WARNED -> statusLog(o, ObjStatus.ACTIVE, ObjStatus.WARNED, "王秉坤",
                    "定位迟报、态度敷衍，予以训诫", Instant.now().minus(3, ChronoUnit.DAYS));
            case REVOKED -> {
                statusLog(o, ObjStatus.ACTIVE, ObjStatus.WARNED, "城西司法所干警",
                        "再次违规先行训诫", Instant.now().minus(20, ChronoUnit.DAYS));
                statusLog(o, ObjStatus.WARNED, ObjStatus.REVOKED, "李文静",
                        "违反监督管理规定情节严重，提请撤销缓刑收监", Instant.now().minus(6, ChronoUnit.DAYS));
            }
            case TERMINATED -> statusLog(o, ObjStatus.ACTIVE, ObjStatus.TERMINATED, "李文静",
                    "矫正期满，依法解除社区矫正", Instant.now().minus(2, ChronoUnit.DAYS));
            default -> {
            }
        }
    }

    private void statusLog(CorrectionObject o, ObjStatus from, ObjStatus to,
                           String operator, String reason, Instant at) {
        StatusLog l = new StatusLog();
        l.setObject(o);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setOperatorName(operator);
        l.setReason(reason);
        l.setCreatedAt(at);
        statusLogRepository.save(l);
    }

    private void seedPoint(CorrectionObject o, double dxEast, double dyNorth, Instant at, String clientId) {
        seedPoint(o, dxEast, dyNorth, at, "ONLINE", clientId);
    }

    private void seedPoint(CorrectionObject o, double dxEast, double dyNorth, Instant at,
                           String source, String clientId) {
        double lat = o.getHomeLat() + dyNorth / 111_320.0;
        double lng = o.getHomeLng() + dxEast / (111_320.0 * Math.cos(Math.toRadians(o.getHomeLat())));
        LocationPoint p = new LocationPoint();
        p.setObject(o);
        p.setLat(lat);
        p.setLng(lng);
        p.setAccuracy(12.0);
        p.setRecordedAt(at);
        p.setReportedAt(source.equals("OFFLINE_SYNC") ? at.plus(6, ChronoUnit.HOURS) : at);
        p.setSource(source);
        pointRepository.save(p);

        LocationIngest ing = new LocationIngest();
        ing.setObject(o);
        ing.setClientId(clientId);
        ing.setOutcome("STORED");
        ing.setPointId(p.getId());
        ing.setCreatedAt(p.getReportedAt());
        ingestRepository.save(ing);
    }

    private void violation(CorrectionObject o, ViolationType type, String detail, boolean handled,
                           Double lat, Double lng, Instant at) {
        Violation v = new Violation();
        v.setObject(o);
        v.setType(type);
        v.setDetail(detail);
        v.setLat(lat);
        v.setLng(lng);
        v.setOccurredAt(at);
        v.setHandled(handled);
        if (handled) {
            v.setHandledBy("周正安");
            v.setHandledNote("核实情况属实，已教育处理并归档");
            v.setHandledAt(at.plus(2, ChronoUnit.HOURS));
        }
        violationRepository.save(v);
    }

    private void checkIn(CorrectionObject o, LocalDate due, String status, Instant doneAt) {
        checkIn(o, due, status, doneAt, "APP");
    }

    private void checkIn(CorrectionObject o, LocalDate due, String status, Instant doneAt, String method) {
        CheckIn c = new CheckIn();
        c.setObject(o);
        c.setDueDate(due);
        c.setStatus(status);
        c.setDoneAt(doneAt);
        c.setMethod(method);
        checkInRepository.save(c);
    }

    private void leave(CorrectionObject o, String reason, String dest,
                       LocalDate start, LocalDate end, String status, String approver) {
        LeaveRequest l = new LeaveRequest();
        l.setObject(o);
        l.setReason(reason);
        l.setDestination(dest);
        l.setStartDate(start);
        l.setEndDate(end);
        l.setStatus(status);
        l.setApprover(approver);
        l.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        leaveRepository.save(l);
    }

    private JudicialOffice office(String code, String name, String region) {
        JudicialOffice o = new JudicialOffice();
        o.setCode(code);
        o.setName(name);
        o.setRegion(region);
        return officeRepository.save(o);
    }

    private void user(String username, String pwdHash, String displayName,
                      UserRole role, JudicialOffice office, Long objectId) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(pwdHash);
        u.setDisplayName(displayName);
        u.setRole(role);
        u.setOffice(office);
        u.setObjectId(objectId);
        userRepository.save(u);
    }

    private CorrectionObject byNo(List<CorrectionObject> list, String no) {
        return list.stream().filter(o -> o.getCode().endsWith(no)).findFirst().orElseThrow();
    }

    private record SeedObj(String no, String name, JudicialOffice office, ObjStatus status,
                          String crime, double dx, double dy, int fence) {
    }
}
