package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.vo.TheSystemInfoVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.SelectionPolicyService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统连通性诊断与监控面板控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class SystemController {

    /**
     * 注入选题开关与策略服务依赖
     */
    private final SelectionPolicyService selectionPolicyService;

    /**
     * 初始化系统诊断与监控控制层
     *
     * @param selectionPolicyService 选题开关与策略服务
     */
    public SystemController(SelectionPolicyService selectionPolicyService) {
        this.selectionPolicyService = selectionPolicyService;
    }

    /**
     * 测试接口
     *
     * @return 测试响应
     */
    @SentinelRateLimit(resource = "system.diagnostics.test")
    @SaIgnore
    @GetMapping("/test")
    public BaseResponse<String> test() {
        return TheResult.notyet();
    }

    /**
     * 查看系统的相关信息面板信息
     *
     * @return 系统统计与资源监控信息视图对象
     */
    @SentinelRateLimit(resource = "system.info.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/system/info")
    public BaseResponse<TheSystemInfoVO> getSystemInfo() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getSystemInfo());
    }

}
