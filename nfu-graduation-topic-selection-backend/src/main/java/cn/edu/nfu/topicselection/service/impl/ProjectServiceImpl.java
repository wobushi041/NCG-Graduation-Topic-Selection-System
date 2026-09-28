package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.mapper.ProjectMapper;
import cn.edu.nfu.topicselection.model.entity.Project;
import cn.edu.nfu.topicselection.model.request.organization.ProjectQueryRequest;
import cn.edu.nfu.topicselection.service.ProjectService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 专业业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>
        implements ProjectService {

    /**
     * 基于 MyBatis-Plus QueryWrapper 组装 projectName 与 deptName 模糊匹配及经过 SqlUtils 校验的排序条件
     *
     * @param projectQueryRequest 专业查询请求参数
     * @return 专业查询条件包装器
     */
    @Override
    public QueryWrapper<Project> getQueryWrapper(ProjectQueryRequest projectQueryRequest) {
        if (projectQueryRequest == null) {
            throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "请求参数为空");
        }
        String sortField = projectQueryRequest.getSortField();
        String sortOrder = projectQueryRequest.getSortOrder();
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(projectQueryRequest.getProjectName()), "projectName", projectQueryRequest.getProjectName());
        queryWrapper.like(StringUtils.isNotBlank(projectQueryRequest.getDeptName()), "deptName", projectQueryRequest.getDeptName());
        queryWrapper.orderBy(SqlUtils.validProjectSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
