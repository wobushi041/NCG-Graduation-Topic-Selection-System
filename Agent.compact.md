# 项目编码风格与注释规范 (Agent.compact.md)

在编写、修改或重构任何代码时，必须严格执行以下规范。

## 一、 核心注释红线（最高优先级）

1. **语言与中英空格**：全仓注释统一使用中文（动宾短语/命令式语气）。**中文与英文单词、数字、代码符号之间必须保留 1 个半角空格**（如 `注入 Redis 依赖`、`缺少题目 id 参数`）。
2. **类与接口头注释**：
   - 格式：`/**` + 首行简短中文职责描述（不加句号）+ `[可选 * TODO: 待办项]` + 空行 ` *` + `@author wobushi041` + `*/`。
   - 严禁添加 `@date`、`@since`、`@createDate`、`@version`。
3. **方法文档注释（全覆盖原则）**：
   - **适用范围**：`Controller`、`Service`、`ServiceImpl`（**含 `@Override` 重写方法**）、`Manager`、`Utils`、构造器及类的 **`private` 辅助方法**，100% 必须编写多行 Javadoc。
   - **接口与实现侧重点区分**：
     - **`Service` 接口层**：面向调用方，首行描述**「做什么（业务契约）」**，禁提底层表名、SQL 或缓存键细节。
     - **`ServiceImpl` 实现层**：面向维护方，`@Override` 首行描述**「怎么做（落地机制）」**，点明具体存储介质、技术手段或关键副作用。
   - **标签规范**：功能描述与标签区之间**固定空 1 行（` *`）**；有入参时必写 `@param 参数名 中文说明`（多参数说明用空格对齐），无参不写；非 `void` 返回值必写 `@return 中文说明`，`void` 不写。
4. **字段、枚举与三斜杠分区 (`///`)**：
   - `Entity`、`Request`、`VO`、`Enum` 的所有属性/枚举项，以及 `@Resource` / `private final` 注入字段，均使用**三行 Javadoc**（`/** \n * 含义 \n */`）；枚举实例末尾以独立成行的 `;` 结束。
   - 所有 `Request` / `VO` 类的 `serialVersionUID` 必须置于**类最底部**，并在正上方标注 `/// 序列化字段 ///`。
   - 大体积类内部按业务模块使用 `/// 模块名称 ///` 分区（如 `/// 认证相关接口 ///`）。
5. **方法内阶段注释与单元测试编号**：
   - **生产代码内部**：禁用数字编号，按逻辑阶段使用无序号单行注释（如 `// 流量控制`、`// 参数检查`）配合空行分段；`else if` / `else` 的说明注释写在上一分支 `}` 下方、`else` 上方。
   - **单元测试 (`*Test.java`)**：`@Test` 上方写 `// 场景：测试 xxx`，方法内严格使用三段式编号：`// 1. 准备测试数据`、`// 2. 调用 xxx 方法`、`// 3. 断言响应体字段正确`。

## 二、 代码格式、排版与命名规范

1. **缩进与空行**：Java 统一 **4 空格**，前端 TS/React **2 空格**（禁 Tab）。类 `{` 后、类 `}` 前、以及所有常量、字段、方法之间**固定间隔 1 个空行**。常规行宽 120 列（长中文错误提示不强行拆断）。
2. **控制流与参数校验**：K&R 行尾 `{` 风格；Java 所有 `if / else / for / while` **强制带 `{}`**。若项目含 `ThrowUtils`，优先使用 `ThrowUtils.throwIf(...)` 紧跟 `assert obj != null;`；MyBatis-Plus `LambdaQueryWrapper` 多条件链式调用时末尾 `;` 独立成行并与变量名对齐。
3. **两段式导包**：第 1 组为项目包 + 第三方包（字母升序），**空 1 行**，第 2 组为 `jakarta.* / javax.*` + `java.*` 标准库（字母升序）。
4. **分层后缀约定**：请求模型用 `XxxRequest`、视图模型用 `XxxVO`、实体类用纯名词、中间件封装用 `XxxManager` + `XxxConfig`、枚举用 `XxxEnum`、常量用 `interface XxxConstant`。

## 三、 All-in-One 复合标准模版

```java
/**
 * 聊天消息服务实现
 * TODO: 支持消息异步批量归档
 *
 * @author wobushi041
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    /**
     * 注入 Redis 模板依赖
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /// 消息存储相关方法 ///

    /**
     * 兜底保存未成功落库的聊天消息至 Redis（ServiceImpl：讲怎么做/落地机制；Service 接口则写：保存异常场景下的兜底聊天消息）
     *
     * @param message        聊天消息对象
     * @param timeoutSeconds 过期时间（秒）
     * @return 是否写入成功
     */
    @Override
    public boolean saveFallbackMessage(ChatMessageResponse message, long timeoutSeconds) {
        // 参数检查
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息参数不能为空");
        }

        // 写入 Redis 列表并设置过期时间
        String key = "chat:fallback:" + message.getTeamId(); // TODO: 后续提取为常量
        redisTemplate.opsForList().rightPush(key, message);
        return true;
    }

    /// 序列化字段（仅用于 Request / VO 类最底部） ///
    private static final long serialVersionUID = 1L;

}
```

## 四、 输出代码前强制自检清单 (Checklist)

- [ ] 类头是否已标注 `@author wobushi041` 且未混入 `@date` / `@createDate`？
- [ ] **所有方法（特别是 `@Override` 方法与 `private` 辅助方法）**是否均已打上包含 `@param`（空格对齐）与 `@return` 的 Javadoc？
- [ ] `Service` 接口注释（做什么）与 `ServiceImpl` 实现注释（怎么做）是否已区分侧重点？
- [ ] 所有常量、`@Resource` 字段、枚举项是否均已添加三行 Javadoc 且彼此间隔 1 个空行？
- [ ] `Request` / `VO` 的 `serialVersionUID` 是否已移至类底部并标注 `/// 序列化字段 ///`？
- [ ] 中文与英文、数字之间是否均已保留 1 个半角空格？
