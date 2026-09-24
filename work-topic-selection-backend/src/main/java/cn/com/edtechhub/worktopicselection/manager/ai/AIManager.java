package cn.com.edtechhub.worktopicselection.manager.ai;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;

/**
 * AI 对话与选题查重服务管理器
 *
 * @author wobushi041
 */
@Component
public class AIManager {

    /**
     * 注入 AI 配置依赖
     */
    @Resource
    private AIConfig aiConfig;

    /**
     * 注入 JSON 序列化映射器依赖
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 调用腾讯云知识引擎接口发送 AI 查重请求并解析 JSON 评估结果
     *
     * @param userId      用户唯一标识
     * @param userContent 待查重的选题提问内容
     * @return 解析后的 AI 查重评估结果对象
     */
    public AIResult sendAi(
            String userId,
            String userContent
    ) {
        if (StringUtils.isBlank(aiConfig.getBotAppKey())) {
            throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "AI 服务尚未配置");
        }
        // 请求 AI
        String response = this.sendAiRequest(userId, userId, userContent);

        // 转化为包含 Map 事件的对象
        AIResponse aiResponse = this.mapToAiResponse(response);

        // 获取最终回复事件 Map
        Map<String, Object> secondReplyMap = aiResponse.getSecondReply();
        String payload = secondReplyMap.get("payload").toString();
        String content;
        try {
            JsonNode payloadJson = objectMapper.readTree(payload);
            content = payloadJson.path("content").asText();
        } catch (JsonProcessingException e) {
            throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "解析 AI 响应数据失败");
        }
        // 设置正则表达式规则
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*}");
        Matcher matcher = pattern.matcher(content);

        // 提取检验结果对象
        AIResult aiResult = null;
        if (matcher.find()) {
            String json = matcher.group();
            try {
                aiResult = objectMapper.readValue(json, AIResult.class);
            } catch (JsonProcessingException e) {
                throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "解析 AI 检验结果失败");
            }
        }
        return aiResult;
    }

    /**
     * 封装 HTTP 请求参数并通过 POST 调用远程 AI 接口获取原始响应体
     *
     * @param visitorBizId 访客业务唯一标识
     * @param sessionId    对话会话唯一标识
     * @param userContent  用户提问文本内容
     * @return 远程接口返回的 SSE 格式原始文本
     */
    private String sendAiRequest(
            String visitorBizId,
            String sessionId,
            String userContent
    ) {
        // 封装请求参数
        Map<String, Object> request = new HashMap<>();
        request.put("bot_app_key", aiConfig.getBotAppKey()); // 应用密钥
        request.put("visitor_biz_id", visitorBizId); // 用户 id
        request.put("session_id", sessionId); // 会话 id
        request.put("system_role", aiConfig.getSystemRole());
        request.put("content", userContent); // 用户提问内容
        request.put("streaming_throttle", 100); // 字符回包的积攒数量
        request.put("incremental", false); // 是否控制回复事件和思考事件中的 content 为增量内容（可以避免成为 SSE 调用）
        request.put("visitor_labels", Collections.emptyList()); // 知识标签列表
        request.put("search_network", "disable"); // 是否打开联网搜索
        request.put("stream", "disable"); // 是否流式输出
        request.put("workflow_status", "disable"); // 关闭工作流模式
        HttpResponse response;
        try {
            response = HttpRequest
                    .post(aiConfig.getRequestUrl())
                    .body(objectMapper.writeValueAsString(request))
                    .timeout(15_000)
                    .execute();
        } catch (JsonProcessingException e) {
            throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "构造 AI 请求失败");
        }

        if (!response.isOk()) {
            throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "AI 服务请求失败");
        }
        return response.body();
    }

    /**
     * 使用正则表达式提取 SSE 响应文本中的事件流并映射为 AIResponse 结构对象
     *
     * @param responseBody 接口返回的 SSE 原始响应字符串
     * @return 映射后的 AI 响应结构对象
     */
    private AIResponse mapToAiResponse(String responseBody) {
        // 正则表达式匹配 event 和对应的 data
        Pattern pattern = Pattern.compile("event:(.*?)\r?\ndata:(.*?)\r?\n", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseBody + "\n"); // 加个换行符确保最后一个匹配能被捕获

        // 计数器，用于区分同类型的 event
        int replyCount = 0;
        int tokenStatCount = 0;

        // 填充 AIResponse 对象
        AIResponse aiResponse = new AIResponse();
        while (matcher.find()) {
            String event = matcher.group(1).trim();
            String dataJson = matcher.group(2).trim();

            try {
                Map<String, Object> dataMap = objectMapper.readValue(
                        dataJson,
                        new TypeReference<Map<String, Object>>() {
                        }
                );

                // 根据 event 类型和计数器分配到不同的属性
                if ("reply".equals(event)) {
                    replyCount++;
                    if (replyCount == 1) {
                        aiResponse.setFirstReply(dataMap);
                    } else if (replyCount == 2) {
                        aiResponse.setSecondReply(dataMap);
                    }
                } else if ("token_stat".equals(event)) {
                    tokenStatCount++;
                    if (tokenStatCount == 1) {
                        aiResponse.setFirstTokenStat(dataMap);
                    } else if (tokenStatCount == 2) {
                        aiResponse.setSecondTokenStat(dataMap);
                    }
                }
            } catch (Exception e) {
                throw new BusinessException(CodeBindMessageEnums.OPERATION_ERROR, "解析 AI 响应数据失败");
            }
        }

        return aiResponse;
    }

}
