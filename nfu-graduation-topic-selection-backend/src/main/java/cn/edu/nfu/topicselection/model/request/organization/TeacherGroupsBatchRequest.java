package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 教师选题组额度批量查询请求
 *
 * @author wobushi041
 */
@Data
public class TeacherGroupsBatchRequest implements Serializable {

    /**
     * 教师账号列表
     */
    private List<String> teacherAccounts;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
