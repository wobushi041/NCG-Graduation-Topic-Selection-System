package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryRequest;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 毕业设计题目业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入学生选题关联服务依赖
     */
    @Resource
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 基于 MyBatis-Plus QueryWrapper 结合当前用户角色组装题目和选题组查询条件
     *
     * @param topicQueryRequest 题目查询请求参数
     * @return 题目查询条件包装器
     */
    @Override
    public QueryWrapper<Topic> getQueryWrapper(TopicQueryRequest topicQueryRequest) {
        ThrowUtils.throwIf(topicQueryRequest == null, CodeBindMessageEnums.PARAMS_ERROR, "请求参数为空");
        assert topicQueryRequest != null;

        String sortField = topicQueryRequest.getSortField();
        String sortOrder = topicQueryRequest.getSortOrder();
        QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
        User currentLoginUser = userService.userGetCurrentLoginUser();
        queryWrapper.eq(userService.userIsAdmin(currentLoginUser) || userService.userIsTopicLeader(currentLoginUser), "status", topicQueryRequest.getStatus());
        queryWrapper.like(StringUtils.isNotBlank(topicQueryRequest.getTopic()), "topic", topicQueryRequest.getTopic());
        queryWrapper.like(StringUtils.isNotBlank(topicQueryRequest.getType()), "type", topicQueryRequest.getType());
        queryWrapper.eq(StringUtils.isNotBlank(topicQueryRequest.getTeacherName()), "teacherName", topicQueryRequest.getTeacherName());
        queryWrapper.eq(topicQueryRequest.getTopicGroupId() != null, "topicGroupId",
                topicQueryRequest.getTopicGroupId());
        queryWrapper.eq(userService.userIsTopicLeader(currentLoginUser), "topicGroupId",
                currentLoginUser.getTopicGroupId());
        queryWrapper.eq(topicQueryRequest.getStartTime() != null, "startTime", topicQueryRequest.getStartTime());
        queryWrapper.eq(topicQueryRequest.getEndTime() != null, "endTime", topicQueryRequest.getEndTime());
        queryWrapper.orderBy(SqlUtils.validTopicSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    /**
     * 基于 MyBatis-Plus QueryWrapper 绑定选题负责人所属选题组并筛选有剩余名额的题目
     *
     * @param topicQueryByAdminRequest 管理员或选题负责人题目查询请求参数
     * @return 题目查询条件包装器
     */
    @Override
    public QueryWrapper<Topic> getTopicQueryByAdminWrapper(TopicQueryByAdminRequest topicQueryByAdminRequest) {
        // 检查请求参数是否为空
        if (topicQueryByAdminRequest == null) {
            throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "请求参数为空");
        }

        // 检查用户是否登录
        final User loginUser = userService.userGetCurrentLoginUser();

        // 获取排序字段和排序顺序
        String sortField = topicQueryByAdminRequest.getSortField();
        String sortOrder = topicQueryByAdminRequest.getSortOrder();

        // 创建查询包装器
        QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(topicQueryByAdminRequest.getTopic()), "topic", topicQueryByAdminRequest.getTopic());
        queryWrapper.like(StringUtils.isNotBlank(topicQueryByAdminRequest.getType()), "type", topicQueryByAdminRequest.getType());
        queryWrapper.eq(StringUtils.isNotBlank(topicQueryByAdminRequest.getTeacherName()), "teacherName", topicQueryByAdminRequest.getTeacherName());
        queryWrapper.eq(topicQueryByAdminRequest.getTopicGroupId() != null, "topicGroupId",
                topicQueryByAdminRequest.getTopicGroupId());
        queryWrapper.eq(topicQueryByAdminRequest.getStartTime() != null, "startTime", topicQueryByAdminRequest.getStartTime());
        queryWrapper.eq(topicQueryByAdminRequest.getEndTime() != null, "endTime", topicQueryByAdminRequest.getEndTime());
        queryWrapper.eq(userService.userIsTopicLeader(loginUser), "topicGroupId", loginUser.getTopicGroupId());
        // 设置查询条件，筛选出剩余问题数量为 1 的题目
        queryWrapper.eq("surplusQuantity", 1);
        queryWrapper.orderBy(SqlUtils.validTopicSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
