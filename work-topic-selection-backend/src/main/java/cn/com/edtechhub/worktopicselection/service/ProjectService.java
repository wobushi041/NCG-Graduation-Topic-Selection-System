package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectQueryRequest;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 专业业务服务接口
 *
 * @author wobushi041
 */
public interface ProjectService extends IService<Project> {

    /**
     * 根据专业查询请求构建查询条件封装
     *
     * @param projectQueryRequest 专业查询请求参数
     * @return 专业查询条件包装器
     */
    QueryWrapper<Project> getQueryWrapper(ProjectQueryRequest projectQueryRequest);

}
