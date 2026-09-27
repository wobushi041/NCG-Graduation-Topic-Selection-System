package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.request.ai.AiSendRequest;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;

/**
 * AI 问答应用服务接口
 *
 * @author wobushi041
 */
public interface AIApplicationService {

    /// AI 问答服务契约 ///

    /**
     * 处理学生提交的 AI 问答请求并返回回复结果
     *
     * @param request AI 问答请求
     * @return AI 回复结果响应
     */
    BaseResponse<String> aiSend(AiSendRequest request);

}
