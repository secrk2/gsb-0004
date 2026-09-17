# 矫务通 · 社区矫正管理平台（骨架版）

区司法局自研系统「矫务通」首期落地：**矫务作战台** 与 **对象档案** 两大模块。
后端 Spring Boot 3（Java 17），前端 Vue 3 + Vite + ECharts，H2 文件库免外部依赖，
`docker compose` 一键拉起，预置开箱真实数据。

## 一键启动

```bash
docker compose up -d --build
```

- 前端（Nginx 反代后端）：<http://localhost:8104>
- 后端直连：<http://localhost:7104>（健康检查 `/actuator/health`）
- 数据持久化在 docker volume `jiaowutong-data`（容器内 `/app/data`，H2 文件库）

重置预置数据：`docker compose down -v && docker compose up -d --build`

## 预置账号（密码均为 `123456`）

| 账号 | 角色 | 数据范围 |
| --- | --- | --- |
| `jiandu` | 区监管员（周正安） | 全区三所，可验证跨所汇总 |
| `jing1` | 城东司法所干警（王秉坤） | 仅城东所对象，可验证跨所越权 403 |
| `jing2` / `jing3` | 城西 / 江北干警 | 各仅本所 |
| `obj0001`～`obj0012` | 矫正对象本人 | 仅本人档案与定位，可验证对象间隔离 |

预置 **3 个司法所**（城东/城西/江北）、**12 名对象**，覆盖全部状态：
入矫登记（0004/0011）、在矫（0001/0005/0008/0009/0012）、请假外出（0002/0010）、
训诫（0003）、收监（0006）、解除（0007）；并带今日应报到、逾期未报、
越界/未报到/训诫红点、请假审批单、历史轨迹（含断网补报点）。

## 功能与验收点

### 矫务作战台（`/dashboard`）
- **各所在矫漏斗**：入矫登记 → 在矫 → 请假外出 → 训诫 → 收监 → 解除，按所分卡片横向条形漏斗。
- **今日应报到**：列出当日 DUE 人员，点击直达档案。
- **越界与违规红点**：未处理违规脉冲红点（越界/未报到/训诫分类），填处理意见后消点并留痕。
- **响应式**：1440 双栏、1024 单栏两列所卡片、390 手机单列 KPI。

### 对象档案（`/objects`、`/objects/:id`）
- **姓名脱敏**：列表与详情默认显示「拼音首字母·编号」（如 张伟 / JWT2026-0001 → `ZW·0001`）。
- **二次确认 + 理由 + 留痕**：工作人员点「查看全名」必须二次确认、填写 ≥4 字理由，
  写入 `name_view_log`，档案页可查全部查阅留痕。
- **状态机**：入矫登记→在矫→（请假外出/训诫/收监/解除），请假可回在矫、训诫可回在矫，
  收监只能解除，解除为终态。非法回退后端 **400 拦截并返回具体原因**
  （例：「解除是终态，不能再变更为在矫」），前端 toast 原样展示。流转全部写 `status_log`。
- **对象间隔离**：工作人员按司法所过滤；对象账号只见本人。
  越权访问他人档案（如手动改 URL）返回 **403 错误态页**（非空白页），说明隔离原因。
- 轨迹图区分实时点 / 断网补报点 / 居住点 + 电子围栏虚线圈。

### 对象端（`/mobile`，390px 优先）
- **断网提示**：监听 online/offline，顶部横幅明示离线、暂存条数；恢复网络自动补报。
- **不用旧位置糊弄**：实时上报校验定位时间戳（默认 ±300 秒），陈旧定位前后端均拒绝并明示；
  设备端 `watchPosition` 设置 `maximumAge:0`，过期 fix 不发送。
- **离线队列**：断网点位带设备真实 `recordedAt` 与 UUID `clientId` 持久化到 localStorage，
  恢复后按时间升序批量 `/api/locations/sync`。
- **合并且幂等**：
  - 与最新轨迹点位移 < 8 米判为位置未变更 → `MERGED`，不落新点；
  - `(object_id, client_id)` 唯一台账，重传/重复补报返回首次结论，**绝不产生重复轨迹点**；
  - 移动端提供「原地定位 / 移动 50 米 / 越界 900 米 / 旧位置 / 重传上一条」演示按钮。
- 越界实时（在矫状态且超出围栏半径）自动生成未处理违规红点（同类型去重）。
- 本人报到、销假、请假申请；工作人员端可在档案内审批请假（批准自动转「请假外出」）。

## 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` `/logout` `/api/auth/me` | Token 登录（随机 64 位 hex，存 H2） |
| GET | `/api/dashboard` | 漏斗/应报到/逾期/红点（按管辖范围） |
| GET | `/api/objects` `/api/objects/{id}` | 列表（脱敏）/详情 |
| POST | `/api/objects/{id}/transitions` | 状态机流转（非法回退 400+原因） |
| POST | `/api/objects/{id}/reveal-name` | 二次确认查全名并留痕 |
| GET | `/api/objects/{id}/timeline` `/name-view-logs` `/leaves` `/check-ins` | 档案子资源 |
| POST | `/api/objects/{id}/leaves`、`/api/leaves/{id}/decision` | 请假申请/审批 |
| POST | `/api/me/check-in` `/api/me/return` | 对象本人报到/销假 |
| POST | `/api/locations/report` | 在线实时单条上报（拒绝旧位置） |
| POST | `/api/locations/sync` | 断网恢复批量补报（幂等+合并） |
| GET | `/api/locations/objects/{id}/track` | 轨迹 |
| POST | `/api/violations/{id}/handle` | 红点处理消点 |

## 本地开发（不用 Docker）

```bash
# 后端
cd backend
mvn spring-boot:run           # 7104

# 前端
cd frontend
npm install
npm run dev                   # 5173，已配置 /api 代理到 7104
```

## 目录

```
backend/   Spring Boot 3（JPA/H2，端口 7104）
  domain/ 状态与角色枚举；service/StateMachine.java 状态机
  service/ LocationService 幂等合并、ObjectService 隔离与留痕、DashboardService
  bootstrap/DataInitializer.java 预置数据
frontend/  Vue3 + Vite + ECharts（Nginx 80 → 反代 7104，宿主 8104）
docker-compose.yml
```

> 说明：演示数据中的姓名、坐标、单位均为虚构；定位模拟按钮仅用于无 GPS 环境验收，
> 真机走 `navigator.geolocation`。
