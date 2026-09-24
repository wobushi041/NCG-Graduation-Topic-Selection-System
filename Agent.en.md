# Coding & Commenting Guidelines (Agent.en.md)

You MUST strictly follow these rules when generating, modifying, or refactoring code.

## 1. Commenting Rules (Highest Priority)

- **Language & Spacing**: Write ALL comments and error messages in **Chinese** (imperative verb phrases). ALWAYS keep 1 half-width space between Chinese characters and English words/numbers/identifiers (e.g., `注入 Redis 依赖`, `缺少题目 id 参数`).
- **Class / Interface Header Javadoc**:
  - Format: Multi-line `/** ... */` -> Line 1: concise Chinese role summary (no period) -> `[Optional: * TODO: ...]` -> empty comment line ` *` -> `@author wobushi041`.
  - NEVER include `@date`, `@since`, `@createDate`, or `@version`.
- **Method Javadoc (100% Coverage)**:
  - EVERY method in `Controller`, `Service`, `ServiceImpl` (**including `@Override` methods**), `Manager`, `Utils`, constructors, and **`private` helper methods** MUST have a multi-line Javadoc.
  - **Layer Focus Distinction**:
    - **`Service` Interface**: Describe **WHAT** business contract it fulfills (for callers), without exposing table names or cache keys (e.g., `保存异常场景下的兜底聊天消息`).
    - **`ServiceImpl` Class**: Describe **HOW** the concrete mechanism works on `@Override` methods (for maintainers), specifying storage or side effects (e.g., `兜底保存未成功落库的聊天消息至 Redis`).
  - **Tags**: Leave 1 blank comment line (` *`) before tags. Include `@param <name> <Chinese desc>` ONLY if parameters exist (align descriptions with spaces). Include `@return <Chinese desc>` ONLY if return type is not `void`.
- **Fields, Enums & `///` Section Dividers**:
  - Use 3-line Javadoc (`/** \n * 中文含义 \n */`) for ALL fields (`Entity`, `Request`, `VO`, `Enum`, and `@Resource` / `private final` dependencies). End enum constants with `;` on a new line.
  - Place `serialVersionUID` at the **very bottom** of `Request` / `VO` classes, preceded by the exact line: `/// 序列化字段 ///`.
  - Group functional sections in large classes using `/// 模块名称 ///` (e.g., `/// 认证相关接口 ///`).
- **Inline Phase & Unit Test Comments**:
  - **Production Code**: NEVER use numbered steps (`1. 2.`). Use unnumbered Chinese phase comments (`// 流量控制`, `// 参数检查`) separated by 1 blank line. Place `else if` / `else` explanations on the line above `else`.
  - **Unit Tests (`*Test.java`)**: Place `// 场景：测试 xxx` above `@Test`, and strictly use numbered steps inside: `// 1. 准备测试数据`, `// 2. 调用 xxx 方法`, `// 3. 断言响应体字段正确`.

## 2. Formatting & Architecture Rules

- **Indentation & Blank Lines**: 4 spaces for Java, 2 spaces for TS/React (NO Tabs). Leave 1 blank line after class `{`, before class `}`, and between EVERY constant, field, and method.
- **Braces & Validation**: K&R style (`{` at end of line); ALWAYS use `{}` for `if/else/for/while`. Prefer `ThrowUtils.throwIf(...)` + `assert obj != null;` where available. Place trailing `;` of multi-line `LambdaQueryWrapper` chains on its own aligned line.
- **Imports**: 2 groups separated by 1 blank line: (1) project + 3rd-party packages alphabetically, (2) `jakarta.* / javax.*` + `java.*` alphabetically.
- **Naming Suffixes**: DTOs -> `XxxRequest`; VOs -> `XxxVO`; Entities -> pure nouns; Middleware wrappers -> `XxxManager` + `XxxConfig`; Enums -> `XxxEnum`; Constants -> `interface XxxConstant`.

## 3. All-in-One Reference Snippet

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
     * 兜底保存未成功落库的聊天消息至 Redis（ServiceImpl：讲怎么做；Service 接口写：保存异常场景下的兜底聊天消息）
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

        // 写入 Redis 列表
        String key = "chat:fallback:" + message.getTeamId(); // TODO: 后续提取为常量
        redisTemplate.opsForList().rightPush(key, message);
        return true;
    }

    /// 序列化字段（Request / VO 类固定放最底部） ///
    private static final long serialVersionUID = 1L;

}
```

## 4. Pre-Output Verification Checklist

- [ ] Does the class header have `@author wobushi041` and NO `@date` / `@createDate`?
- [ ] Do **ALL methods (including `@Override` and `private` helpers)** have multi-line Javadoc with space-aligned `@param` and `@return`?
- [ ] Do `Service` (what/contract) and `ServiceImpl` (how/mechanism) Javadocs have distinct focuses?
- [ ] Do all constants, `@Resource` fields, and enum items have 3-line Javadocs separated by 1 blank line?
- [ ] Is `serialVersionUID` at the bottom of `Request` / `VO` with `/// 序列化字段 ///`?
- [ ] Is there 1 half-width space between all Chinese and English/numbers?
