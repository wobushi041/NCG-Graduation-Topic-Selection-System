INSERT INTO `college` (`id`, `collegeName`) VALUES (1, '示例工程学院');
INSERT INTO `topic_group` (`id`, `collegeId`, `groupName`) VALUES (1, 1, '软件工程选题组');
INSERT INTO `major` (`id`, `majorName`, `collegeId`, `topicGroupId`) VALUES (1, '示例软件工程专业', 1, 1);
INSERT INTO `user` (`id`, `userAccount`, `userName`, `userPassword`, `userRole`, `collegeId`)
VALUES (1, 'demo-teacher', '示例教师', '$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu', 1, 1);
INSERT INTO `topic` (`topic`, `type`, `description`, `requirement`, `teacherName`, `teacherAccount`, `topicGroupId`)
VALUES ('示例：校园设备预约系统', '软件系统', '仅用于自动化测试的虚构题目。', '掌握基础 Web 开发。', '示例教师', 'demo-teacher', 1);
