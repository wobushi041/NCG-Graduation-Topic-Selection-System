package cn.com.edtechhub.worktopicselection.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 学生专题选择状态枚举体
 *
 * @author wobushi041
 */
@Getter
public enum StudentTopicSelectionStatusEnum {

    /**
     * 取消预先选择
     */
    UN_PRESELECT(-1, "取消预先选择"),

    /**
     * 确认预先选择
     */
    EN_PRESELECT(0, "确认预先选择"),

    /**
     * 取消确认选择
     */
    UN_SELECT(1, "取消确认选择"),

    /**
     * 确定确认选择
     */
    EN_SELECT(2, "确定确认选择")
    ;

    /**
     * 码值
     */
    private final int code;

    /**
     * 描述
     */
    private final String description;

    /**
     * 内部构造方法
     *
     * @param code        码值
     * @param description 描述
     */
    StudentTopicSelectionStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据码值获取枚举
     *
     * @param code 码值
     * @return 枚举实例
     */
    public static StudentTopicSelectionStatusEnum getEnums(int code) {
        if (ObjUtil.isEmpty(code)) {
            return null;
        }
        for (StudentTopicSelectionStatusEnum enumItem : StudentTopicSelectionStatusEnum.values()) {
            if (enumItem.getCode() == code) {
                return enumItem;
            }
        }
        return null;
    }

}
