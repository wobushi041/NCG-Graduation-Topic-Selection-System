package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量取消发布课题的处理结果视图
 *
 * @author wobushi041
 */
@Data
public class UnpublishTopicResultVO {

    /**
     * 成功取消发布的课题 id 列表
     */
    private List<Long> cancelledTopicIds;

    /**
     * 因业务限制被跳过的课题列表
     */
    private List<UnpublishTopicSkippedVO> skippedTopics;

}
