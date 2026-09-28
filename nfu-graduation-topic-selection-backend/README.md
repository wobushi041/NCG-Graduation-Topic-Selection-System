# NCG Graduation Topic Selection :: Server

本模块为「广州南方学院毕设选题管理系统」的 Spring Boot 后端服务模块（`cn.edu.nfu:graduation-topic-selection-server`）。

- **Java 根包**：`cn.edu.nfu.topicselection`
- **启动类**：`cn.edu.nfu.topicselection.TopicSelectionApplication`
- **Spring 应用名**：`nfu-topic-selection`
- **Sa-Token Cookie 名**：`nfu-topic-selection`
- **Redis / Caffeine 前缀**：`nfu:topic-selection:`
- **默认数据库名**：`nfu_topic_selection`

## 常用命令（在仓库根目录执行）

```powershell
# 运行单元测试
.\mvnw.cmd -pl nfu-graduation-topic-selection-backend -am test

# 打包普通 Jar 与可执行 -exec.jar
.\mvnw.cmd -pl nfu-graduation-topic-selection-backend -am clean package -DskipTests

# 启动本地开发服务
.\mvnw.cmd -pl nfu-graduation-topic-selection-backend spring-boot:run
```

完整使用说明请参见仓库根目录 [README.md](../README.md)。
