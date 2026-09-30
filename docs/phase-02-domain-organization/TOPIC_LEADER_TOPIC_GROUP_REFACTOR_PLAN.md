# 选题负责人及选题组关系重构计划

## 一、目标

将现有 `dept` 角色明确重构为“选题负责人”，建立独立的选题组实体，并使用稳定的数据库 ID 表达学院、专业、选题组、负责人、题目和教师额度之间的关系，消除通过专业名称和选题组名称字符串推导负责人权限的实现。

本项目属于全新二开项目，不存在旧数据迁移需求。本次直接修改最终版 `schema.sql`、`demo-data.sql` 和测试 Schema；迁移文件仅保留空壳模板。

## 二、确定的领域规则

1. 学校组织层级为“学院 → 专业”，不存在独立的系部层级。
2. 选题组是业务分组，不是学校组织层级。
3. 一个学院可以建立多个选题组。
4. 一个专业只归属一个选题组，一个选题组可以包含多个专业。
5. 一个选题负责人只负责一个选题组。
6. 一个选题组暂时允许配置多个负责人，因此不对 `user.topicGroupId` 增加唯一约束。
7. 学生的选题组由其专业确定。
8. 教师可以通过组选题额度配置参与多个选题组。
9. 题目必须明确归属一个选题组。
10. 选题负责人只能查看和审核自己负责组选题。

## 三、目标数据模型

### 3.1 组织与选题组

- `dept` 重命名为 `college`。
- `project` 重命名为 `major`。
- 新增 `topic_group` 表，并通过 `collegeId` 归属学院。
- `major` 使用 `collegeId` 和 `topicGroupId` 建立关系。

### 3.2 用户关系

- `user.dept` 改为 `user.collegeId`。
- `user.project` 改为 `user.majorId`。
- 新增 `user.topicGroupId`，仅供选题负责人角色使用。
- 角色编码 `2` 保持不变，角色枚举由 `DEPT/dept` 改为 `TOPIC_LEADER/topic_leader`。

### 3.3 题目与额度

- `topic.topicGroup` 改为 `topic.topicGroupId`。
- 删除 `topic.deptName` 和 `topic.deptTeacher` 等可通过关联关系获取的冗余字段。
- `teacher_group_quota.groupName` 改为 `topicGroupId`。
- 教师账号继续作为当前额度表和题目表的教师归属键，避免扩大到无关的用户主键重构。

## 四、执行步骤

### 阶段一：数据库初始化脚本

1. 重写 `schema.sql` 中的学院、专业、选题组、用户、题目和教师组选题额度结构。
2. 增加必要的唯一索引、普通索引和 `RESTRICT` 外键。
3. 重写 `demo-data.sql`，按“学院 → 选题组 → 专业 → 用户 → 额度 → 题目”顺序插入示例数据。
4. 同步后端 H2 测试 Schema 和集成测试 MySQL Schema。

### 阶段二：后端实体和持久层

1. 将 `Dept` 体系改为 `College`。
2. 将 `Project` 体系改为 `Major`。
3. 新增 `TopicGroup` 实体、Mapper、Service。
4. 更新 `User`、`Topic` 及相关 Request、DTO、VO 字段。
5. 更新 Mapper XML、排序字段白名单和查询条件。

### 阶段三：后端角色和权限

1. 将 `UserRoleEnum.DEPT` 改为 `UserRoleEnum.TOPIC_LEADER`。
2. 将 Sa-Token 角色字符串 `dept` 改为 `topic_leader`。
3. 将 `userIsDept` 改为 `userIsTopicLeader`。
4. 创建选题负责人时直接校验并保存 `topicGroupId`，由选题组推导学院。
5. 负责人查看和审核题目时直接比较 `user.topicGroupId` 与 `topic.topicGroupId`。
6. 收紧教师额度、报表和用户查询的数据范围，避免负责人获得整个学院的数据权限。
7. 更新教师与选题负责人的身份切换逻辑及提示文本。

### 阶段四：前端组织和负责人表单

1. 将“系部”界面统一为“学院”，将“专业负责人”统一为“选题负责人”。
2. 新增选题组管理页面，支持按学院创建、修改和删除选题组。
3. 将负责人表单由“工号、姓名、系部、专业”改为“工号、姓名、所属学院、负责选题组”。
4. 学院只作为前端联动筛选条件，最终提交 `topicGroupId`。
5. 将专业、题目发布和教师额度表单中的组选题名称字段改为 `topicGroupId`。
6. 同步角色常量、菜单、路由权限、头像身份显示、API 服务和 TypeScript 类型。

### 阶段五：测试和验证

1. 更新单元测试、Controller 契约测试、测试 Fixture 和集成测试。
2. 验证管理员可以完成学院、选题组、专业和负责人配置。
3. 验证负责人只能查看、通过或驳回本组选题。
4. 验证教师只能向有额度的选题组申报题目。
5. 验证学生只能选择本专业所属组选题。
6. 验证修改选题组名称不会破坏 ID 关联。
7. 验证被专业、负责人、题目或额度引用的选题组不能删除。

## 五、验收标准

- 后端不再通过 `user.project → project.groupName` 推导负责人选题组。
- 负责人数据范围统一以 `topicGroupId` 为边界。
- 前后端业务字段不再使用选题组名称作为关联键。
- 页面中不再出现“系部主任”或“专业负责人”作为角色名称。
- `schema.sql` 和 `demo-data.sql` 可以在空数据库中顺序执行。
- Java 规范检查、Maven 测试、前端检查和构建全部通过。

## 六、验证命令

```powershell
pwsh "C:/Users/abc/.codex/skills/java-coding-conventions/scripts/verify-style.ps1" -TargetPath "nfu-graduation-topic-selection-backend/src/main/java"
./mvnw.cmd test
./mvnw.cmd verify
pnpm --dir nfu-graduation-topic-selection-frontend lint
pnpm --dir nfu-graduation-topic-selection-frontend test
pnpm --dir nfu-graduation-topic-selection-frontend build
```

## 七、执行结果

- 已完成最终版 Schema、Demo 数据、空迁移模板，以及 H2/MySQL 集成测试 Schema 的同步重构。
- 已完成学院、专业、选题组、负责人、题目和教师组选题额度的 ID 关联改造。
- 已完成角色 `TOPIC_LEADER/topic_leader`、后端权限范围、前端表单、路由和展示文案改造。
- 已通过 143 个后端单元测试、11 个前端测试、TypeScript 检查、ESLint、前端生产构建和 Java 规范检查。
- 已在独立临时 MySQL 数据库中顺序执行 `schema.sql` 与 `demo-data.sql`，验证成功后删除临时数据库。
- 集成测试代码编译通过；完整 Testcontainers 运行需要本机 Docker Desktop 引擎处于运行状态。
