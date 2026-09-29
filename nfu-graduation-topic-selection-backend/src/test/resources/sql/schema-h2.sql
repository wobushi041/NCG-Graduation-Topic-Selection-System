CREATE TABLE IF NOT EXISTS `college`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `collegeName` VARCHAR(256) NOT NULL UNIQUE,
    `createTime`  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime`  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`    TINYINT  DEFAULT 0 NOT NULL
);

CREATE TABLE IF NOT EXISTS `topic_group`
(
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `collegeId`  BIGINT       NOT NULL,
    `groupName`  VARCHAR(256) NOT NULL,
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`   TINYINT  DEFAULT 0 NOT NULL,
    CONSTRAINT `uk_topic_group_college_name` UNIQUE (`collegeId`, `groupName`),
    CONSTRAINT `fk_topic_group_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`)
);

CREATE TABLE IF NOT EXISTS `major`
(
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `majorName`    VARCHAR(256) NOT NULL,
    `collegeId`    BIGINT       NOT NULL,
    `topicGroupId` BIGINT       NOT NULL,
    `createTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`     TINYINT  DEFAULT 0 NOT NULL,
    CONSTRAINT `uk_major_college_name` UNIQUE (`collegeId`, `majorName`),
    CONSTRAINT `fk_major_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`),
    CONSTRAINT `fk_major_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`)
);

CREATE TABLE IF NOT EXISTS `user`
(
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `userAccount`  VARCHAR(128) NOT NULL UNIQUE,
    `userName`     VARCHAR(256),
    `userPassword` VARCHAR(512) NOT NULL,
    `createTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime`   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`     TINYINT  DEFAULT 0 NOT NULL,
    `userRole`     INT      DEFAULT 0 NOT NULL,
    `collegeId`    BIGINT,
    `majorId`      BIGINT,
    `topicGroupId` BIGINT,
    `status`       VARCHAR(256),
    `topicAmount`  INT,
    `email`        VARCHAR(256),
    CONSTRAINT `fk_user_college` FOREIGN KEY (`collegeId`) REFERENCES `college` (`id`),
    CONSTRAINT `fk_user_major` FOREIGN KEY (`majorId`) REFERENCES `major` (`id`),
    CONSTRAINT `fk_user_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`)
);

CREATE TABLE IF NOT EXISTS `topic`
(
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `topic`           VARCHAR(255),
    `type`            VARCHAR(255),
    `description`     LONGTEXT,
    `requirement`     LONGTEXT,
    `teacherName`     VARCHAR(256),
    `teacherAccount`  VARCHAR(128) NOT NULL,
    `topicGroupId`    BIGINT       NOT NULL,
    `createTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime`      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`        TINYINT  DEFAULT 0 NOT NULL,
    `surplusQuantity` INT      DEFAULT 1 NOT NULL,
    `startTime`       DATETIME,
    `endTime`         DATETIME,
    `status`          INT      DEFAULT -1 NOT NULL,
    `selectAmount`    INT      DEFAULT 0,
    `reason`          VARCHAR(256),
    CONSTRAINT `fk_topic_teacher` FOREIGN KEY (`teacherAccount`) REFERENCES `user` (`userAccount`),
    CONSTRAINT `fk_topic_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`)
);

CREATE TABLE IF NOT EXISTS `student_topic_selection`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `userAccount` VARCHAR(128) NOT NULL,
    `topicId`     BIGINT       NOT NULL,
    `createTime`  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `updateTime`  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `isDelete`    TINYINT  DEFAULT 0 NOT NULL,
    `status`      INT      DEFAULT 0 NOT NULL,
    CONSTRAINT `uk_selection_user_topic` UNIQUE (`userAccount`, `topicId`),
    CONSTRAINT `fk_selection_user` FOREIGN KEY (`userAccount`) REFERENCES `user` (`userAccount`),
    CONSTRAINT `fk_selection_topic` FOREIGN KEY (`topicId`) REFERENCES `topic` (`id`)
);

CREATE TABLE IF NOT EXISTS `teacher_group_quota`
(
    `teacherAccount` VARCHAR(128) NOT NULL,
    `topicGroupId`   BIGINT       NOT NULL,
    `maxTopics`      INT          NOT NULL,
    PRIMARY KEY (`teacherAccount`, `topicGroupId`),
    CONSTRAINT `fk_teacher_quota_user` FOREIGN KEY (`teacherAccount`) REFERENCES `user` (`userAccount`),
    CONSTRAINT `fk_teacher_quota_group` FOREIGN KEY (`topicGroupId`) REFERENCES `topic_group` (`id`)
);

CREATE TABLE IF NOT EXISTS `switch`
(
    `name`   VARCHAR(128) PRIMARY KEY,
    `status` INT NOT NULL
);
