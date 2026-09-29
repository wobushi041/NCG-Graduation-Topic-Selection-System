-- 广州南方学院工学院本地演示数据（MySQL 8）。
-- 学院、专业和教师姓名来自学校官网公开页面；账号、邮箱、密码、学生姓名、学号和题目均为虚构。
-- 本文件仅用于本地开发与功能演示，不得作为真实教务数据使用。
-- 所有演示账号的初始密码均为：12345678
-- 专业来源：https://sece.nfu.edu.cn/xygk/xyjj/xyjj.htm
-- 教师来源：
--   https://sece.nfu.edu.cn/sztd/dzydqgcx/dzxxykxjs.htm
--   https://sece.nfu.edu.cn/sztd/dzydqgcx/txgc.htm
--   https://sece.nfu.edu.cn/sztd/dzydqgcx/dqgcjqzdh.htm
--   https://sece.nfu.edu.cn/sztd/jsjkxygcx/jsjkxyjs.htm
--   https://sece.nfu.edu.cn/sztd/jsjkxygcx/rjgc.htm
--   https://sece.nfu.edu.cn/sztd/jsjkxygcx/znkxyjs.htm
--   https://sece.nfu.edu.cn/sztd/jsjkxygcx/sjkxydsjjs.htm

USE `nfu_topic_selection`;

SET NAMES utf8mb4;
SET @demo_password_hash = '$2b$10$KVyx9aXjP4k43kFCKRF73ehUM08FXRhdWc9sGvSeylFZuutpO975e';


-- -----------------------------------------------------------------------------
-- 超级管理员
-- -----------------------------------------------------------------------------

INSERT INTO `user` (`userAccount`, `userName`, `userPassword`, `userRole`, `status`, `isDelete`)
VALUES ('admin', 'admin', '$2b$10$KVyx9aXjP4k43kFCKRF73ehUM08FXRhdWc9sGvSeylFZuutpO975e', 3, '老用户', 0)
ON DUPLICATE KEY UPDATE
                     `userPassword` = VALUES(`userPassword`),
                     `userRole`     = 3,
                     `status`       = '老用户',
                     `isDelete`     = 0;

-- -----------------------------------------------------------------------------
-- 学院、选题组与专业
-- -----------------------------------------------------------------------------

INSERT INTO `college` (`collegeName`, `isDelete`)
VALUES ('工学院', 0)
ON DUPLICATE KEY UPDATE
    `isDelete` = 0;

INSERT INTO `topic_group` (`collegeId`, `groupName`, `isDelete`)
SELECT c.id, seed.groupName, 0
FROM `college` c
JOIN (
    SELECT '电子与电气工程选题组' AS groupName
    UNION ALL SELECT '计算机与软件工程选题组'
    UNION ALL SELECT '智能与数据工程选题组'
) seed
WHERE c.`collegeName` = '工学院'
ON DUPLICATE KEY UPDATE
    `isDelete` = 0;

DROP TEMPORARY TABLE IF EXISTS `mock_major_seed`;
CREATE TEMPORARY TABLE `mock_major_seed`
(
    `majorName` VARCHAR(256) NOT NULL,
    `groupName` VARCHAR(256) NOT NULL,
    PRIMARY KEY (`majorName`)
);

INSERT INTO `mock_major_seed` (`majorName`, `groupName`)
VALUES ('电子信息科学与技术', '电子与电气工程选题组'),
       ('通信工程', '电子与电气工程选题组'),
       ('电气工程及其自动化', '电子与电气工程选题组'),
       ('计算机科学与技术', '计算机与软件工程选题组'),
       ('软件工程', '计算机与软件工程选题组'),
       ('智能科学与技术', '智能与数据工程选题组'),
       ('数据科学与大数据技术', '智能与数据工程选题组');

INSERT INTO `major` (`majorName`, `collegeId`, `topicGroupId`, `isDelete`)
SELECT seed.majorName, c.id, g.id, 0
FROM `mock_major_seed` seed
JOIN `college` c ON c.`collegeName` = '工学院'
JOIN `topic_group` g
  ON g.`collegeId` = c.id
 AND g.`groupName` = seed.groupName
ON DUPLICATE KEY UPDATE
    `topicGroupId` = VALUES(`topicGroupId`),
    `isDelete` = 0;

-- -----------------------------------------------------------------------------
-- 教师与选题负责人
-- 教师姓名来自官网公开师资页面；工号式账号和邮箱为本项目虚构。
-- -----------------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `mock_teacher_seed`;
CREATE TEMPORARY TABLE `mock_teacher_seed`
(
    `userAccount` VARCHAR(128) NOT NULL,
    `userName`    VARCHAR(256) NOT NULL,
    `groupName`   VARCHAR(256) NOT NULL,
    `email`       VARCHAR(256) NOT NULL,
    PRIMARY KEY (`userAccount`)
);

INSERT INTO `mock_teacher_seed` (`userAccount`, `userName`, `groupName`, `email`)
VALUES ('mock-eng-t001', '于胜云', '电子与电气工程选题组', 'mock-eng-t001@example.invalid'),
       ('mock-eng-t002', '黄家晖', '电子与电气工程选题组', 'mock-eng-t002@example.invalid'),
       ('mock-eng-t003', '田文春', '电子与电气工程选题组', 'mock-eng-t003@example.invalid'),
       ('mock-eng-t004', '郝学飞', '电子与电气工程选题组', 'mock-eng-t004@example.invalid'),
       ('mock-eng-t005', '邓达荣', '电子与电气工程选题组', 'mock-eng-t005@example.invalid'),
       ('mock-eng-t006', '熊子昂', '电子与电气工程选题组', 'mock-eng-t006@example.invalid'),
       ('mock-eng-t007', '张飞', '计算机与软件工程选题组', 'mock-eng-t007@example.invalid'),
       ('mock-eng-t008', '庞引明', '计算机与软件工程选题组', 'mock-eng-t008@example.invalid'),
       ('mock-eng-t009', '陈深进', '计算机与软件工程选题组', 'mock-eng-t009@example.invalid'),
       ('mock-eng-t010', '杨娟', '计算机与软件工程选题组', 'mock-eng-t010@example.invalid'),
       ('mock-eng-t011', '邵孟良', '智能与数据工程选题组', 'mock-eng-t011@example.invalid'),
       ('mock-eng-t012', '俞新凯', '智能与数据工程选题组', 'mock-eng-t012@example.invalid'),
       ('mock-eng-t013', '左海春', '智能与数据工程选题组', 'mock-eng-t013@example.invalid'),
       ('mock-eng-t014', '孙安临', '智能与数据工程选题组', 'mock-eng-t014@example.invalid');

INSERT INTO `user` (
    `userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`,
    `topicAmount`, `status`, `email`, `isDelete`
)
SELECT seed.userAccount, seed.userName, @demo_password_hash, 1, c.id,
       5, '老用户', seed.email, 0
FROM `mock_teacher_seed` seed
JOIN `college` c ON c.`collegeName` = '工学院'
ON DUPLICATE KEY UPDATE
    `userName` = VALUES(`userName`),
    `userPassword` = VALUES(`userPassword`),
    `userRole` = VALUES(`userRole`),
    `collegeId` = VALUES(`collegeId`),
    `majorId` = NULL,
    `topicGroupId` = NULL,
    `topicAmount` = VALUES(`topicAmount`),
    `status` = VALUES(`status`),
    `email` = VALUES(`email`),
    `isDelete` = 0;

DROP TEMPORARY TABLE IF EXISTS `mock_leader_seed`;
CREATE TEMPORARY TABLE `mock_leader_seed`
(
    `userAccount` VARCHAR(128) NOT NULL,
    `userName`    VARCHAR(256) NOT NULL,
    `groupName`   VARCHAR(256) NOT NULL,
    `email`       VARCHAR(256) NOT NULL,
    PRIMARY KEY (`userAccount`)
);

-- 姓名和邮箱与对应教师账号一致，用于演示教师/选题负责人角色切换。
INSERT INTO `mock_leader_seed` (`userAccount`, `userName`, `groupName`, `email`)
VALUES ('mock-eng-l001', '于胜云', '电子与电气工程选题组', 'mock-eng-t001@example.invalid'),
       ('mock-eng-l002', '张飞', '计算机与软件工程选题组', 'mock-eng-t007@example.invalid'),
       ('mock-eng-l003', '左海春', '智能与数据工程选题组', 'mock-eng-t013@example.invalid');

INSERT INTO `user` (
    `userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`,
    `topicGroupId`, `status`, `email`, `isDelete`
)
SELECT seed.userAccount, seed.userName, @demo_password_hash, 2, c.id,
       g.id, '老用户', seed.email, 0
FROM `mock_leader_seed` seed
JOIN `college` c ON c.`collegeName` = '工学院'
JOIN `topic_group` g
  ON g.`collegeId` = c.id
 AND g.`groupName` = seed.groupName
ON DUPLICATE KEY UPDATE
    `userName` = VALUES(`userName`),
    `userPassword` = VALUES(`userPassword`),
    `userRole` = VALUES(`userRole`),
    `collegeId` = VALUES(`collegeId`),
    `majorId` = NULL,
    `topicGroupId` = VALUES(`topicGroupId`),
    `topicAmount` = NULL,
    `status` = VALUES(`status`),
    `email` = VALUES(`email`),
    `isDelete` = 0;

INSERT INTO `teacher_group_quota` (`teacherAccount`, `topicGroupId`, `maxTopics`)
SELECT seed.userAccount, g.id, 5
FROM `mock_teacher_seed` seed
JOIN `college` c ON c.`collegeName` = '工学院'
JOIN `topic_group` g
  ON g.`collegeId` = c.id
 AND g.`groupName` = seed.groupName
ON DUPLICATE KEY UPDATE
    `maxTopics` = VALUES(`maxTopics`);

-- -----------------------------------------------------------------------------
-- 学生
-- 以下姓名、学号式账号和邮箱全部随机生成且纯属虚构，每个专业 4 人。
-- -----------------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `mock_student_seed`;
CREATE TEMPORARY TABLE `mock_student_seed`
(
    `userAccount` VARCHAR(128) NOT NULL,
    `userName`    VARCHAR(256) NOT NULL,
    `majorName`   VARCHAR(256) NOT NULL,
    `email`       VARCHAR(256) NOT NULL,
    PRIMARY KEY (`userAccount`)
);

INSERT INTO `mock_student_seed` (`userAccount`, `userName`, `majorName`, `email`)
VALUES ('mock-2023-eis-001', '林星河', '电子信息科学与技术', 'mock-2023-eis-001@example.invalid'),
       ('mock-2023-eis-002', '陈语桐', '电子信息科学与技术', 'mock-2023-eis-002@example.invalid'),
       ('mock-2023-eis-003', '周明远', '电子信息科学与技术', 'mock-2023-eis-003@example.invalid'),
       ('mock-2023-eis-004', '何雨晴', '电子信息科学与技术', 'mock-2023-eis-004@example.invalid'),
       ('mock-2023-com-001', '梁知行', '通信工程', 'mock-2023-com-001@example.invalid'),
       ('mock-2023-com-002', '黄可欣', '通信工程', 'mock-2023-com-002@example.invalid'),
       ('mock-2023-com-003', '吴景澄', '通信工程', 'mock-2023-com-003@example.invalid'),
       ('mock-2023-com-004', '郑思妍', '通信工程', 'mock-2023-com-004@example.invalid'),
       ('mock-2023-ele-001', '冯宇航', '电气工程及其自动化', 'mock-2023-ele-001@example.invalid'),
       ('mock-2023-ele-002', '许安然', '电气工程及其自动化', 'mock-2023-ele-002@example.invalid'),
       ('mock-2023-ele-003', '罗子轩', '电气工程及其自动化', 'mock-2023-ele-003@example.invalid'),
       ('mock-2023-ele-004', '彭书瑶', '电气工程及其自动化', 'mock-2023-ele-004@example.invalid'),
       ('mock-2023-cs-001', '邓浩然', '计算机科学与技术', 'mock-2023-cs-001@example.invalid'),
       ('mock-2023-cs-002', '谢婉清', '计算机科学与技术', 'mock-2023-cs-002@example.invalid'),
       ('mock-2023-cs-003', '苏亦辰', '计算机科学与技术', 'mock-2023-cs-003@example.invalid'),
       ('mock-2023-cs-004', '钟若溪', '计算机科学与技术', 'mock-2023-cs-004@example.invalid'),
       ('mock-2023-se-001', '郭俊熙', '软件工程', 'mock-2023-se-001@example.invalid'),
       ('mock-2023-se-002', '唐诗涵', '软件工程', 'mock-2023-se-002@example.invalid'),
       ('mock-2023-se-003', '叶承泽', '软件工程', 'mock-2023-se-003@example.invalid'),
       ('mock-2023-se-004', '蒋欣怡', '软件工程', 'mock-2023-se-004@example.invalid'),
       ('mock-2023-ai-001', '韩一帆', '智能科学与技术', 'mock-2023-ai-001@example.invalid'),
       ('mock-2023-ai-002', '赖梦琪', '智能科学与技术', 'mock-2023-ai-002@example.invalid'),
       ('mock-2023-ai-003', '蔡睿哲', '智能科学与技术', 'mock-2023-ai-003@example.invalid'),
       ('mock-2023-ai-004', '方静宜', '智能科学与技术', 'mock-2023-ai-004@example.invalid'),
       ('mock-2023-ds-001', '卢嘉树', '数据科学与大数据技术', 'mock-2023-ds-001@example.invalid'),
       ('mock-2023-ds-002', '袁芷晴', '数据科学与大数据技术', 'mock-2023-ds-002@example.invalid'),
       ('mock-2023-ds-003', '姚致远', '数据科学与大数据技术', 'mock-2023-ds-003@example.invalid'),
       ('mock-2023-ds-004', '戴心悦', '数据科学与大数据技术', 'mock-2023-ds-004@example.invalid');

INSERT INTO `user` (
    `userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`,
    `majorId`, `topicAmount`, `status`, `email`, `isDelete`
)
SELECT seed.userAccount, seed.userName, @demo_password_hash, 0, c.id,
       m.id, 3, '老用户', seed.email, 0
FROM `mock_student_seed` seed
JOIN `college` c ON c.`collegeName` = '工学院'
JOIN `major` m
  ON m.`collegeId` = c.id
 AND m.`majorName` = seed.majorName
ON DUPLICATE KEY UPDATE
    `userName` = VALUES(`userName`),
    `userPassword` = VALUES(`userPassword`),
    `userRole` = VALUES(`userRole`),
    `collegeId` = VALUES(`collegeId`),
    `majorId` = VALUES(`majorId`),
    `topicGroupId` = NULL,
    `topicAmount` = VALUES(`topicAmount`),
    `status` = VALUES(`status`),
    `email` = VALUES(`email`),
    `isDelete` = 0;

-- -----------------------------------------------------------------------------
-- 演示题目
-- 题目均为虚构；每个专业准备 1 个已发布题目，便于直接演示学生选题流程。
-- -----------------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS `mock_topic_seed`;
CREATE TEMPORARY TABLE `mock_topic_seed`
(
    `topic`          VARCHAR(255) NOT NULL,
    `type`           VARCHAR(255) NOT NULL,
    `description`    TEXT         NOT NULL,
    `requirement`    TEXT         NOT NULL,
    `teacherAccount` VARCHAR(128) NOT NULL,
    `groupName`      VARCHAR(256) NOT NULL,
    PRIMARY KEY (`topic`, `teacherAccount`)
);

INSERT INTO `mock_topic_seed` (
    `topic`, `type`, `description`, `requirement`, `teacherAccount`, `groupName`
)
VALUES ('基于边缘计算的实验室环境监测系统', '工程设计', '采集并分析实验室温湿度与设备状态数据。', '掌握嵌入式开发与基础 Web 开发。', 'mock-eng-t001', '电子与电气工程选题组'),
       ('校园 LoRa 物联网通信质量分析平台', '应用研究', '构建校园物联网链路质量采集、统计与可视化平台。', '掌握通信原理与 Python 数据分析。', 'mock-eng-t003', '电子与电气工程选题组'),
       ('光伏储能微电网能量管理系统', '工程设计', '模拟光伏、储能和负载之间的能量调度策略。', '掌握电路基础与控制系统基础。', 'mock-eng-t005', '电子与电气工程选题组'),
       ('毕业设计选题智能推荐与审核系统', '软件系统', '根据学生兴趣与教师研究方向提供选题推荐和审核辅助。', '掌握 Java、数据库与前端开发。', 'mock-eng-t007', '计算机与软件工程选题组'),
       ('面向课程项目的自动化测试管理平台', '软件系统', '管理测试任务、测试报告和缺陷闭环。', '掌握软件工程、接口测试与持续集成。', 'mock-eng-t009', '计算机与软件工程选题组'),
       ('基于视觉模型的实验室安全行为识别', '应用研究', '识别实验室场景中的典型安全风险行为。', '掌握 Python、机器学习与计算机视觉基础。', 'mock-eng-t011', '智能与数据工程选题组'),
       ('校园能耗数据分析与预测平台', '数据分析', '完成校园建筑能耗数据清洗、分析、预测与可视化。', '掌握数据分析、时间序列与可视化。', 'mock-eng-t013', '智能与数据工程选题组');

INSERT INTO `topic` (
    `topic`, `type`, `description`, `requirement`, `teacherName`, `teacherAccount`,
    `topicGroupId`, `surplusQuantity`, `startTime`, `endTime`, `status`, `selectAmount`, `isDelete`
)
SELECT seed.topic, seed.type, seed.description, seed.requirement,
       teacher.userName, seed.teacherAccount, g.id,
       2, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1, 0, 0
FROM `mock_topic_seed` seed
JOIN `user` teacher
  ON teacher.`userAccount` = seed.teacherAccount
 AND teacher.`userRole` = 1
JOIN `college` c ON c.`collegeName` = '工学院'
JOIN `topic_group` g
  ON g.`collegeId` = c.id
 AND g.`groupName` = seed.groupName
WHERE NOT EXISTS (
    SELECT 1
    FROM `topic` existing
    WHERE existing.`topic` = seed.topic
      AND existing.`teacherAccount` = seed.teacherAccount
);

DROP TEMPORARY TABLE IF EXISTS `mock_topic_seed`;
DROP TEMPORARY TABLE IF EXISTS `mock_student_seed`;
DROP TEMPORARY TABLE IF EXISTS `mock_leader_seed`;
DROP TEMPORARY TABLE IF EXISTS `mock_teacher_seed`;
DROP TEMPORARY TABLE IF EXISTS `mock_major_seed`;

SET @demo_password_hash = NULL;
