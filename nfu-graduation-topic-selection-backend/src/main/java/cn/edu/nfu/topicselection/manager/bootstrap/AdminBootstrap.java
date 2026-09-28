package cn.edu.nfu.topicselection.manager.bootstrap;

import cn.edu.nfu.topicselection.constant.UserConstant;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 管理员账号一次性初始化引导执行器
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入密码服务依赖
     */
    @Resource
    private PasswordService passwordService;

    /**
     * 引导管理员登录账号配置
     */
    @Value("${app.bootstrap-admin.account:}")
    private String account;

    /**
     * 引导管理员真实姓名配置
     */
    @Value("${app.bootstrap-admin.name:}")
    private String name;

    /**
     * 引导管理员初始登录密码配置
     */
    @Value("${app.bootstrap-admin.password:}")
    private String password;

    /**
     * 应用启动时校验环境变量配置并按需初始化系统首个管理员账号
     *
     * @param args 命令行启动参数数组
     */
    @Override
    public void run(String... args) {
        if (StringUtils.isAllBlank(account, name, password)) {
            return;
        }
        if (StringUtils.isAnyBlank(account, name, password)) {
            throw new IllegalStateException("管理员引导账号、姓名和密码必须同时配置");
        }
        if (!passwordService.isPasswordValid(password)) {
            throw new IllegalStateException("管理员引导密码必须为 8 到 72 个 UTF-8 字节");
        }

        long administratorCount = userService.count(
                new QueryWrapper<User>().eq("userRole", UserRoleEnum.ADMIN.getCode())
        );
        if (administratorCount > 0) {
            log.info("系统已有管理员，跳过管理员引导");
            return;
        }

        String normalizedAccount = account.trim();
        if (normalizedAccount.length() > UserConstant.MAX_USER_ACCOUNT_LENGTH) {
            throw new IllegalStateException("管理员引导账号不能超过 128 个字符");
        }
        if (userService.userIsExist(normalizedAccount) != null) {
            throw new IllegalStateException("管理员引导账号已被非管理员用户占用");
        }

        User administrator = new User();
        administrator.setUserAccount(normalizedAccount);
        administrator.setUserName(name.trim());
        administrator.setUserPassword(passwordService.encodePassword(password));
        administrator.setUserRole(UserRoleEnum.ADMIN.getCode());
        administrator.setStatus(null);

        if (!userService.save(administrator)) {
            throw new IllegalStateException("创建管理员引导账号失败");
        }
        log.info("管理员引导账号已创建，首次登录前需要修改临时密码");
    }

}
