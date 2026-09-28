package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.request.selection.GetSelectTopicByIdRequest;
import cn.edu.nfu.topicselection.model.request.selection.GetSelectTopicRequest;
import cn.edu.nfu.topicselection.model.request.selection.GetStudentByTopicIdRequest;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TopicSelectionQueryService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于选题关联表、课题表与用户表实现学生选题与教师确认只读查询服务
 *
 * @author wobushi041
 */
@Slf4j
@Service
public class TopicSelectionQueryServiceImpl implements TopicSelectionQueryService {

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 初始化学生选题与教师确认读用例服务实现
     *
     * @param userService                  用户服务
     * @param topicService                 课题服务
     * @param studentTopicSelectionService 学生选题关联服务
     */
    public TopicSelectionQueryServiceImpl(UserService userService, TopicService topicService,
                                          StudentTopicSelectionService studentTopicSelectionService) {
        this.userService = userService;
        this.topicService = topicService;
        this.studentTopicSelectionService = studentTopicSelectionService;
    }

    /// 选题读用例 ///

    /**
     * 校验当前教师对题目的归属权并通过学生选题关联表与用户表查询最终选中该题目的学生列表
     *
     * @param request 根据题目 id 查询已选学生请求
     * @return 已选该题目的学生列表
     */
    @Override
    public List<User> getSelectTopicById(GetSelectTopicByIdRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long id = request.getId();
        ThrowUtils.throwIf(id == null, CodeBindMessageEnums.PARAMS_ERROR, "id 不能为空");
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "id 必须是正整数");

        User loginTeacher = userService.userGetCurrentLoginUser();
        Topic topic = topicService.getById(id);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "题目不存在");
        ThrowUtils.throwIf(!isTopicOwner(loginTeacher, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能查看自己题目的学生");

        // 找到对应的学生题目关联记录
        List<StudentTopicSelection> list = studentTopicSelectionService.list(new QueryWrapper<StudentTopicSelection>()
                .eq("topicId", id)
                .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );

        // 获取用户数据
        List<User> userList = new ArrayList<>();
        for (StudentTopicSelection student : list) {
            final String userAccount = student.getUserAccount();
            final User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", userAccount));
            if (user == null) {
                continue;
            }
            userList.add(user);
        }
        return userList;
    }

    /**
     * 根据当前登录学生账号从学生选题关联表提取预选状态记录并批量查询对应题目实体
     *
     * @return 当前学生预选的题目列表
     */
    @Override
    public List<Topic> getPreTopic() {
        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();

        // 获取查询条件
        String userAccount = loginUser.getUserAccount();
        QueryWrapper<StudentTopicSelection> queryWrapper = new QueryWrapper<StudentTopicSelection>()
                .eq("userAccount", userAccount)
                .eq("status", StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode());

        // 查询对应的预先选题记录
        ThrowUtils.throwIf(userAccount == null, CodeBindMessageEnums.OPERATION_ERROR, "参数有问题");
        List<StudentTopicSelection> studentTopicSelectionList = studentTopicSelectionService.list(queryWrapper);

        ThrowUtils.throwIf(studentTopicSelectionList == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "当前没有预选的题目");
        assert studentTopicSelectionList != null;

        // 填充完整的返回体, 把关联对应的选题都拿到
        List<Long> topicIds = studentTopicSelectionList
                .stream()
                .map(StudentTopicSelection::getTopicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return topicService.listByIds(topicIds);
    }

    /**
     * 根据当前登录学生账号从学生选题关联表查询最终确认记录并加载对应题目实体
     *
     * @return 当前学生最终确认的题目列表
     */
    @Override
    public List<Topic> getSelectTopic() {
        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();
        String userAccount = loginUser.getUserAccount();

        // 查找确认最终选题的记录
        StudentTopicSelection studentTopicSelection = studentTopicSelectionService.getOne(
                new QueryWrapper<StudentTopicSelection>()
                        .eq("userAccount", userAccount)
                        .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );
        log.info("用户: {} 确认了自己是否有最终选题", userAccount);

        ThrowUtils.throwIf(studentTopicSelection == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "当前用户没有确认最终的选题");
        assert studentTopicSelection != null;

        // 封装最终的返回值
        Long topicId = studentTopicSelection.getTopicId();
        Topic topic = topicService.getById(topicId);
        List<Topic> topicList = new ArrayList<>();
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.OPERATION_ERROR, "不存在对应的题目，请联系系统管理员");
        topicList.add(topic);
        return topicList;
    }

    /**
     * 查询当前登录学生与指定题目的最终选题记录并将记录更新时间转换为秒级时间戳字符串
     *
     * @param request 查询最终选题时间请求
     * @return 最终选题确认时间戳（秒）字符串
     */
    @Override
    public String getSelectTopicTime(GetSelectTopicRequest request) {
        // 检查参数
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long topicId = request.getTopicId();
        ThrowUtils.throwIf(topicId == null, CodeBindMessageEnums.PARAMS_ERROR, "id 不能为空");
        assert topicId != null;
        ThrowUtils.throwIf(topicId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "id 必须是正整数");

        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();

        // 获取题目关联记录
        StudentTopicSelection studentTopicSelection = studentTopicSelectionService
                .getOne(new QueryWrapper<StudentTopicSelection>()
                        .eq("topicId", topicId)
                        .eq("userAccount", loginUser.getUserAccount())
                        .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
                );

        ThrowUtils.throwIf(studentTopicSelection == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "当前学生没有确认该题目");
        ThrowUtils.throwIf(studentTopicSelection.getUpdateTime() == null, CodeBindMessageEnums.OPERATION_ERROR, "选题记录缺少确认时间，请联系系统管理员");
        long topicTimestamp = studentTopicSelection.getUpdateTime().getTime() / 1000;
        log.debug("获取关联题目最终选择时间: {}", topicTimestamp);
        return String.valueOf(topicTimestamp);
    }

    /**
     * 校验当前教师对题目的归属权并通过学生选题关联表按题目 id 查询已选学生实体列表
     *
     * @param request 根据题目 id 查询学生请求
     * @return 已选择该题目的学生列表
     */
    @Override
    public List<User> getStudentByTopicId(GetStudentByTopicIdRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        Long topicId = request.getId();
        ThrowUtils.throwIf(topicId == null || topicId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "题目 id 必须是正整数");

        User loginTeacher = userService.userGetCurrentLoginUser();
        Topic topic = topicService.getById(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "题目不存在");
        ThrowUtils.throwIf(!isTopicOwner(loginTeacher, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能查看自己题目的学生");

        // 查询对应的学生关联记录并组装用户实体
        List<StudentTopicSelection> studentList = studentTopicSelectionService.list(new QueryWrapper<StudentTopicSelection>()
                .eq("topicId", topicId)
                .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );
        ArrayList<User> userList = new ArrayList<>();
        for (StudentTopicSelection student : studentList) {
            String userAccount = student.getUserAccount();
            User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", userAccount));
            if (user == null) {
                continue;
            }
            userList.add(user);
        }
        return userList;
    }

    /**
     * 判断指定用户是否为该课题的指导教师本人
     *
     * @param user  用户实体
     * @param topic 课题实体
     * @return 是否为课题归属教师
     */
    boolean isTopicOwner(User user, Topic topic) {
        if (user == null || topic == null) {
            return false;
        }
        if (StringUtils.isNotBlank(topic.getTeacherAccount())) {
            return Objects.equals(user.getUserAccount(), topic.getTeacherAccount());
        }
        return Objects.equals(user.getUserName(), topic.getTeacherName())
                && Objects.equals(StringUtils.trimToNull(user.getDept()), StringUtils.trimToNull(topic.getDeptName()));
    }

}
