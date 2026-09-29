package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.mapper.MajorMapper;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.request.organization.MajorQueryRequest;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 专业业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class MajorServiceImpl extends ServiceImpl<MajorMapper, Major>
        implements MajorService {

    /**
     * 基于 MyBatis-Plus QueryWrapper 组装专业名称、学院及选题组查询条件
     *
     * @param majorQueryRequest 专业查询请求参数
     * @return 专业查询条件包装器
     */
    @Override
    public QueryWrapper<Major> getQueryWrapper(MajorQueryRequest majorQueryRequest) {
        if (majorQueryRequest == null) {
            throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "请求参数为空");
        }
        String sortField = majorQueryRequest.getSortField();
        String sortOrder = majorQueryRequest.getSortOrder();
        QueryWrapper<Major> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(org.apache.commons.lang3.StringUtils.isNotBlank(majorQueryRequest.getMajorName()),
                "majorName", majorQueryRequest.getMajorName());
        queryWrapper.eq(majorQueryRequest.getCollegeId() != null, "collegeId", majorQueryRequest.getCollegeId());
        queryWrapper.eq(majorQueryRequest.getTopicGroupId() != null, "topicGroupId",
                majorQueryRequest.getTopicGroupId());
        queryWrapper.orderBy(SqlUtils.validMajorSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
