package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.request.policy.SetCollegeConfigRequest;
import cn.edu.nfu.topicselection.model.vo.CollegeConfigVO;
import cn.edu.nfu.topicselection.model.vo.TheSystemInfoVO;
import cn.edu.nfu.topicselection.model.vo.TopicLockVO;

/**
 * 系统选题开关、跨选策略配置与系统监控信息应用服务接口
 *
 * @author wobushi041
 */
public interface SelectionPolicyService {

    /**
     * 查询跨学院选题开关状态
     *
     * @return 是否开启跨学院选题
     */
    Boolean getCrossTopicStatus();

    /**
     * 设置跨学院选题开关状态
     *
     * @param enabled 是否开启跨学院选题
     * @return 操作结果提示信息
     */
    String setCrossTopicStatus(boolean enabled);

    /**
     * 查询学生查看选题开关状态
     *
     * @return 是否允许学生查看选题
     */
    Boolean getViewTopicStatus();

    /**
     * 设置学生查看选题开关状态
     *
     * @param enabled 是否允许学生查看选题
     * @return 操作结果提示信息
     */
    String setViewTopicStatus(boolean enabled);

    /**
     * 查询单选模式切换开关状态
     *
     * @return 当前单选模式开关状态
     */
    Boolean getSwitchSingleChoiceStatus();

    /**
     * 设置单选模式切换开关状态
     *
     * @param enabled 是否切换为学生单选模式
     * @return 操作结果提示信息
     */
    String setSwitchSingleChoiceStatus(boolean enabled);

    /**
     * 查询退选加锁状态及截止时间
     *
     * @return 退选加锁状态及锁定时间视图对象
     */
    TopicLockVO getTopicLock();

    /**
     * 设置退选加锁开关及截止时间
     *
     * @param enabled   是否开启退选加锁
     * @param timestamp 加锁截止时间戳（秒）字符串
     * @return 操作结果提示信息
     */
    String setTopicLock(boolean enabled, String timestamp);

    /**
     * 查询学院跨学院选题映射配置
     *
     * @return 学院跨选配置视图对象
     */
    CollegeConfigVO getCollegeConfig();

    /**
     * 设置学院跨学院选题映射配置
     *
     * @param request 设置学院跨选配置请求
     * @return 是否设置成功
     */
    Boolean setCollegeConfig(SetCollegeConfigRequest request);

    /**
     * 清除全部学院跨学院选题映射配置
     *
     * @return 是否清除成功
     */
    Boolean delCollegeConfig();

    /**
     * 查询系统统计与主机资源监控面板信息
     *
     * @return 系统统计与资源监控信息视图对象
     */
    TheSystemInfoVO getSystemInfo();

}
