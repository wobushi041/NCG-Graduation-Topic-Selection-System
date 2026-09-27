package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.user.DeleteRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.TeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.TeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 用户管理与查询应用服务接口
 *
 * @author wobushi041
 */
public interface UserApplicationService {

    /**
     * 创建新用户并返回包含一次性临时密码的响应结果
     *
     * @param request 创建用户请求
     * @return 包含新用户 id 与一次性临时密码提示信息的统一响应
     */
    BaseResponse<Long> addUser(UserAddRequest request);

    /**
     * 删除指定账号的用户及其关联课题或选题占用，并强制踢出登录态
     *
     * @param request 删除用户请求
     * @return 是否删除成功
     */
    Boolean deleteUser(DeleteRequest request);

    /**
     * 更新指定用户资料与角色，若角色发生变更则强制踢出登录态
     *
     * @param request 更新用户请求
     * @return 是否更新成功
     */
    Boolean updateUser(UserUpdateRequest request);

    /**
     * 获取当前登录用户的脱敏视图信息
     *
     * @return 当前登录用户脱敏视图对象
     */
    LoginUserVO getLoginUser();

    /**
     * 按角色范围分页查询用户列表（管理员可查全部，教师仅限查看本系学生）
     *
     * @param request 用户分页查询请求
     * @return 用户分页数据
     */
    Page<User> listUserByPage(UserQueryRequest request);

    /**
     * 查询指定角色的教师或系部主任脱敏下拉列表（教师查询主任时仅限本系部）
     *
     * @param request 教师查询请求
     * @return 教师脱敏下拉列表数据
     */
    List<TeacherVO> getTeacher(TeacherQueryRequest request);

    /**
     * 根据用户主键 id 查询用户实体
     *
     * @param id 用户 id
     * @return 用户实体数据
     */
    User getUserById(long id);

    /**
     * 根据用户主键 id 查询脱敏后的用户视图对象
     *
     * @param id 用户 id
     * @return 用户脱敏视图对象
     */
    UserVO getUserVOById(long id);

}
