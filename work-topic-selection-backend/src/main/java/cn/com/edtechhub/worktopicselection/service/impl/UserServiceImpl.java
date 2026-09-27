package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.CommonConstant;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.mapper.UserMapper;
import cn.com.edtechhub.worktopicselection.model.dto.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.utils.SqlUtils;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户业务服务实现类
 *
 * @author wobushi041
 */
@Service
@Slf4j
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 基于 MyBatis-Plus QueryWrapper 组装用户角色精确匹配、账号与姓名及系部模糊匹配以及经过 SqlUtils 校验的排序条件
     *
     * @param userQueryRequest 用户查询请求参数
     * @return 用户查询条件包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userQueryRequest.getUserAccount();
        final Integer userRole = userQueryRequest.getUserRole();
        String userName = userQueryRequest.getUserName();
        String dept = userQueryRequest.getDept();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StringUtils.isNotBlank(dept), "dept", dept);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validUserSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);

        return queryWrapper;
    }

    /**
     * 通过 Sa-Token StpUtil 从当前会话上下文中提取并转换登录用户 ID
     *
     * @return 当前登录用户 ID
     */
    @Override
    public Long userGetCurrentLonginUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    /**
     * 通过 Sa-Token 获取当前登录用户 ID 并查询 MySQL user 表获取最新用户实体
     *
     * @return 当前登录用户实体
     */
    @Override
    public User userGetCurrentLoginUser() {
        Long loginUserId = this.userGetCurrentLonginUserId();
//        return this.userGetSessionById(loginUserId); // 缓存是有些更新问题的(如果性能要求较高, 则可以考虑使用)
        return this.getById(loginUserId); // 最好是通过数据库查询实时更新, 否则某些场景是有问题的
    }

    /**
     * 基于 MyBatis-Plus LambdaQueryWrapper 按 userAccount 字段精确查询 user 表单条记录
     *
     * @param userAccount 用户账号
     * @return 用户实体信息
     */
    @Override
    public User userIsExist(String userAccount) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(User::getUserAccount, userAccount)
        ;
        return this.getOne(lambdaQueryWrapper);
    }

    /**
     * 基于 MyBatis-Plus LambdaQueryWrapper 按 userAccount 与 userName 字段联合精确查询 user 表单条记录
     *
     * @param userAccount 用户账号
     * @param userName    用户姓名
     * @return 用户实体信息
     */
    @Override
    public User userIsExist(String userAccount, String userName) {
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(User::getUserAccount, userAccount)
                .eq(User::getUserName, userName)
        ;
        return this.getOne(lambdaQueryWrapper);
    }

    /**
     * 比对 User 实体的 userRole 字段是否等于 UserRoleEnum.ADMIN 角色编码
     *
     * @param user 待校验的用户实体
     * @return 是否为管理员角色
     */
    @Override
    public Boolean userIsAdmin(User user) {
        return user != null && Objects.equals(user.getUserRole(), UserRoleEnum.ADMIN.getCode());
    }

    /**
     * 比对 User 实体的 userRole 字段是否等于 UserRoleEnum.DEPT 角色编码
     *
     * @param user 待校验的用户实体
     * @return 是否为系部主任角色
     */
    @Override
    public Boolean userIsDept(User user) {
        return user != null && Objects.equals(user.getUserRole(), UserRoleEnum.DEPT.getCode());
    }

    /**
     * 比对 User 实体的 userRole 字段是否等于 UserRoleEnum.TEACHER 角色编码
     *
     * @param user 待校验的用户实体
     * @return 是否为教师角色
     */
    @Override
    public Boolean userIsTeacher(User user) {
        return user != null && Objects.equals(user.getUserRole(), UserRoleEnum.TEACHER.getCode());
    }

    /**
     * 比对 User 实体的 userRole 字段是否等于 UserRoleEnum.STUDENT 角色编码
     *
     * @param user 待校验的用户实体
     * @return 是否为学生角色
     */
    @Override
    public Boolean userIsStudent(User user) {
        return user != null && Objects.equals(user.getUserRole(), UserRoleEnum.STUDENT.getCode());
    }

    /// 用户视图对象转换接口（TODO: 下面代码可以迁移到 UserVO 中） ///

    /**
     * 使用 Spring BeanUtils 将 User 实体属性浅拷贝至 LoginUserVO 视图对象实现敏感字段脱敏
     *
     * @param user 用户实体
     * @return 脱敏后的当前登录用户视图对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 使用 Spring BeanUtils 将 User 实体属性浅拷贝至 UserVO 视图对象实现敏感字段脱敏
     *
     * @param user 用户实体
     * @return 脱敏后的用户视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 基于 Stream 流遍历 User 实体列表并逐一调用单对象转换方法构建 UserVO 列表
     *
     * @param userList 用户实体列表
     * @return 脱敏后的用户视图对象列表
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

}
