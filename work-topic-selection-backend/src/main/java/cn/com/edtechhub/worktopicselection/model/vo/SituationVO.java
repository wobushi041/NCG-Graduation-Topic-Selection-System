package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.Data;

/**
 * 选题情况脱敏类
 *
 * @author wobushi041
 */
@Data
public class SituationVO {

    /**
     * 总人数
     */
    private int Amount;

    /**
     * 已选题人数
     */
    private int selectAmount;

    /**
     * 未选题人数
     */
    private int unselectAmount;

}
