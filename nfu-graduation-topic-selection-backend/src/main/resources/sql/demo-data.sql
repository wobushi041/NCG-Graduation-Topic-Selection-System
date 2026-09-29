-- Entirely fictional demonstration data. No real accounts or credentials are included.

USE `nfu_topic_selection`;

SET NAMES utf8mb4;

INSERT INTO `college` (`collegeName`)
SELECT '示例工程学院'
WHERE NOT EXISTS (SELECT 1 FROM `college` WHERE `collegeName` = '示例工程学院');

INSERT INTO `topic_group` (`collegeId`, `groupName`)
SELECT c.id, '示例计算机类选题组'
FROM `college` c
WHERE c.`collegeName` = '示例工程学院'
  AND NOT EXISTS (
    SELECT 1
    FROM `topic_group` g
    WHERE g.`collegeId` = c.id
      AND g.`groupName` = '示例计算机类选题组'
  );

INSERT INTO `major` (`majorName`, `collegeId`, `topicGroupId`)
SELECT '示例软件工程专业', c.id, g.id
FROM `college` c
JOIN `topic_group` g ON g.`collegeId` = c.id
WHERE c.`collegeName` = '示例工程学院'
  AND g.`groupName` = '示例计算机类选题组'
  AND NOT EXISTS (
    SELECT 1
    FROM `major` m
    WHERE m.`collegeId` = c.id
      AND m.`majorName` = '示例软件工程专业'
  );

INSERT INTO `user` (`userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`, `status`, `email`)
SELECT 'demo-teacher', '示例教师', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, c.id, '老用户', 'demo-teacher@example.invalid'
FROM `college` c
WHERE c.`collegeName` = '示例工程学院'
  AND NOT EXISTS (SELECT 1 FROM `user` WHERE `userAccount` = 'demo-teacher');

INSERT INTO `user` (`userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`, `topicGroupId`, `status`, `email`)
SELECT 'demo-topic-leader', '示例选题负责人', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2, c.id, g.id, '老用户', 'demo-leader@example.invalid'
FROM `college` c
JOIN `topic_group` g ON g.`collegeId` = c.id
WHERE c.`collegeName` = '示例工程学院'
  AND g.`groupName` = '示例计算机类选题组'
  AND NOT EXISTS (SELECT 1 FROM `user` WHERE `userAccount` = 'demo-topic-leader');

INSERT INTO `user` (`userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`, `majorId`, `status`, `email`)
SELECT 'demo-student', '示例学生', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 0, c.id, m.id, '老用户', 'demo-student@example.invalid'
FROM `college` c
JOIN `major` m ON m.`collegeId` = c.id
WHERE c.`collegeName` = '示例工程学院'
  AND m.`majorName` = '示例软件工程专业'
  AND NOT EXISTS (SELECT 1 FROM `user` WHERE `userAccount` = 'demo-student');

INSERT INTO `teacher_group_quota` (`teacherAccount`, `topicGroupId`, `maxTopics`)
SELECT 'demo-teacher', g.id, 5
FROM `topic_group` g
JOIN `college` c ON c.id = g.`collegeId`
WHERE c.`collegeName` = '示例工程学院'
  AND g.`groupName` = '示例计算机类选题组'
ON DUPLICATE KEY UPDATE `maxTopics` = VALUES(`maxTopics`);

INSERT INTO `topic` (`topic`, `type`, `description`, `requirement`, `teacherName`, `teacherAccount`, `topicGroupId`)
SELECT '示例：校园设备预约系统', '软件系统', '仅用于本地功能演示的虚构题目。', '掌握基础 Web 开发。', '示例教师', 'demo-teacher', g.id
FROM `topic_group` g
JOIN `college` c ON c.id = g.`collegeId`
WHERE c.`collegeName` = '示例工程学院'
  AND g.`groupName` = '示例计算机类选题组'
  AND NOT EXISTS (SELECT 1 FROM `topic` WHERE `topic` = '示例：校园设备预约系统');
