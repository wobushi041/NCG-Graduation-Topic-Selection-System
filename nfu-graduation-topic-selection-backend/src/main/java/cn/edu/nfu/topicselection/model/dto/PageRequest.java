package cn.edu.nfu.topicselection.model.dto;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import lombok.Data;

/**
 * 分页请求
 *
 * @author wobushi041
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;

    /**
     * 获取并校验排序顺序
     *
     * @return 排序顺序
     */
    public String getSortOrder() {
        // 参数检查
        if (!SqlUtils.validSortOrder(sortOrder)) {
            throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "排序顺序仅支持 ascend 或 descend");
        }
        return sortOrder;
    }

}
