package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleSwitchAvailabilityVO {
    private boolean available;
    private String targetRole;
}
