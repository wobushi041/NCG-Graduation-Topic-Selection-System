package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.CommonConstant;
import cn.com.edtechhub.worktopicselection.mapper.DeptMapper;
import cn.com.edtechhub.worktopicselection.model.entity.Dept;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptQueryRequest;
import cn.com.edtechhub.worktopicselection.service.DeptService;
import cn.com.edtechhub.worktopicselection.utils.SqlUtils;
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
