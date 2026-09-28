package cn.edu.nfu.topicselection.model.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户实体序列化脱敏测试
 *
 * @author wobushi041
 */
class UserSerializationTest {

    // 场景：测试序列化 User 对象时不暴露密码与邮箱字段
    @Test
    void serializedUserDoesNotExposePasswordOrEmail() throws Exception {
        User user = new User();
        user.setUserAccount("student001");
        user.setUserPassword("bcrypt-hash");
        user.setEmail("student@example.com");

        String json = new ObjectMapper().writeValueAsString(user);

        assertTrue(json.contains("student001"));
        assertFalse(json.contains("userPassword"));
        assertFalse(json.contains("bcrypt-hash"));
        assertFalse(json.contains("email"));
        assertFalse(json.contains("student@example.com"));
    }

    // 场景：测试 User 的 toString 方法不暴露密码与邮箱内容
    @Test
    void toStringDoesNotExposePasswordOrEmail() {
        User user = new User();
        user.setUserPassword("bcrypt-hash");
        user.setEmail("student@example.com");

        String text = user.toString();

        assertFalse(text.contains("bcrypt-hash"));
        assertFalse(text.contains("student@example.com"));
    }

}
