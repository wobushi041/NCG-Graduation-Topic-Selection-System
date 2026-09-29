package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.mapper.CollegeMapper;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 学院业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class CollegeServiceImpl extends ServiceImpl<CollegeMapper, College>
        implements CollegeService {

    /**
     * 基于 MyBatis-Plus QueryWrapper 组装 collegeName 模糊匹配及经过 SqlUtils 校验的排序条件
     *
     * @param collegeQueryRequest 学院查询请求参数
     * @return 学院查询条件包装器
     */
    @Override
    public QueryWrapper<College> getQueryWrapper(CollegeQueryRequest collegeQueryRequest) {
        String sortField = collegeQueryRequest.getSortField();
        String sortOrder = collegeQueryRequest.getSortOrder();
        QueryWrapper<College> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(collegeQueryRequest.getCollegeName()), "collegeName", collegeQueryRequest.getCollegeName());
        queryWrapper.orderBy(SqlUtils.validCollegeSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
