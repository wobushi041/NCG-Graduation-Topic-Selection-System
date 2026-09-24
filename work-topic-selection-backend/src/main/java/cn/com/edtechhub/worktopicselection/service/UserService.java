package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.dto.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户业务服务接口
 *
 * @author wobushi041
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户账号查询用户信息
     *
     * @param userAccount 用户账号
     * @return 用户实体信息
     */
    User userIsExist(String userAccount);

    /**
     * 根据用户账号和用户姓名查询用户信息
     *
     * @param userAccount 用户账号
     * @param userName    用户姓名
     * @return 用户实体信息
     */
    User userIsExist(String userAccount, String userName);

    /**
     * 对新设置的明文密码进行安全单向散列编码
     *
     * @param rawPassword 明文密码
     * @return 编码后的密码密文
     */
    String encodePassword(String rawPassword);

    /**
     * 在历史密码升级场景下对明文密码进行兼容散列编码
     *
     * @param rawPassword 明文密码
     * @return 编码后的密码密文
     */
    String encodePasswordForMigration(String rawPassword);

    /**
     * 校验输入的明文密码与已存储的散列密码是否匹配
     *
     * @param rawPassword     输入的明文密码
     * @param encodedPassword 已存储的散列密码
     * @return 密码是否匹配
     */
    boolean matchesPassword(String rawPassword, String encodedPassword);

    /**
     * 判断已存储的密码散列是否需要升级为新版加密格式
     *
     * @param encodedPassword 已存储的散列密码
     * @return 是否需要升级加密格式
     */
    boolean needsPasswordUpgrade(String encodedPassword);

    /**
     * 生成只展示一次的随机临时密码
     *
     * @return 随机临时密码字符串
     */
    String generateTemporaryPassword();

    /**
     * 校验明文密码长度是否符合安全策略要求
     *
     * @param rawPassword 明文密码
     * @return 密码长度是否合法
     */
    boolean isPasswordValid(String rawPassword);

    /**
     * 获取当前已登录用户的唯一标识
     *
     * @return 当前登录用户 ID
     */
    Long userGetCurrentLonginUserId();

    /**
     * 获取当前已登录用户的完整实体信息
     *
     * @return 当前登录用户实体
     */
    User userGetCurrentLoginUser();

    /**
     * 判断指定用户是否具有管理员角色
     *
     * @param user 待校验的用户实体
     * @return 是否为管理员
     */
    Boolean userIsAdmin(User user);

    /**
     * 判断指定用户是否具有系部主任角色
     *
     * @param user 待校验的用户实体
     * @return 是否为系部主任
     */
    Boolean userIsDept(User user);

    /**
     * 判断指定用户是否具有教师角色
     *
     * @param user 待校验的用户实体
     * @return 是否为教师
     */
    Boolean userIsTeacher(User user);

    /**
     * 判断指定用户是否具有学生角色
     *
     * @param user 待校验的用户实体
     * @return 是否为学生
     */
    Boolean userIsStudent(User user);

    /**
     * 根据用户查询请求构建查询条件封装
     *
     * @param userQueryRequest 用户查询请求参数
     * @return 用户查询条件包装器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /// 用户视图对象转换接口（TODO: 下面是旧代码可以被迁移到 UserVO 中） ///

    /**
     * 将用户实体脱敏转换为当前登录用户视图对象
     *
     * @param user 用户实体
     * @return 脱敏后的当前登录用户视图对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 将用户实体脱敏转换为对外用户视图对象
     *
     * @param user 用户实体
     * @return 脱敏后的用户视图对象
     */
    UserVO getUserVO(User user);

    /**
     * 将用户实体列表批量脱敏转换为对外用户视图对象列表
     *
     * @param userList 用户实体列表
     * @return 脱敏后的用户视图对象列表
     */
    List<UserVO> getUserVO(List<User> userList);

}
