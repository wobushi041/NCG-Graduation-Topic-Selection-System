package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.request.organization.DeleteCollegeRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteMajorRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupDeleteRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.vo.CollegeVO;
import cn.edu.nfu.topicselection.model.vo.MajorVO;
import cn.edu.nfu.topicselection.model.vo.TopicGroupVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 定义学院维护、专业维护、专业选题组绑定以及教师选题组查询的应用服务契约
 *
 * @author wobushi041
 */
public interface OrganizationApplicationService {

    /**
     * 添加新的学院
     *
     * @param request 添加学院请求
     * @return 新添加的学院 id
     */
    Long addCollege(CollegeAddRequest request);

    /**
     * 添加新的专业并可选绑定所属选题组
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    Long addMajor(MajorAddRequest request);

    /**
     * 配置专业所属选题组
     *
     * @param request 更新专业选题组请求
     * @return 是否更新成功
     */
    Boolean updateMajorGroup(MajorGroupUpdateRequest request);

    /**
     * 创建学院下的选题组
     *
     * @param request 选题组创建请求
     * @return 新增选题组 id
     */
    Long addTopicGroup(TopicGroupAddRequest request);

    /**
     * 更新选题组名称和所属学院
     *
     * @param request 选题组更新请求
     * @return 是否更新成功
     */
    Boolean updateTopicGroup(TopicGroupUpdateRequest request);

    /**
     * 删除未被引用的选题组
     *
     * @param request 选题组删除请求
     * @return 是否删除成功
     */
    Boolean deleteTopicGroup(TopicGroupDeleteRequest request);

    /**
     * 校验无下属专业、用户与课题后删除指定学院
     *
     * @param request 删除学院请求
     * @return 是否删除成功
     */
    Boolean deleteCollege(DeleteCollegeRequest request);

    /**
     * 校验无下属用户后删除指定专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    Boolean deleteMajor(DeleteMajorRequest request);

    /**
     * 分页查询学院数据
     *
     * @param request 学院查询请求
     * @return 学院分页数据
     */
    Page<College> getCollegePage(CollegeQueryRequest request);

    /**
     * 查询当前登录用户可见的学院下拉列表数据
     *
     * @param request 学院查询请求
     * @return 学院下拉列表数据
     */
    List<CollegeVO> getCollegeList(CollegeQueryRequest request);

    /**
     * 分页查询专业数据
     *
     * @param request 专业查询请求
     * @return 专业分页数据
     */
    Page<Major> getMajorPage(MajorQueryRequest request);

    /**
     * 查询专业下拉列表数据
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    List<MajorVO> getMajorList(MajorQueryRequest request);

    /**
     * 分页查询选题组
     *
     * @param request 选题组查询请求
     * @return 选题组分页数据
     */
    Page<TopicGroup> getTopicGroupPage(TopicGroupQueryRequest request);

    /**
     * 查询学院下可用的选题组选项
     *
     * @param request 选题组查询请求
     * @return 选题组选项
     */
    List<TopicGroupVO> getTopicGroupList(TopicGroupQueryRequest request);

    /**
     * 获取当前登录教师的选题组及额度列表
     *
     * @return 当前教师的选题组信息列表
     */
    List<Map<String, Object>> getTeacherGroups();

    /**
     * 按角色范围过滤并批量查询指定教师的选题组及额度列表
     *
     * @param request 教师选题组额度批量查询请求
     * @return 教师账号到选题组额度列表的映射
     */
    Map<String, List<Map<String, Object>>> getTeacherGroupsBatch(TeacherGroupsBatchRequest request);

    /**
     * 更新指定教师在选题组中的最大出题数量
     *
     * @param request 教师选题组额度更新请求
     * @return 是否更新成功
     */
    Boolean updateTeacherGroupQuota(TeacherGroupQuotaUpdateRequest request);

    /**
     * 查询系统内现有的全部选题组名称列表
     *
     * @return 系统内现有选题组名称列表
     */
    List<String> getGroupList();

}
