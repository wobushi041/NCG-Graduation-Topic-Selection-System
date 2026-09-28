package cn.edu.nfu.topicselection.manager.bootstrap;

import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员账号引导组件测试
 *
 * @author wobushi041
 */
class AdminBootstrapTest {

    // 场景：测试引导配置为空时不创建管理员
    @Test
    void blankConfigurationDoesNothing() {
        // 1. 准备空白引导配置
        UserService userService = mock(UserService.class);
        AdminBootstrap bootstrap = bootstrap(userService, mock(PasswordService.class), "", "", "");

        // 2. 执行管理员账号引导
        bootstrap.run();

        // 3. 断言未保存管理员账号
        verify(userService, never()).save(any(User.class));
    }

    // 场景：测试完整引导配置创建初始管理员
    @Test
    void configuredBootstrapCreatesInitialAdminWithEncodedPassword() {
        // 1. 准备有效配置和密码服务返回值
        UserService userService = mock(UserService.class);
        PasswordService passwordService = mock(PasswordService.class);
        when(passwordService.isPasswordValid("Temporary-Admin-9!")).thenReturn(true);
        when(passwordService.encodePassword("Temporary-Admin-9!")).thenReturn("bcrypt-hash");
        when(userService.save(any(User.class))).thenReturn(true);
        AdminBootstrap bootstrap = bootstrap(
                userService,
                passwordService,
                " admin001 ",
                " 本地管理员 ",
                "Temporary-Admin-9!"
        );

        // 2. 执行管理员账号引导
        bootstrap.run();

        // 3. 断言保存规范化账号和 BCrypt 密码摘要
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        User administrator = captor.getValue();
        assertEquals("admin001", administrator.getUserAccount());
        assertEquals("本地管理员", administrator.getUserName());
        assertEquals("bcrypt-hash", administrator.getUserPassword());
        assertEquals(UserRoleEnum.ADMIN.getCode(), administrator.getUserRole());
        assertNull(administrator.getStatus());
    }

    // 场景：测试已经存在管理员时不重复创建账号
    @Test
    void configuredBootstrapDoesNothingWhenAnyAdminAlreadyExists() {
        // 1. 准备已经存在管理员的查询结果
        UserService userService = mock(UserService.class);
        PasswordService passwordService = mock(PasswordService.class);
        when(passwordService.isPasswordValid("Temporary-Admin-9!")).thenReturn(true);
        when(userService.count(any())).thenReturn(1L);
        AdminBootstrap bootstrap = bootstrap(
                userService,
                passwordService,
                "admin002",
                "另一个管理员",
                "Temporary-Admin-9!"
        );

        // 2. 执行管理员账号引导
        bootstrap.run();

        // 3. 断言未重复保存管理员账号
        verify(userService, never()).save(any(User.class));
    }

    /**
     * 使用反射注入依赖和配置并构建管理员引导组件
     *
     * @param userService     用户服务
     * @param passwordService 密码服务
     * @param account         管理员账号配置
     * @param name            管理员姓名配置
     * @param password        管理员密码配置
     * @return 管理员引导组件
     */
    private static AdminBootstrap bootstrap(
            UserService userService,
            PasswordService passwordService,
            String account,
            String name,
            String password
    ) {
        AdminBootstrap bootstrap = new AdminBootstrap();
        ReflectionTestUtils.setField(bootstrap, "userService", userService);
        ReflectionTestUtils.setField(bootstrap, "passwordService", passwordService);
        ReflectionTestUtils.setField(bootstrap, "account", account);
        ReflectionTestUtils.setField(bootstrap, "name", name);
        ReflectionTestUtils.setField(bootstrap, "password", password);
        return bootstrap;
    }

}
