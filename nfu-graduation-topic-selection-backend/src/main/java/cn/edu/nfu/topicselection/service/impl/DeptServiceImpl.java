package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.mapper.DeptMapper;
import cn.edu.nfu.topicselection.model.entity.Dept;
import cn.edu.nfu.topicselection.model.request.organization.DeptQueryRequest;
import cn.edu.nfu.topicselection.service.DeptService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 系部业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept>
        implements DeptService {

    /**
     * 基于 MyBatis-Plus QueryWrapper 组装 deptName 模糊匹配及经过 SqlUtils 校验的排序条件
     *
     * @param deptQueryRequest 系部查询请求参数
     * @return 系部查询条件包装器
     */
    @Override
    public QueryWrapper<Dept> getQueryWrapper(DeptQueryRequest deptQueryRequest) {
        String sortField = deptQueryRequest.getSortField();
        String sortOrder = deptQueryRequest.getSortOrder();
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(deptQueryRequest.getDeptName()), "deptName", deptQueryRequest.getDeptName());
        queryWrapper.orderBy(SqlUtils.validDeptSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
