package cn.edu.nfu.topicselection.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 选题审核状态枚举
 *
 * @author wobushi041
 */
@Getter
public enum TopicStatusEnum {

    /**
     * 未发布
     */
    NOT_PUBLISHED(0, "未发布"),

    /**
     * 已发布
     */
    PUBLISHED(1, "已发布"),

    /**
     * 待审核
     */
    PENDING_REVIEW(-1, "待审核"),

    /**
     * 被打回
     */
    REJECTED(-2, "被打回")
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
    TopicStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据码值获取枚举
     *
     * @param code 码值
     * @return 枚举实例
     */
    public static TopicStatusEnum getEnums(int code) {
        if (ObjUtil.isEmpty(code)) {
            return null;
        }
        for (TopicStatusEnum enumItem : TopicStatusEnum.values()) {
            if (enumItem.getCode() == code) {
                return enumItem;
            }
        }
        return null;
    }

}
