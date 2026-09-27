package cn.com.edtechhub.worktopicselection.event;

import cn.com.edtechhub.worktopicselection.manager.satoken.AuthSessionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CredentialsChangedListener {

    private final AuthSessionManager authSessionManager;

    public CredentialsChangedListener(AuthSessionManager authSessionManager) {
        this.authSessionManager = authSessionManager;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCredentialsChanged(CredentialsChangedEvent event) {
        authSessionManager.logoutUser(event.getUserId());
    }
}
