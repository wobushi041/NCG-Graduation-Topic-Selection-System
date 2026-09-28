package cn.edu.nfu.topicselection.integration.support;

import cn.edu.nfu.topicselection.mapper.DeptMapper;
import cn.edu.nfu.topicselection.mapper.ProjectMapper;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.Dept;
import cn.edu.nfu.topicselection.model.entity.Project;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.annotation.Resource;

/**
 * 集成测试领域数据构造工厂
 *
 * @author wobushi041
 */
@Component
public class TestFixtureFactory {

    /**
     * BCrypt 密码编码器
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注入用户 Mapper 依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 注入系部 Mapper 依赖
     */
    @Resource
    private DeptMapper deptMapper;

    /**
     * 注入专业 Mapper 依赖
     */
    @Resource
    private ProjectMapper projectMapper;

    /**
     * 注入课题 Mapper 依赖
     */
    @Resource
    private TopicMapper topicMapper;

    /**
     * 注入学生选题关联 Mapper 依赖
     */
    @Resource
    private StudentTopicSelectionMapper studentTopicSelectionMapper;

    /**
     * 创建并持久化测试系部与专业记录
     *
     * @param deptName    系部名称
     * @param projectName 专业名称
     * @param groupName   所属选题组名称
     */
    public void createDeptAndProject(String deptName, String projectName, String groupName) {
        Dept dept = new Dept();
        dept.setDeptName(deptName);
        deptMapper.insert(dept);

        Project project = new Project();
        project.setDeptName(deptName);
        project.setProjectName(projectName);
        project.setGroupName(groupName);
        projectMapper.insert(project);
    }

    /**
     * 创建并持久化指定角色的测试用户
     *
     * @param account     登录账号
     * @param rawPassword 原始明文密码
     * @param userName    用户姓名
     * @param roleCode    角色编码
     * @param dept        所属系部
     * @param project     所属专业
     * @param topicAmount 课题额度或已选数量
     * @return 持久化后的用户实体
     */
    public User createUser(String account, String rawPassword, String userName, int roleCode,
                           String dept, String project, int topicAmount) {
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(passwordEncoder.encode(rawPassword));
        user.setUserName(userName);
        user.setUserRole(roleCode);
        user.setDept(dept);
        user.setProject(project);
        user.setStatus("0");
        user.setTopicAmount(topicAmount);
        user.setEmail(account + "@nfu.edu.cn");
        userMapper.insert(user);
        return user;
    }

    /**
     * 创建并持久化已发布且处于开放时间窗口内的课题
     *
     * @param title           课题标题
     * @param teacherAccount  指导教师账号
     * @param teacherName     指导教师姓名
     * @param deptName        系部名称
     * @param topicGroup      选题组名称
     * @param surplusQuantity 剩余可选容量
     * @return 持久化后的课题实体
     */
    public Topic createPublishedTopic(String title, String teacherAccount, String teacherName,
                                      String deptName, String topicGroup, int surplusQuantity) {
        long now = System.currentTimeMillis();
        Topic topic = new Topic();
        topic.setTopic(title);
        topic.setType("工程设计");
        topic.setDescription("课题详细描述内容，用于集成测试验证完整链路");
        topic.setRequirement("熟悉 Spring Boot 与 React 开发");
        topic.setTeacherAccount(teacherAccount);
        topic.setTeacherName(teacherName);
        topic.setDeptName(deptName);
        topic.setDeptTeacher("系主任");
        topic.setTopicGroup(topicGroup);
        topic.setSurplusQuantity(surplusQuantity);
        topic.setSelectAmount(0);
        topic.setStatus(1);
        topic.setStartTime(new Date(now - 3600_000L));
        topic.setEndTime(new Date(now + 86400_000L));
        topicMapper.insert(topic);
        return topic;
    }

}
