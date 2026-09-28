package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.request.ai.AiSendRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.AIApplicationService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * AI 问答应用服务实现类
 *
 * @author wobushi041
 */
@Service
public class AIApplicationServiceImpl implements AIApplicationService {

    /// AI 问答服务实现 ///

    /**
     * 校验请求体与消息内容非空后返回 AI 问答暂未开放响应
     *
     * @param request AI 问答请求
     * @return AI 回复结果响应
     */
    @Override
    public BaseResponse<String> aiSend(AiSendRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String content = request.getContent();
        ThrowUtils.throwIf(StringUtils.isBlank(content), CodeBindMessageEnums.PARAMS_ERROR, "请不要发送空消息");

        return TheResult.notyet("AI 问答功能暂未开放");
    }

}
