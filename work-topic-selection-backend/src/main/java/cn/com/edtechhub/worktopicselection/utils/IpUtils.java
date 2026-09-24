package cn.com.edtechhub.worktopicselection.utils;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * 客户端 IP 地址解析工具类
 *
 * @author wobushi041
 */
@Slf4j
public class IpUtils {

    /**
     * 从 HTTP 请求中解析客户端真实 IP 地址
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址字符串
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");

        // 处理真实用户的情况
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip != null && ip.equals("127.0.0.1")) {
                // 根据网卡取本机配置的 IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (Exception e) {
                    log.error("获取本机 IP 失败", e);
                }
                if (inet != null) {
                    ip = inet.getHostAddress();
                }
            }
        }

        // 处理多个代理的情况
        if (ip != null && ip.length() > 15) {
            // 第一个 IP 为客户端真实 IP，后面多个 IP 则按照 ',' 分割
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }

        // 处理本地测试的情况
        if (ip == null) {
            return "127.0.0.1";
        }

        return ip;
    }

}
