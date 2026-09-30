# 项目文档索引

文档按实施阶段和业务主题分类，文件名保持原有名称，便于追踪历史提交和重构记录。

## 阶段目录

| 阶段 | 目录 | 内容 |
|---|---|---|
| 阶段 00 | [`phase-00-planning`](./phase-00-planning/) | 总体规划、HTTP 接口契约台账与工程命名计划 |
| 阶段 01 | [`phase-01-auth-aop`](./phase-01-auth-aop/) | 认证、AOP 与事务收口 |
| 阶段 02 | [`phase-02-domain-organization`](./phase-02-domain-organization/) | 领域模型、学院、专业、选题组与选题负责人 |
| 阶段 03 | [`phase-03-topic-selection`](./phase-03-topic-selection/) | 选题写入、查询和报表 |
| 阶段 04 | [`phase-04-user-policy`](./phase-04-user-policy/) | 用户管理与系统策略 |
| 阶段 05 | [`phase-05-file-ai`](./phase-05-file-ai/) | 文件与 AI 能力 |
| 阶段 06 | [`phase-06-withdrawal`](./phase-06-withdrawal/) | 退选异步通知与批量退选计划 |

## 维护规则

- 新增阶段性计划或完成报告时，放入对应 `phase-XX-*` 目录。
- 保留原文件名，避免历史引用和提交记录失效。
- 跨阶段引用使用从仓库根目录开始的完整 `docs/phase-XX-*/...` 路径。
- 尚未归类的文档先放入 `phase-00-planning`，确认主题后再移动。
