package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.request.organization.DeptQueryRequest;
import cn.edu.nfu.topicselection.model.entity.Dept;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 系部业务服务接口
 *
 * @author wobushi041
 */
public interface DeptService extends IService<Dept> {

    /**
     * 根据系部查询请求构建查询条件封装
     *
     * @param deptQueryRequest 系部查询请求参数
     * @return 系部查询条件包装器
     */
    QueryWrapper<Dept> getQueryWrapper(DeptQueryRequest deptQueryRequest);

}
