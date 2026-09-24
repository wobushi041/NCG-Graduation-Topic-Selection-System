package cn.com.edtechhub.worktopicselection.model.dto.user;

import cn.com.edtechhub.worktopicselection.model.dto.PageRequest;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 系部教师查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeptTeacherQueryRequest extends PageRequest implements Serializable {

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 教师系部
     */
    private String deptName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
