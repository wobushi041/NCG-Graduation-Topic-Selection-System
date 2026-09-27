package cn.com.edtechhub.worktopicselection.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CredentialsChangedEvent {
    private final Long userId;
}
