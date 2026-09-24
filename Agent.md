# 项目编码风格与注释规范指南 (@author wobushi041)

本文件为项目 AI 编程与代码审查的强制规范准则（`Agent.md`）。在生成、修改或重构任何代码时，必须严格遵守以下注释格式与代码编写规范。

---

## 一、 注释格式与作用场景规范

### 1. 速查总表

| 编号 | 注释类型 | 标准格式模版 | 唯一作用场景 |
| :--- | :--- | :--- | :--- |
| **01** | **标准类/接口头注释** | `/**` + `中文职责` + `[TODO]` + 空行 + `@author wobushi041` + `*/` | 所有 `Controller`、`Service`、`ServiceImpl`、`Manager`、`Config`、`Utils`、`Request`、`VO`、`Enum`、`Constant` 类或接口顶部 |
| **02** | **标准方法文档注释** | `/**` + `方法描述` + 空行 + `@param（如有）` + `@return（如有）` + `*/` | 所有 `Controller`、`Service` 接口、**`ServiceImpl` 实现类（含 `@Override` 方法）**、`Manager`、`Utils` 及自定义构造器 |
| **03** | **字段/枚举项文档注释** | `/**` <br> ` * 字段或枚举含义` <br> ` */` | ① `Entity`、`Request` (DTO)、`VO`、`Enum` 的所有成员变量与枚举实例<br>② `@Resource` 依赖注入字段（`注入 xxx 依赖`） |
| **04** | **三斜杠区块分隔注释** | `/// 分区名称 ///` | ① 所有 `Request` / `VO` 底部 `serialVersionUID` 上方的 `/// 序列化字段 ///`<br>② 大体积类内部划分功能模块（如 `/// 用户相关接口 ///`、`/// 系统信息 ///`） |
| **05** | **方法内阶段单行注释** | `// 阶段动作或业务规则说明` | 生产代码方法体内部切分逻辑段落（如 `// 流量控制`、`// 参数检查`），以及枚举类内按状态码段分组 |
| **06** | **分支前置单行注释** | `}` <br> `// 分支条件说明` <br> `else if (...) {` | 多分支 `if - else if - else` 结构中，写在上一分支 `}` 下方、`else if` / `else` 上方，解释分支业务条件 |
| **07** | **单元测试编号注释** | 方法外：`// 场景：测试 xxx`<br>方法内：`// 1. ...` `// 2. ...` `// 3. ...` | `*Test.java` 单元测试类中，标记测试场景与三段式步骤（`1. 准备测试数据` -> `2. 调用方法` -> `3. 断言结果`） |
| **08** | **行尾机制说明注释** | `代码语句; // 原因或副作用说明` | 解释单行代码背后的非直观机制、性能取舍或隐式副作用（如事务回滚、缓存跳过、拦截器终止） |
| **09** | **待办标记注释 (`TODO`)** | 类头：` * TODO: 待办事项`<br>行内：`// TODO: 待办或已知问题说明` | ① 类头 Javadoc 中罗列模块级演进计划<br>② 待重构代码块正上方（独立成行）或存在已知边缘问题的语句行尾 |

---

### 2. 核心注释规则细则

1. **语言与空格（Pangu Spacing）**：
   - 全仓注释统一使用**中文**，采用动宾短语/命令式语气（如 `创建用户接口`、`获取脱敏的当前登录用户信息`）。
   - **中文字符与英文单词、数字、代码符号之间必须保留 1 个半角空格**（例如：`注入 Redis 管理依赖`、`缺少题目 id 参数`、`不能超过 128 个字符`）。
2. **类头注释 (`@author wobushi041`)**：
   - 第一行为简短中文职责描述，不加句号。
   - 若有模块级待办事项，在职责下方以 ` * TODO: ...` 罗列。
   - 描述（或 `TODO`）与 `@author wobushi041` 之间**固定空 1 行（` *`）**。
   - **禁止添加** `@date`、`@since`、`@createDate`、`@version` 等冗余标签。
3. **方法文档注释 (`@param` & `@return`)**：
   - **全覆盖原则**：包括 `ServiceImpl` 中带有 `@Override` 的重写方法以及类的 `private` 辅助方法在内，**所有方法都必须编写标准多行 Javadoc**。
   - **接口层与实现层的描述侧重点区分**：
     - **`Service` 接口层（面向调用方，讲「做什么 / 业务契约」）**：第一行描述聚焦于对外提供的业务能力，不暴露底层具体表名、SQL 或缓存实现细节（例如：`保存异常场景下的兜底聊天消息`）。
     - **`ServiceImpl` 实现层（面向维护方，讲「怎么做 / 落地机制」）**：`@Override` 方法的第一行描述应点明具体的存储介质、技术手段或关键副作用（例如：`兜底保存未成功落库的聊天消息至 Redis`）。
   - 功能描述与标签区之间**必须空 1 行（` *`）**。
   - **`@param`（如有）**：仅当方法存在入参时编写 `@param 参数名 中文说明`，多个参数的中文说明使用空格对齐；无参方法不写 `@param`。
   - **`@return`（如有）**：仅当方法返回值非 `void` 时编写 `@return 中文说明`；`void` 方法不写 `@return`。

---

## 二、 代码格式、排版与命名规范

### 1. 缩进、行宽与空行
- **缩进单位**：Java 后端统一使用 **4 Spaces**；TypeScript/React 前端统一使用 **2 Spaces**（严禁使用 Tab）。
- **单行宽度**：常规代码控制在 **120 列**以内；`ThrowUtils.throwIf(...)` 的长中文提示或 `log.info(...)` 日志模板允许自然延伸，不强行拆断中文字符串。
- **空行留白**：
  - 类声明 `{` 下方**空 1 行**；类结束 `}` 上方**空 1 行**。
  - 类内部的所有静态常量之间、`@Resource` 字段之间、方法与方法之间，**严格间隔 1 个空行**。
  - 方法内部按逻辑阶段分块，每个阶段块之间**空 1 行**。

### 2. 括号、控制流与防御式校验
- **K&R 大括号风格**：左大括号 `{` 位于行尾（前留 1 个空格）。
- **单行控制流铁律**：Java 代码中所有 `if / else if / else / for / while` **100% 强制使用 `{}` 包裹**，严禁省略花括号。
- **防御式参数校验范式**：
  - 优先使用 `ThrowUtils.throwIf(condition, CodeBindMessageEnums.XXX, "中文错误提示");`。
  - 对象判空校验后，紧跟一行 `assert obj != null;` 消除 IDE 静态空指针告警。
- **链式调用折行**：
  - MyBatis-Plus `LambdaQueryWrapper` 多条件链式调用时，每个条件独占一行（+8 空格缩进），末尾分号 `;` 独立占一行并与变量名对齐。

### 3. 分层命名与后缀约定
- **包命名**：全小写按职责分层（`controller`、`service`、`service.impl`、`manager.<组件>`、`mapper`、`model.entity`、`model.dto.<业务域>`、`model.vo`、`model.enums`、`constant`、`exception`、`utils`）。
- **类名后缀**：
  - 控制层：`XxxController`
  - 服务层：`XxxService`（接口） / `XxxServiceImpl`（实现类），领域方法建议冠以主语前缀（如 `userIsExist`、`userIsAdmin`）
  - 第三方/中间件封装：`XxxManager` + `XxxConfig`（置于 `manager.<组件>` 包下）
  - 数据库实体：置于 `model.entity`，使用纯名词 `UpperCamelCase`（如 `User`、`Topic`，不加 `Entity` 或 `DO` 后缀）
  - 请求入参模型：置于 `model.dto.<业务域>`，统一以 **`XxxRequest` 结尾**（如 `UserAddRequest`、`TopicQueryRequest`，不使用 `DTO` 后缀）
  - 视图响应模型：置于 `model.vo`，统一以 **`XxxVO` 结尾**（如 `UserVO`、`LoginUserVO`）
  - 枚举与常量：枚举以 **`XxxEnum` 结尾**；常量使用 `public interface XxxConstant`（成员名 `UPPER_SNAKE_CASE`）
  - 响应体封装：统一返回 `BaseResponse<T>`，使用 `TheResult.success(...)` 或 `TheResult.error(...)` 构造。

### 4. 导入与依赖注入
- **两段式 Import 组织**：
  1. 第一组：项目内部包（`cn.com.edtechhub...`）+ 第三方依赖包（`cn.*`、`com.*`、`lombok.*`、`org.*`），按字母升序排列。
  2. **空 1 行**。
  3. 第二组：`javax.*` 与 `java.*` JDK 标准库，按字母升序排列。
- **Bean 依赖注入**：统一使用 `@Resource` 搭配 `private` 修饰符。

---

## 三、 标准代码模版示范

### 1. 请求模型 (`XxxRequest`) 与视图模型 (`XxxVO`)
```java
package cn.com.edtechhub.worktopicselection.model.dto.topic;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 题目审核请求
 *
 * @author wobushi041
 */
@Data
public class TopicReviewRequest implements Serializable {

    /**
     * 题目标识
     */
    private Long id;

    /**
     * 审核状态 0-打回 1-通过
     */
    private Integer reviewStatus;

    /**
     * 打回原因
     */
    private String rejectReason;

    /// 序列化字段 ///
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
```

### 2. 控制层 (`XxxController`)
```java
package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.sentine.SentineManager;
import cn.com.edtechhub.worktopicselection.model.dto.topic.TopicReviewRequest;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.enums.TopicStatusEnum;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 选题控制层
 * TODO: 支持批量审核导出功能
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/topic")
@Slf4j
public class TopicController {

    /**
     * 注入流量控制依赖
     */
    @Resource
    private SentineManager sentineManager;

    /**
     * 注入选题服务依赖
     */
    @Resource
    private TopicService topicService;

    /// 选题审核相关接口 ///

    /**
     * 审核题目接口
     *
     * @param request 题目审核请求参数
     * @return 是否审核成功
     */
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewTopic(@RequestBody TopicReviewRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long topicId = request.getId();
        ThrowUtils.throwIf(topicId == null || topicId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "缺少题目 id 参数");

        Topic topic = topicService.getById(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "审核的题目不存在");
        assert topic != null;

        // 如果审核状态为打回则需要检查是否填写了原因
        if (request.getReviewStatus() == TopicStatusEnum.REJECTED.getCode()) {
            ThrowUtils.throwIf(StringUtils.isBlank(request.getRejectReason()), CodeBindMessageEnums.PARAMS_ERROR, "打回题目时必须填写原因");
        }
        // 兜底情况
        else {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.PARAMS_ERROR, "非法的审核状态码");
        }

        // 执行状态更新
        boolean result = topicService.updateById(topic); // TODO: 后续补充站内信通知逻辑
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "更新题目审核状态失败");
        return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
    }

}
```

### 3. 服务接口与实现类 (`XxxService` & `XxxServiceImpl`)
```java
/**
 * 选题服务
 *
 * @author wobushi041
 */
public interface TopicService extends IService<Topic> {

    /**
     * 检查指定教师名下是否存在同名题目（接口层：描述业务契约「做什么」）
     *
     * @param topicTitle     题目标题
     * @param teacherAccount 教师工号
     * @return 匹配的题目实体
     */
    Topic topicIsExist(String topicTitle, String teacherAccount);

}

/**
 * 选题服务实现
 *
 * @author wobushi041
 */
@Service
@Slf4j
@Transactional
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    /**
     * 通过 Lambda 条件构造器查询数据库是否存在同名题目（实现层：描述落地机制「怎么做」）
     *
     * @param topicTitle     题目标题
     * @param teacherAccount 教师工号
     * @return 数据库中匹配的题目实体
     */
    @Override
    public Topic topicIsExist(String topicTitle, String teacherAccount) {
        LambdaQueryWrapper<Topic> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Topic::getTopic, topicTitle)
                .eq(Topic::getTeacherAccount, teacherAccount)
        ;
        return this.getOne(lambdaQueryWrapper);
    }

}
```

### 4. 单元测试 (`XxxTest`)
```java
/**
 * TheResult 工具类单元测试
 *
 * @author wobushi041
 */
class TheResultTest {

    // 场景：测试 success 方法（成功响应体）
    @Test
    void testSuccess() {
        // 1. 准备测试数据
        CodeBindMessageEnums successEnum = CodeBindMessageEnums.SUCCESS;
        String testData = "测试成功数据";

        // 2. 调用 success 方法
        BaseResponse<String> response = TheResult.success(successEnum, testData);

        // 3. 断言响应体字段正确
        assertEquals(successEnum.getCode(), response.getCode(), "状态码应匹配枚举值");
        assertEquals(testData, response.getData(), "数据字段应正确赋值");
    }

}
```
