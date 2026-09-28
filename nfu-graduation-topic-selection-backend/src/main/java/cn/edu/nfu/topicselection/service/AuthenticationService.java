package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.request.auth.LoginRequest;
import cn.edu.nfu.topicselection.model.request.auth.RoleSwitchRequest;
import cn.edu.nfu.topicselection.model.vo.LoginUserVO;
import cn.edu.nfu.topicselection.model.vo.RoleSwitchAvailabilityVO;

/**
 * 提供登录会话和角色切换业务能力
 *
 * @author wobushi041
 */
public interface AuthenticationService {

    /**
     * 校验账号凭证并建立登录会话
     *
     * @param request  登录请求
     * @param clientIp 客户端 IP
     * @param device   登录设备类型
     * @return 登录用户信息
     */
    LoginUserVO login(LoginRequest request, String clientIp, String device);

    /**
     * 注销当前登录会话
     *
     * @return 是否注销成功
     */
    boolean logout();

    /**
     * 在教师与专业负责人身份之间切换登录会话
     *
     * @param request 角色切换请求
     * @param device  登录设备类型
     * @return 切换后的登录用户信息
     */
    LoginUserVO switchRole(RoleSwitchRequest request, String device);

    /**
     * 查询当前账号的角色切换可用性
     *
     * @return 角色切换可用性
     */
    RoleSwitchAvailabilityVO getRoleSwitchAvailability();

}
