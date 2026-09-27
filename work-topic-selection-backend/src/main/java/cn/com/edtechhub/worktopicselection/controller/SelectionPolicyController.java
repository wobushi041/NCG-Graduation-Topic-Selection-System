package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.request.policy.SetDeptConfigRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptConfigVO;
import cn.com.edtechhub.worktopicselection.model.vo.TopicLockVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.SelectionPolicyService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统选题开关与跨系策略配置控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class SelectionPolicyController {

    /**
     * 注入选题开关与策略服务依赖
     */
    private final SelectionPolicyService selectionPolicyService;

    /**
     * 初始化选题开关与策略控制层
     *
     * @param selectionPolicyService 选题开关与策略服务
     */
    public SelectionPolicyController(SelectionPolicyService selectionPolicyService) {
        this.selectionPolicyService = selectionPolicyService;
    }

    /// 跨系选题、看题、单选与退选加锁开关 ///

    /**
     * 查询是否允许跨系状态
     *
     * @return 是否开启跨系选题
     */
    @SentinelRateLimit(resource = "policy.cross-topic.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/cross_topic")
    public BaseResponse<Boolean> getCrossTopicStatus() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getCrossTopicStatus());
    }

    /**
     * 设置是否允许跨系开关
     *
     * @param enabled 是否开启跨系选题
     * @return 操作结果提示信息
     */
    @SentinelRateLimit(resource = "policy.cross-topic.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/cross_topic")
    public BaseResponse<String> setCrossTopicStatus(@RequestParam boolean enabled) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.setCrossTopicStatus(enabled));
    }

    /**
     * 查询学生查看选题状态
     *
     * @return 是否允许学生查看选题
     */
    @SentinelRateLimit(resource = "policy.view-topic.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/view_topic")
    public BaseResponse<Boolean> getViewTopicStatus() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getViewTopicStatus());
    }

    /**
     * 设置学生查看选题开关
     *
     * @param enabled 是否允许学生查看选题
     * @return 操作结果提示信息
     */
    @SentinelRateLimit(resource = "policy.view-topic.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/view_topic")
    public BaseResponse<String> setViewTopicStatus(@RequestParam boolean enabled) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.setViewTopicStatus(enabled));
    }

    /**
     * 查询单选模式切换状态
     *
     * @return 当前单选模式开关状态
     */
    @SentinelRateLimit(resource = "policy.single-choice.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/switch_single_choice")
    public BaseResponse<Boolean> getSwitchSingleChoiceStatus() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getSwitchSingleChoiceStatus());
    }

    /**
     * 设置单选模式切换开关
     *
     * @param enabled 是否切换为学生单选模式
     * @return 操作结果提示信息
     */
    @SentinelRateLimit(resource = "policy.single-choice.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/switch_single_choice")
    public BaseResponse<String> setSwitchSingleChoiceStatus(@RequestParam boolean enabled) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.setSwitchSingleChoiceStatus(enabled));
    }

    /**
     * 查询是否退选加锁状态
     *
     * @return 退选加锁状态及锁定时间视图对象
     */
    @SentinelRateLimit(resource = "policy.topic-lock.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "teacher", "student"}, mode = SaMode.OR)
    @GetMapping("/topic_lock")
    public BaseResponse<TopicLockVO> getTopicLock() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getTopicLock());
    }

    /**
     * 设置是否退选加锁开关
     *
     * @param enabled   是否开启退选加锁
     * @param timestamp 加锁截止时间戳（秒）字符串
     * @return 操作结果提示信息
     */
    @SentinelRateLimit(resource = "policy.topic-lock.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic_lock")
    public BaseResponse<String> setTopicLock(@RequestParam boolean enabled, String timestamp) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.setTopicLock(enabled, timestamp));
    }

    /// 系部跨系选题配置 ///

    /**
     * 查看系部选跨选配置
     *
     * @return 系部跨选配置视图对象
     */
    @SentinelRateLimit(resource = "policy.dept-config.query")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/dept/config")
    public BaseResponse<DeptConfigVO> getDeptConfig() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.getDeptConfig());
    }

    /**
     * 设置系部选跨选配置
     *
     * @param request 设置系部跨选配置请求
     * @return 是否设置成功
     */
    @SentinelRateLimit(resource = "policy.dept-config.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/set/dept/config")
    public BaseResponse<Boolean> setDeptConfig(@RequestBody SetDeptConfigRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.setDeptConfig(request));
    }

    /**
     * 清除系部选跨选配置
     *
     * @return 是否清除成功
     */
    @SentinelRateLimit(resource = "policy.dept-config.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/del/dept/config")
    public BaseResponse<Boolean> delDeptConfig() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionPolicyService.delDeptConfig());
    }

}
