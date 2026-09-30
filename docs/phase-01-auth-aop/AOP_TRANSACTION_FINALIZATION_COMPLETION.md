# 全局事务边界与 AOP 架构最终收口报告（Wave 6：阶段 4）

## 一、收口背景与目标

随着 **阶段 1（认证域）**、**阶段 2（选题域读写）** 及 **Wave 1 ～ Wave 5（课题、组织与选题组、策略与用户、查询与统计、文件与 AI）** 共 **79 个 HTTP 接口**全部完成 Controller/Service 职责解耦与 `@SentinelRateLimit` 切面限流迁移，全仓 **23 个多步写用例**已 100% 下沉至应用服务层并显式声明方法级 `@Transactional`。

本次 **Wave 6（阶段 4：AOP 与全局事务最终收口）** 作为全仓重构的最后一环，完成了以下目标：
1. **安全摘除 5 个基础 `ServiceImpl` 上的类级 `@Transactional`（完结 `ARCH-05` 与 `AOP-005`）**；
2. **移除 `WorkTopicSelectionApplication` 上不必要的 `exposeProxy = true`（完结 `AOP-006`）**；
3. **明确 `CacheSearchOptimizationAOP` 缓存切面治理决议（完结 `AOP-001`～`AOP-003`）**；
4. **补充全局事务边界与 AOP 红线回归守护测试 `ArchitectureTransactionBoundaryGuardTest`**。

---

## 二、核心收口交付项

### 1. 基础 `ServiceImpl` 类级 `@Transactional` 全面摘除（`ARCH-05` / `AOP-005`）

| 序号 | 基础服务实现类 | 原状态 | 收口后状态 | 架构收益 |
|---|---|---|---|---|
| 1 | `UserServiceImpl` | 类级 `@Transactional` | **无类级事务** | 消除 `userIsAdmin`、`getUserVO`、`getQueryWrapper`、`userGetCurrentLoginUser` 等高频只读/内存方法无差别开启写事务的开销 |
| 2 | `TopicServiceImpl` | 类级 `@Transactional` | **无类级事务** | 消除 `getQueryWrapper`、`getTopicQueryByAdminWrapper` 及分页查询的写事务代理开销 |
| 3 | `StudentTopicSelectionServiceImpl` | 类级 `@Transactional` | **无类级事务** | 消除选题状态统计与未选学生筛选时的写事务连接占用 |
| 4 | `DeptServiceImpl` | 类级 `@Transactional` | **无类级事务** | 消除系部分页/列表查询与 `getQueryWrapper` 的写事务代理开销 |
| 5 | `ProjectServiceImpl` | 类级 `@Transactional` | **无类级事务** | 消除专业分页/列表查询与 `getQueryWrapper` 的写事务代理开销 |

同时，全仓 **23 个多步写用例**由 6 个应用服务实现类通过**方法级 `@Transactional`** 精确管理事务边界：
- `PasswordServiceImpl`：3 个写方法
- `TopicSelectionApplicationServiceImpl`：4 个写方法
- `TopicApplicationServiceImpl`：7 个写方法
- `OrganizationApplicationServiceImpl`：5 个写方法
- `UserApplicationServiceImpl`：3 个写方法
- `FileApplicationServiceImpl`：1 个写方法

### 2. `@EnableAspectJAutoProxy` 配置收敛（`AOP-006`）
- 全仓核查无任何 `AopContext.currentProxy()` 调用，已将 `WorkTopicSelectionApplication` 上的 `@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)` 收敛为 `@EnableAspectJAutoProxy(proxyTargetClass = true)`，消除每次 AOP 代理调用写入 ThreadLocal 暴露栈的额外开销。

### 3. `CacheSearchOptimizationAOP` 治理决议（`AOP-001`～`AOP-003`）
- 保持 `TopicQueryController` 中的 `@CacheSearchOptimization` 注释冻结状态，默认不挂载切入点，避免在尚未建立按角色/系部隔离 Key 及写后主动失效机制前产生跨角色脏读。

---

## 三、验证结果与全仓重构总览

1. **Java 编码规范校验（`verify-style.ps1`）**：Wave 6 修改及新增的 7 个 Java 文件全部通过校验（`0 violations found`）。
2. **全仓单元测试**：新增 `ArchitectureTransactionBoundaryGuardTest` 后，后端全量 **210 个单元测试 100% 通过**（`Failures: 0, Errors: 0`）。
3. **全仓架构问题清零总览**：
   - `ARCH-01`（`UserController` 拆分）：11 个高内聚 Controller 各司其职（**已完成**）
   - `ARCH-02`（Controller 0 Mapper）：全仓 Controller 0 Mapper 直接注入（**已完成**）
   - `ARCH-03`（`UserController` 0 `TransactionTemplate`）：20 处全部下沉（**已完成**）
   - `ARCH-04`（`FileController` 0 `TransactionTemplate`）：下沉至 `FileApplicationServiceImpl#uploadFile`（**已完成**）
   - `ARCH-05`（摘除 5 个基础 `ServiceImpl` 类级 `@Transactional`）：23 个写用例精准方法级事务，读用例 0 写事务（**已完成**）
   - `ARCH-06`（100% 路由与协议兼容）：79 个 HTTP 接口前端零破坏性改动（**已完成**）
