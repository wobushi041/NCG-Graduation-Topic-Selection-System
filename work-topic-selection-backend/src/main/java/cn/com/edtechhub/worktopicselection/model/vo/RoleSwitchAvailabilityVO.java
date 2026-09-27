package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 角色切换可用性结果
 *
 * @author wobushi041
 */
@Data
@AllArgsConstructor
public class RoleSwitchAvailabilityVO {

    /**
     * 是否存在可切换角色
     */
    private boolean available;

    /**
     * 可切换的目标角色标识
     */
    private String targetRole;

}
