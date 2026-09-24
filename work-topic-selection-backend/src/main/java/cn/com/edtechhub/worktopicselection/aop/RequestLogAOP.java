package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.utils.DeviceUtils;
import cn.com.edtechhub.worktopicselection.utils.IpUtils;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 请求日志与恶意流量拦截切面
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class RequestLogAOP implements HandlerInterceptor {

    /**
     * 注入 Redis 管理器依赖
     */
    @Resource
    private RedisManager redisManager;

    /**
     * 最大请求次数限制
     */
    private static final int MAX_REQUESTS = 350;

    /**
     * 统计时间窗口秒数
     */
    private static final int TIME_WINDOW_SECONDS = 50;

    /**
     * 封禁持续时间秒数
     */
    private static final int BAN_TIME_SECONDS = 60;

    /**
     * 前置拦截客户端请求，打印访问日志并检测恶意高频流量进行短期封禁
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param handler  目标处理器对象
     * @return 是否放行当前请求
     */
    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        // 打印来源日志信息
        String device = DeviceUtils.getRequestDevice(request);
        String ip = IpUtils.getIpAddress(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String loginId = "外来访客";
        if (StpUtil.isLogin()) {
            loginId = StpUtil.getLoginIdAsString();
        }
        int count = 0;
        Date unbanDate = null;
        String deviceInfo = DeviceUtils.getRequestDeviceInfo(request);

        // 设置恶意流量拦截器(不要对外来访客进行拦截, 否则有可能把整个学校的 ip 都屏蔽)
        if (!loginId.equals("外来访客")) {
            // 设置 Redis 键名
            String redisKey = "user:requests:" + loginId;

            // 获取当前请求次数
            String countStr = redisManager.getValue(redisKey);
            count = countStr == null ? 0 : Integer.parseInt(countStr);

            // 设置白名单
            List<Long> ids = new ArrayList<>(); // 把 9 - 16 的用户加入白名单
            for (long i = 1; i <= 16; i++) {
                ids.add(i);
            }

            // 不在白名单中, 并且超过限制就封禁
            if (count >= MAX_REQUESTS && !ids.contains(Long.parseLong(loginId))) {
                // 获取解禁日期
                long remainingSeconds = StpUtil.getDisableTime(loginId); // 剩余封禁秒数
                long unbanTimestamp = System.currentTimeMillis() + remainingSeconds * 1000; // 毫秒时间戳
                unbanDate = new Date(unbanTimestamp); // 转为日期对象
                if (remainingSeconds > 0) {
                    ThrowUtils.throwIf(true, CodeBindMessageEnums.USER_DISABLE_ERROR, "您的帐号被封禁中, 请等待一段时间再操作, 将在 " + unbanDate + " 解封, 请不要恶意访问本站");
                }

                // 短暂封禁用户
                StpUtil.disable(loginId, BAN_TIME_SECONDS);
                log.info("[RequestLogAOP] 拦截到请求, 来自: {} {} == {} {} {} {}_{} {} == {}", ip, device, loginId, method, uri, count, MAX_REQUESTS, unbanDate, deviceInfo);
                ThrowUtils.throwIf(true, CodeBindMessageEnums.USER_DISABLE_ERROR, "您的帐号被封禁中, 请等待一段时间再操作, 将在 " + unbanDate + " 解封, 请不要恶意访问本站");
            } else {
                // 继续更新键值
                redisManager.setValue(redisKey, String.valueOf(count + 1), TIME_WINDOW_SECONDS);
            }
        }
        log.info("[RequestLogAOP] 拦截到请求, 来自: {} {} == {} {} {} {}_{} {} == {}", ip, device, loginId, method, uri, count, MAX_REQUESTS, "not-ban", deviceInfo);
        return true; // 返回 false 会终止请求, 可以利用这一点进行 IP 屏蔽
    }

}
