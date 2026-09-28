package cn.edu.nfu.topicselection.constant;

/**
 * 选题模块常量接口
 *
 * @author wobushi041
 */
public interface TopicConstant {

    /**
     * 默认审核打回理由的最大长度
     */
    Integer MAX_REASON_SIZE = 1024;

    /**
     * 查看选题开关缓存 Key
     */
    String VIEW_TOPIC_SWITCH = "view-topic-switch";

    /**
     * 跨系选题开关缓存 Key
     */
    String CROSS_TOPIC_SWITCH = "cross-topic-switch";

    /**
     * 单选角色开关缓存 Key
     */
    String SWITCH_SINGLE_CHOICE = "switch-single-choice";

    /**
     * 是否退选加锁缓存 Key
     */
    String TOPIC_LOCK = "topic-lock";

    /**
     * 退选加锁时间缓存 Key
     */
    String TOPIC_LOCK_TIME = "topic-lock-time";

    /**
     * 跨系选题配置缓存 Key
     */
    String DEPT_CROSS_TOPIC_CONFIG = "dept-cross-topic-config";

}
