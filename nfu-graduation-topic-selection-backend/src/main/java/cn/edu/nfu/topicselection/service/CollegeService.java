package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.entity.College;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 学院业务服务接口
 *
 * @author wobushi041
 */
public interface CollegeService extends IService<College> {

    /**
     * 根据学院查询请求构建查询条件封装
     *
     * @param collegeQueryRequest 学院查询请求参数
     * @return 学院查询条件包装器
     */
    QueryWrapper<College> getQueryWrapper(CollegeQueryRequest collegeQueryRequest);

}
