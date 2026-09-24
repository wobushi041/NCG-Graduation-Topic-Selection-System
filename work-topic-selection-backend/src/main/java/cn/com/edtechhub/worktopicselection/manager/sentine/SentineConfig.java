package cn.com.edtechhub.worktopicselection.manager.sentine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Sentinel 流量控制配置属性类
 *
 * @author wobushi041
 */
@Component
@Data
@Slf4j
public class SentineConfig {

    /**
     * 系统预估总用户数（默认配置为 2000）
     */
    private double u = 2000;

    /**
     * 高峰期同时在线请求比例（取值范围 0 ~ 1，如 0.75 表示 75% 同时在线）
     */
    private double p = 0.75;

    /**
     * 单个在线用户每秒平均请求接口次数
     */
    private double r = 2;

    /**
     * 突发流量安全系数（通常取值 1 ~ 2，默认配置为 1）
     */
    private double safety = 1;

    /**
     * 根据并发模型公式计算得出的默认每秒查询率（QPS）阈值
     */
    private Double qps = u * p * r * safety;

    /**
     * 容器初始化完成后输出 Sentinel 流量控制参数与 QPS 阈值日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[{}] u: {}", this.getClass().getSimpleName(), this.u);
        log.debug("[{}] p: {}", this.getClass().getSimpleName(), this.p);
        log.debug("[{}] r: {}", this.getClass().getSimpleName(), this.r);
        log.debug("[{}] safety: {}", this.getClass().getSimpleName(), this.safety);
        log.debug("[{}] qps: {}", this.getClass().getSimpleName(), this.qps);
    }

}
