package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.model.request.ai.AiSendRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.AIApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/ai")
public class AIController {

    /**
     * 注入 AI 问答应用服务依赖
     */
    private final AIApplicationService aiApplicationService;

    /**
     * 构造智能控制层实例
     *
     * @param aiApplicationService AI 问答应用服务
     */
    public AIController(AIApplicationService aiApplicationService) {
        this.aiApplicationService = aiApplicationService;
    }

    /// AI 问答接口 ///

    /**
     * 快速提供 AI 询问接口
     *
     * @param request AI 询问请求
     * @return AI 回复结果
     */
    @SentinelRateLimit(resource = "ai.chat.send")
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @PostMapping("/send")
    public BaseResponse<String> aiSend(@RequestBody AiSendRequest request) {
        return aiApplicationService.aiSend(request);
    }

}
