package cn.com.edtechhub.worktopicselection.manager.ai;

import lombok.Data;

/**
 * AI 选题查重评估结果模型
 *
 * @author wobushi041
 */
@Data
public class AIResult {

    /**
     * 相似程度等级（0 - 无相似点，1 - 部分相似，2 - 完全相似）
     */
    private String level;

    /**
     * 相似程度评估详细描述
     */
    private String description;

}
