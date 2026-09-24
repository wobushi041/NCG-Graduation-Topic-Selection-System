package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.dto.topic.TopicQueryByAdminRequest;
import cn.com.edtechhub.worktopicselection.model.dto.topic.TopicQueryRequest;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 毕业设计题目业务服务接口
 *
 * @author wobushi041
 */
public interface TopicService extends IService<Topic> {

    /**
     * 根据题目查询请求构建通用查询条件封装
     *
     * @param topicQueryRequest 题目查询请求参数
     * @return 题目查询条件包装器
     */
    QueryWrapper<Topic> getQueryWrapper(TopicQueryRequest topicQueryRequest);

    /**
     * 根据管理员或系主任题目查询请求构建带系部与余量过滤的查询条件封装
     * TODO: 廖写的查询条件都有问题...就不应该这么传递参数的, 头疼真的, 我也不敢删除
     *
     * @param topicQueryByAdminRequest 管理员或系主任题目查询请求参数
     * @return 题目查询条件包装器
     */
    QueryWrapper<Topic> getTopicQueryByAdminWrapper(TopicQueryByAdminRequest topicQueryByAdminRequest);

}
