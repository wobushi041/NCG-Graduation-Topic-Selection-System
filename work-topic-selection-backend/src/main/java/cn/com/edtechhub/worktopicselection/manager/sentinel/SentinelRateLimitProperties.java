package cn.com.edtechhub.worktopicselection.manager.sentinel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证接口 Sentinel QPS 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.sentinel.auth")
public class SentinelRateLimitProperties {

    private double login = 100;
    private double logout = 300;
    private double roleSwitch = 100;
    private double roleSwitchAvailability = 300;
    private double passwordAdminReset = 20;
    private double passwordChange = 50;
    private double passwordReset = 50;
    private double passwordResetCodeSend = 20;
    private double emailCodeSend = 20;
    private double emailCodeVerify = 100;
}
