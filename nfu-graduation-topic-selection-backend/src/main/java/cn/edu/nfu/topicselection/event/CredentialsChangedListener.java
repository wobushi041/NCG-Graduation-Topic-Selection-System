package cn.edu.nfu.topicselection.event;

import cn.edu.nfu.topicselection.manager.satoken.AuthSessionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户凭证变更事务事件监听器
 *
 * @author wobushi041
 */
@Component
public class CredentialsChangedListener {

    /**
     * 注入认证会话管理器依赖
     */
    private final AuthSessionManager authSessionManager;

    /**
     * 初始化用户凭证变更事件监听器
     *
     * @param authSessionManager 认证会话管理器
     */
    public CredentialsChangedListener(AuthSessionManager authSessionManager) {
        this.authSessionManager = authSessionManager;
    }

    /**
     * 在凭证更新事务提交后注销目标用户会话
     *
     * @param event 用户凭证变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCredentialsChanged(CredentialsChangedEvent event) {
        authSessionManager.logoutUser(event.getUserId());
    }

}
