package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.TopicConstant;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.model.entity.Dept;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.policy.SetDeptConfigRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptConfigVO;
import cn.com.edtechhub.worktopicselection.model.vo.TheSystemInfoVO;
import cn.com.edtechhub.worktopicselection.model.vo.TopicLockVO;
import cn.com.edtechhub.worktopicselection.service.DeptService;
import cn.com.edtechhub.worktopicselection.service.SelectionPolicyService;
import cn.com.edtechhub.worktopicselection.service.SwitchService;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 SwitchService、Redis 键值存储与 JVM/OS MXBean 实现选题策略与系统监控服务
 *
 * @author wobushi041
 */
@Service
public class SelectionPolicyServiceImpl implements SelectionPolicyService {

    /**
     * 注入开关服务依赖
     */
    private final SwitchService switchService;

    /**
     * 注入 Redis 管理依赖
     */
    private final RedisManager redisManager;

    /**
     * 注入系部服务依赖
     */
    private final DeptService deptService;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 初始化系统选题开关与策略服务实现
     *
     * @param switchService 开关服务
     * @param redisManager  Redis 管理组件
     * @param deptService   系部服务
     * @param userService   用户服务
     * @param topicService  课题服务
     */
    public SelectionPolicyServiceImpl(SwitchService switchService, RedisManager redisManager,
                                      DeptService deptService, UserService userService,
                                      TopicService topicService) {
        this.switchService = switchService;
        this.redisManager = redisManager;
        this.deptService = deptService;
        this.userService = userService;
        this.topicService = topicService;
    }

    /// 系统开关配置 ///

    /**
     * 从 SwitchService 读取 TopicConstant.CROSS_TOPIC_SWITCH 开关状态
     *
     * @return 是否开启跨系选题
     */
    @Override
    public Boolean getCrossTopicStatus() {
        return switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH);
    }

    /**
     * 更新 TopicConstant.CROSS_TOPIC_SWITCH 开关状态并返回提示文案
     *
     * @param enabled 是否开启跨系选题
     * @return 操作结果提示信息
     */
    @Override
    public String setCrossTopicStatus(boolean enabled) {
        switchService.setEnabled(TopicConstant.CROSS_TOPIC_SWITCH, enabled);
        return "跨系选题功能已" + (enabled ? "开启" : "关闭");
    }

    /**
     * 从 SwitchService 读取 TopicConstant.VIEW_TOPIC_SWITCH 开关状态
     *
     * @return 是否允许学生查看选题
     */
    @Override
    public Boolean getViewTopicStatus() {
        return switchService.isEnabled(TopicConstant.VIEW_TOPIC_SWITCH);
    }

    /**
     * 更新 TopicConstant.VIEW_TOPIC_SWITCH 开关状态并返回提示文案
     *
     * @param enabled 是否允许学生查看选题
     * @return 操作结果提示信息
     */
    @Override
    public String setViewTopicStatus(boolean enabled) {
        switchService.setEnabled(TopicConstant.VIEW_TOPIC_SWITCH, enabled);
        return (enabled ? "允许" : "禁止") + "学生查看选题";
    }

    /**
     * 从 SwitchService 读取 TopicConstant.SWITCH_SINGLE_CHOICE 开关状态
     *
     * @return 当前单选模式开关状态
     */
    @Override
    public Boolean getSwitchSingleChoiceStatus() {
        return switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE);
    }

    /**
     * 更新 TopicConstant.SWITCH_SINGLE_CHOICE 开关状态并返回模式切换提示文案
     *
     * @param enabled 是否切换为学生单选模式
     * @return 操作结果提示信息
     */
    @Override
    public String setSwitchSingleChoiceStatus(boolean enabled) {
        switchService.setEnabled(TopicConstant.SWITCH_SINGLE_CHOICE, enabled);
        return "当前单选模式切换为" + (enabled ? "学生单选模式" : "教师单选模式");
    }

    /**
     * 读取退选锁定开关与 Redis 中保存的 TOPIC_LOCK_TIME 时间戳并组装 TopicLockVO
     *
     * @return 退选加锁状态及锁定时间视图对象
     */
    @Override
    public TopicLockVO getTopicLock() {
        TopicLockVO topicLockVO = new TopicLockVO();
        topicLockVO.setIslock(switchService.isEnabled(TopicConstant.TOPIC_LOCK));
        topicLockVO.setLockTime(redisManager.getValue(TopicConstant.TOPIC_LOCK_TIME));
        return topicLockVO;
    }

    /**
     * 校验加锁时间戳合法性并同步更新 Redis 锁时间键与 SwitchService 退选锁定开关
     *
     * @param enabled   是否开启退选加锁
     * @param timestamp 加锁截止时间戳（秒）字符串
     * @return 操作结果提示信息
     */
    @Override
    public String setTopicLock(boolean enabled, String timestamp) {
        if (enabled) {
            ThrowUtils.throwIf(StringUtils.isBlank(timestamp), CodeBindMessageEnums.PARAMS_ERROR, "请设置需要加锁的时间");
            long lockTimestamp;
            try {
                lockTimestamp = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "加锁时间格式不正确");
            }
            ThrowUtils.throwIf(lockTimestamp <= System.currentTimeMillis() / 1000, CodeBindMessageEnums.PARAMS_ERROR, "加锁时间必须晚于当前时间");
            redisManager.setValue(TopicConstant.TOPIC_LOCK_TIME, timestamp);
            switchService.setEnabled(TopicConstant.TOPIC_LOCK, true);
        } else {
            redisManager.deleteKey(TopicConstant.TOPIC_LOCK_TIME);
            switchService.setEnabled(TopicConstant.TOPIC_LOCK, false);
        }
        return "当前是否退选加锁为" + (enabled ? "禁止退选题目" : "允许退选题目");
    }

    /// 系部跨选规则配置 ///

    /**
     * 在跨系开关启用时扫描 Redis 中 DEPT_CROSS_TOPIC_CONFIG:* 键并反序列化为 DeptConfigVO
     *
     * @return 系部跨选配置视图对象
     */
    @Override
    public DeptConfigVO getDeptConfig() {
        DeptConfigVO deptConfigVO = new DeptConfigVO();
        if (switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)) {
            Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
            if (keys != null && !keys.isEmpty()) {
                Map<String, List<String>> enableSelectDeptsList = new HashMap<>();

                for (String key : keys) {
                    String value = redisManager.getValue(key);
                    if (value != null) {
                        // key 格式为 DEPT_CROSS_TOPIC_CONFIG:<deptId>
                        String objectDeptName = key.split(":")[1];

                        // value 存储的是 JSON 字符串，需要反序列化
                        List<String> enableSelectDepts = JSONUtil.toList(value, String.class);

                        enableSelectDeptsList.put(objectDeptName, enableSelectDepts);
                    }
                }

                deptConfigVO.setEnableSelectDeptsList(enableSelectDeptsList);
            }
        }
        return deptConfigVO;
    }

    /**
     * 校验跨系开关与源/目标系部存在性后全量替换 Redis 中的 DEPT_CROSS_TOPIC_CONFIG:* 映射
     *
     * @param request 设置系部跨选配置请求
     * @return 是否设置成功
     */
    @Override
    public Boolean setDeptConfig(SetDeptConfigRequest request) {
        // 检查参数
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        // 取出没有开启跨系开关的情况
        ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先开启跨系开关后再配置选题规则");

        Map<String, List<String>> enableSelectDeptsList = request.getEnableSelectDeptsList();
        ThrowUtils.throwIf(enableSelectDeptsList == null || enableSelectDeptsList.isEmpty(), CodeBindMessageEnums.PARAMS_ERROR, "请至少选择一个系部后再配置");
        boolean hasRule = false;
        for (Map.Entry<String, List<String>> entry : enableSelectDeptsList.entrySet()) {
            ThrowUtils.throwIf(StringUtils.isBlank(entry.getKey()), CodeBindMessageEnums.PARAMS_ERROR, "配置中的系部名称不能为空");
            ThrowUtils.throwIf(entry.getValue() == null, CodeBindMessageEnums.PARAMS_ERROR, "系部可选范围不能为空");
            ThrowUtils.throwIf(deptService.getOne(new QueryWrapper<Dept>().eq("deptName", entry.getKey())) == null, CodeBindMessageEnums.PARAMS_ERROR, "配置中包含不存在的系部");
            for (String targetDept : entry.getValue()) {
                ThrowUtils.throwIf(StringUtils.isBlank(targetDept), CodeBindMessageEnums.PARAMS_ERROR, "可选系部名称不能为空");
                ThrowUtils.throwIf(deptService.getOne(new QueryWrapper<Dept>().eq("deptName", targetDept)) == null, CodeBindMessageEnums.PARAMS_ERROR, "配置中包含不存在的可选系部");
            }
            hasRule = hasRule || !entry.getValue().isEmpty();
        }
        ThrowUtils.throwIf(!hasRule, CodeBindMessageEnums.PARAMS_ERROR, "请至少选择一个系部后再配置");

        // 全部校验通过后再替换全量配置
        Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
        redisManager.deleteKeys(keys);

        // 配置规则
        for (Map.Entry<String, List<String>> entry : enableSelectDeptsList.entrySet()) {
            String objectDeptName = entry.getKey();
            List<String> enableSelectDepts = entry.getValue();
            if (!enableSelectDepts.isEmpty()) {
                redisManager.setValue(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":" + objectDeptName, JSONUtil.toJsonStr(enableSelectDepts));
            }
        }
        return true;
    }

    /**
     * 校验跨系开关开启后删除 Redis 中全部 DEPT_CROSS_TOPIC_CONFIG:* 规则键
     *
     * @return 是否清除成功
     */
    @Override
    public Boolean delDeptConfig() {
        // 必须开启跨选开关
        ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先开启跨系开关后再清除跨选规则");

        // 清理所有的跨选规则
        Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
        redisManager.deleteKeys(keys);
        return true;
    }

    /// 系统监控面板 ///

    /**
     * 汇总用户与选题各状态数量并通过 OperatingSystemMXBean 与 MemoryMXBean 采集主机资源指标
     *
     * @return 系统统计与资源监控信息视图对象
     */
    @Override
    public TheSystemInfoVO getSystemInfo() {
        // 创建存储系统信息的 VO 对象
        TheSystemInfoVO theSystemInfoVo = new TheSystemInfoVO();

        // 选题信息
        theSystemInfoVo.setTotalDeptCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 2)
                .count()
        );

        theSystemInfoVo.setTotalTeacherCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 1)
                .count()
        );

        theSystemInfoVo.setTotalStudentCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 0)
                .count()
        );

        theSystemInfoVo.setLoginUserCount(userService
                .lambdaQuery()
                .eq(User::getStatus, "老用户")
                .count()
        );

        theSystemInfoVo.setAuditPassTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, 0)
                .count()
        );

        theSystemInfoVo.setAuditBackTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, -2)
                .count()
        );

        theSystemInfoVo.setAuditTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, -1)
                .count()
        );

        theSystemInfoVo.setReleaseTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, 1)
                .count()
        );

        // 系统信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;

        double cpuLoad = sunOsBean.getSystemCpuLoad() * 100;
        theSystemInfoVo.setCpuUsage(new DecimalFormat("0.00").format(cpuLoad) + "%");

        long totalPhysical = sunOsBean.getTotalPhysicalMemorySize();
        long freePhysical = sunOsBean.getFreePhysicalMemorySize();
        long usedPhysical = totalPhysical - freePhysical;
        theSystemInfoVo.setMemoryUsage(formatSize(usedPhysical) + "/" + formatSize(totalPhysical));

        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;
        theSystemInfoVo.setDiskUsage(formatSize(usedDisk) + "/" + formatSize(totalDisk));

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedHeap = heapUsage.getUsed();
        long maxHeap = heapUsage.getMax();
        theSystemInfoVo.setJvmMemoryUsage(formatSize(usedHeap) + "/" + formatSize(maxHeap));

        return theSystemInfoVo;
    }

    /**
     * 格式化字节容量单位
     *
     * @param size 字节大小
     * @return 格式化后的带单位容量字符串
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        int exp = (int) (Math.log(size) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", size / Math.pow(1024, exp), unit);
    }

}
