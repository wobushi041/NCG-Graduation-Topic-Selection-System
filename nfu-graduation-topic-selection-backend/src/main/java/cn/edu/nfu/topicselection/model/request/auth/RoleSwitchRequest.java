package cn.edu.nfu.topicselection.model.request.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 教师与专业负责人角色切换请求
 *
 * @author wobushi041
 */
@Data
public class RoleSwitchRequest {

    /**
     * 目标角色标识
     */
    @NotBlank(message = "目标角色不能为空")
    @Pattern(regexp = "teacher|dept", message = "目标角色只能是 teacher 或 dept")
    private String targetRole;

}
