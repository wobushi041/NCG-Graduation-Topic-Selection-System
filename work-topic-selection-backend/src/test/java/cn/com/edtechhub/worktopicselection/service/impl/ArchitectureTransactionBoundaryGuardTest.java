package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.WorkTopicSelectionApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 全局事务边界与 AOP 架构红线守护测试
 *
 * @author wobushi041
 */
class ArchitectureTransactionBoundaryGuardTest {

    // 场景：测试 5 个基础 ServiceImpl 与只读查询服务均无类级 @Transactional 注解（守护 ARCH-05）
    @Test
    void foundationalAndQueryServices_shouldHaveZeroClassLevelTransactional() {
        // 1. 准备测试数据并列出所有应免除类级写事务的 ServiceImpl
        List<Class<?>> nonClassTransactionalServices = Arrays.asList(
                UserServiceImpl.class,
                TopicServiceImpl.class,
                StudentTopicSelectionServiceImpl.class,
                DeptServiceImpl.class,
                ProjectServiceImpl.class,
                TopicSelectionQueryServiceImpl.class,
                SelectionReportServiceImpl.class,
                AIApplicationServiceImpl.class,
                TopicSelectionApplicationServiceImpl.class,
                TopicApplicationServiceImpl.class,
                OrganizationApplicationServiceImpl.class,
                UserApplicationServiceImpl.class,
                SelectionPolicyServiceImpl.class,
                FileApplicationServiceImpl.class
        );

        // 2. 逐个检查类级是否存在 @Transactional 注解
        for (Class<?> clazz : nonClassTransactionalServices) {
            Transactional classTx = clazz.getAnnotation(Transactional.class);
            Assertions.assertNull(classTx, clazz.getSimpleName() + " 不应标注类级 @Transactional");
        }

        // 3. 断言检查类数量完整覆盖 14 个核心服务实现
        Assertions.assertEquals(14, nonClassTransactionalServices.size());
    }

    // 场景：测试全仓 23 个多步写用例方法均显式标注方法级 @Transactional（守护 AOP-005）
    @Test
    void applicationWriteServices_shouldHave23MethodLevelTransactionalMethods() {
        // 1. 准备测试数据并列出承载写用例的 6 个应用服务实现类
        List<Class<?>> writeServiceClasses = Arrays.asList(
                PasswordServiceImpl.class,
                TopicSelectionApplicationServiceImpl.class,
                TopicApplicationServiceImpl.class,
                OrganizationApplicationServiceImpl.class,
                UserApplicationServiceImpl.class,
                FileApplicationServiceImpl.class
        );

        // 2. 统计所有方法级 @Transactional 数量
        int transactionalMethodCount = 0;
        for (Class<?> clazz : writeServiceClasses) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Transactional.class)) {
                    transactionalMethodCount++;
                }
            }
        }

        // 3. 断言方法级事务总数精确等于 23 个写用例
        Assertions.assertEquals(23, transactionalMethodCount);
    }

    // 场景：测试 WorkTopicSelectionApplication 收敛 EnableAspectJAutoProxy 的 exposeProxy 配置为 false（守护 AOP-006）
    @Test
    void workTopicSelectionApplication_shouldDisableUnnecessaryExposeProxy() {
        // 1. 准备测试数据并获取启动类上的 @EnableAspectJAutoProxy 注解
        EnableAspectJAutoProxy autoProxy = WorkTopicSelectionApplication.class.getAnnotation(EnableAspectJAutoProxy.class);

        // 2. 校验注解存在性及配置属性
        Assertions.assertNotNull(autoProxy);

        // 3. 断言 proxyTargetClass 为 true 且 exposeProxy 为 false
        Assertions.assertTrue(autoProxy.proxyTargetClass());
        Assertions.assertFalse(autoProxy.exposeProxy());
    }

}
