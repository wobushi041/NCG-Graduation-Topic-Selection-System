-- Public, non-destructive MySQL 8 schema.
-- The database user and its privileges must be configured separately.

CREATE DATABASE IF NOT EXISTS `nfu_topic_selection`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `nfu_topic_selection`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `college`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `collegeName` VARCHAR(256) NOT NULL COMMENT '学院名称',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_college_name` (`collegeName`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '学院表';

CREATE TABLE IF NOT EXISTS `topic_group`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `collegeId`  BIGINT       NOT NULL COMMENT '所属学院 id',
    `groupName`  VARCHAR(256) NOT NULL COMMENT '选题组名称',
    `createTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_group_college_name` (`collegeId`, `groupName`),
    KEY `idx_topic_group_college_id` (`collegeId`),
    CONSTRAINT `fk_topic_group_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '选题组表';

CREATE TABLE IF NOT EXISTS `major`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `majorName`    VARCHAR(256) NOT NULL COMMENT '专业名称',
    `collegeId`    BIGINT       NOT NULL COMMENT '所属学院 id',
    `topicGroupId` BIGINT       NOT NULL COMMENT '所属选题组 id',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_college_name` (`collegeId`, `majorName`),
    KEY `idx_major_topic_group_id` (`topicGroupId`),
    CONSTRAINT `fk_major_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_major_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '专业表';

CREATE TABLE IF NOT EXISTS `user`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userAccount`  VARCHAR(128) NOT NULL COMMENT '账号',
    `userName`     VARCHAR(256)          DEFAULT NULL COMMENT '用户姓名',
    `userPassword` VARCHAR(512) NOT NULL COMMENT '密码散列',
    `createTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    `userRole`     INT          NOT NULL DEFAULT 0 COMMENT '用户角色 0-学生 1-教师 2-选题负责人 3-系统',
    `collegeId`    BIGINT                DEFAULT NULL COMMENT '所属学院 id',
    `majorId`      BIGINT                DEFAULT NULL COMMENT '所属专业 id',
    `topicGroupId` BIGINT                DEFAULT NULL COMMENT '负责的选题组 id',
    `status`       VARCHAR(256)          DEFAULT NULL COMMENT '账号状态',
    `topicAmount`  INT                   DEFAULT NULL COMMENT '预先选题数量/最大出题数量',
    `email`        VARCHAR(256)          DEFAULT NULL COMMENT '验证码发送邮箱',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account` (`userAccount`),
    KEY `idx_user_name` (`userName`),
    KEY `idx_user_college_id` (`collegeId`),
    KEY `idx_user_major_id` (`majorId`),
    KEY `idx_user_topic_group_id` (`topicGroupId`),
    CONSTRAINT `fk_user_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_major` FOREIGN KEY (`majorId`) REFERENCES `major` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_user_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS `topic`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `topic`           VARCHAR(255)           DEFAULT NULL COMMENT '题目',
    `type`            VARCHAR(255)           DEFAULT NULL COMMENT '题目类型',
    `description`     LONGTEXT COMMENT '题目描述',
    `requirement`     LONGTEXT COMMENT '对学生要求',
    `teacherName`     VARCHAR(256)           DEFAULT NULL COMMENT '指导老师',
    `teacherAccount`  VARCHAR(128) NOT NULL COMMENT '指导老师账号',
    `topicGroupId`    BIGINT       NOT NULL COMMENT '所属选题组 id',
    `createTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    `surplusQuantity` INT          NOT NULL DEFAULT 1 COMMENT '剩余数量',
    `startTime`       DATETIME              DEFAULT NULL COMMENT '开启时间',
    `endTime`         DATETIME              DEFAULT NULL COMMENT '结束时间',
    `status`          INT          NOT NULL DEFAULT -1 COMMENT '状态: -2-打回 -1-待审核 0-未发布 1-已发布',
    `selectAmount`    INT                   DEFAULT 0 COMMENT '预选人数',
    `reason`          VARCHAR(256)          DEFAULT NULL COMMENT '打回理由',
    PRIMARY KEY (`id`),
    KEY `idx_topic_teacher_account` (`teacherAccount`),
    KEY `idx_topic_group_id` (`topicGroupId`),
    CONSTRAINT `fk_topic_teacher` FOREIGN KEY (`teacherAccount`) REFERENCES `user` (`userAccount`) ON DELETE RESTRICT,
    CONSTRAINT `fk_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '选题表';

CREATE TABLE IF NOT EXISTS `student_topic_selection`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `userAccount` VARCHAR(128) NOT NULL COMMENT '学生账号',
    `topicId`     BIGINT       NOT NULL COMMENT '题目 id',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    `status`      INT          NOT NULL DEFAULT 0 COMMENT '选题状态: -1-取消预选 0-确认预选 1-取消选题 2-确认选题',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection_user_topic` (`userAccount`, `topicId`),
    KEY `idx_selection_user_account` (`userAccount`),
    KEY `idx_selection_topic_id` (`topicId`),
    CONSTRAINT `fk_selection_user` FOREIGN KEY (`userAccount`) REFERENCES `user` (`userAccount`) ON DELETE RESTRICT,
    CONSTRAINT `fk_selection_topic` FOREIGN KEY (`topicId`) REFERENCES `topic` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '学生选题关联表';

CREATE TABLE IF NOT EXISTS `teacher_group_quota`
(
    `teacherAccount` VARCHAR(128) NOT NULL COMMENT '教师账号',
    `topicGroupId`   BIGINT       NOT NULL COMMENT '选题组 id',
    `maxTopics`      INT          NOT NULL COMMENT '最大出题数量',
    PRIMARY KEY (`teacherAccount`, `topicGroupId`),
    KEY `idx_teacher_quota_group_id` (`topicGroupId`),
    CONSTRAINT `fk_teacher_quota_user` FOREIGN KEY (`teacherAccount`) REFERENCES `user` (`userAccount`) ON DELETE RESTRICT,
    CONSTRAINT `fk_teacher_quota_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`) ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '教师组选题额度表';

CREATE TABLE IF NOT EXISTS `switch`
(
    `name`   VARCHAR(128) NOT NULL COMMENT '开关名称',
    `status` INT          NOT NULL COMMENT '开关状态',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统开关表';
