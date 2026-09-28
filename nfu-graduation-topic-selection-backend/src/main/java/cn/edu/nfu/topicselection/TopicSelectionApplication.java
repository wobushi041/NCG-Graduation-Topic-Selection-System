package cn.edu.nfu.topicselection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 应用程序启动类
 *
 * @author wobushi041
 */
@Slf4j
@SpringBootApplication()
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TopicSelectionApplication {

    /**
     * 启动 Spring Boot 应用程序
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TopicSelectionApplication.class, args);
        log.info("http://127.0.0.1:8000/doc.html");
    }

}

