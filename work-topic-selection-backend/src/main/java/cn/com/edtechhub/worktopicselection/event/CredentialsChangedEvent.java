package cn.com.edtechhub.worktopicselection.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户凭证变更完成事件
 *
 * @author wobushi041
 */
@Getter
@AllArgsConstructor
public class CredentialsChangedEvent {

    /**
     * 凭证发生变更的用户 id
     */
    private final Long userId;

}
