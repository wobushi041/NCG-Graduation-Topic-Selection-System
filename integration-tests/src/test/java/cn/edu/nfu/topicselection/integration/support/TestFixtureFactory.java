package cn.edu.nfu.topicselection.integration.support;

import cn.edu.nfu.topicselection.mapper.CollegeMapper;
import cn.edu.nfu.topicselection.mapper.MajorMapper;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.TopicGroupMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
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
     * 注入学院 Mapper 依赖
     */
    @Resource
    private CollegeMapper collegeMapper;

    /**
     * 注入选题组 Mapper 依赖
     */
    @Resource
    private TopicGroupMapper topicGroupMapper;

    /**
     * 注入专业 Mapper 依赖
     */
    @Resource
    private MajorMapper majorMapper;

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
     * 注入 JDBC 操作依赖
     */
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建并持久化测试学院、选题组与专业记录
     *
     * @param collegeName 学院名称
     * @param majorName   专业名称
     * @param groupName   所属选题组名称
     */
    public Long createCollegeAndMajor(String collegeName, String majorName, String groupName) {
        College college = new College();
        college.setCollegeName(collegeName);
        collegeMapper.insert(college);

        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setCollegeId(college.getId());
        topicGroup.setGroupName(groupName);
        topicGroupMapper.insert(topicGroup);

        Major major = new Major();
        major.setMajorName(majorName);
        major.setCollegeId(college.getId());
        major.setTopicGroupId(topicGroup.getId());
        majorMapper.insert(major);
        return topicGroup.getId();
    }

    /**
     * 创建并持久化指定角色的测试用户
     *
     * @param account     登录账号
     * @param rawPassword 原始明文密码
     * @param userName    用户姓名
     * @param roleCode    角色编码
     * @param college     所属学院名称
     * @param major       所属专业名称
     * @param topicAmount 课题额度或已选数量
     * @return 持久化后的用户实体
     */
    public User createUser(String account, String rawPassword, String userName, int roleCode,
                           String college, String major, int topicAmount) {
        User user = new User();
        user.setUserAccount(account);
        user.setUserPassword(passwordEncoder.encode(rawPassword));
        user.setUserName(userName);
        user.setUserRole(roleCode);
        College savedCollege = getCollegeByName(college);
        Major savedMajor = getMajorByName(savedCollege.getId(), major);
        user.setCollegeId(savedCollege.getId());
        user.setMajorId(savedMajor.getId());
        if (roleCode == 2) {
            user.setTopicGroupId(savedMajor.getTopicGroupId());
        }
        user.setStatus("0");
        user.setTopicAmount(topicAmount);
        user.setEmail(account + "@nfu.edu.cn");
        userMapper.insert(user);
        if (roleCode == 1) {
            jdbcTemplate.update(
                    "INSERT INTO teacher_group_quota (teacherAccount, topicGroupId, maxTopics) VALUES (?, ?, ?)",
                    account,
                    savedMajor.getTopicGroupId(),
                    topicAmount
            );
        }
        return user;
    }

    /**
     * 创建并持久化已发布且处于开放时间窗口内的课题
     *
     * @param title           课题标题
     * @param teacherAccount  指导教师账号
     * @param teacherName     指导教师姓名
     * @param collegeName     学院名称
     * @param topicGroup      选题组名称
     * @param surplusQuantity 剩余可选容量
     * @return 持久化后的课题实体
     */
    public Topic createPublishedTopic(String title, String teacherAccount, String teacherName,
                                      String collegeName, String topicGroup, int surplusQuantity) {
        long now = System.currentTimeMillis();
        Topic topic = new Topic();
        topic.setTopic(title);
        topic.setType("工程设计");
        topic.setDescription("课题详细描述内容，用于集成测试验证完整链路");
        topic.setRequirement("熟悉 Spring Boot 与 React 开发");
        topic.setTeacherAccount(teacherAccount);
        topic.setTeacherName(teacherName);
        topic.setTopicGroupId(getTopicGroupByName(getCollegeByName(collegeName).getId(), topicGroup).getId());
        topic.setSurplusQuantity(surplusQuantity);
        topic.setSelectAmount(0);
        topic.setStatus(1);
        topic.setStartTime(new Date(now - 3600_000L));
        topic.setEndTime(new Date(now + 86400_000L));
        topicMapper.insert(topic);
        return topic;
    }

    /**
     * 按名称查询测试学院。
     *
     * @param collegeName 学院名称
     * @return 学院实体
     */
    private College getCollegeByName(String collegeName) {
        return collegeMapper.selectOne(
                new LambdaQueryWrapper<College>().eq(College::getCollegeName, collegeName)
        );
    }

    /**
     * 按学院与名称查询测试专业。
     *
     * @param collegeId 学院 id
     * @param majorName 专业名称
     * @return 专业实体
     */
    private Major getMajorByName(Long collegeId, String majorName) {
        return majorMapper.selectOne(
                new LambdaQueryWrapper<Major>()
                        .eq(Major::getCollegeId, collegeId)
                        .eq(Major::getMajorName, majorName)
        );
    }

    /**
     * 按学院与名称查询测试选题组。
     *
     * @param collegeId 学院 id
     * @param groupName 选题组名称
     * @return 选题组实体
     */
    private TopicGroup getTopicGroupByName(Long collegeId, String groupName) {
        return topicGroupMapper.selectOne(
                new LambdaQueryWrapper<TopicGroup>()
                        .eq(TopicGroup::getCollegeId, collegeId)
                        .eq(TopicGroup::getGroupName, groupName)
        );
    }

}
