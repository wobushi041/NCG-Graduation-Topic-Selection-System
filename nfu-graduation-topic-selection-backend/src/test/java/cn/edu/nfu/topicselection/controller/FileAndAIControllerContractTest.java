package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.ai.AiSendRequest;
import cn.edu.nfu.topicselection.model.request.file.UploadFileRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.AIApplicationService;
import cn.edu.nfu.topicselection.service.FileApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FileController 与 AIController 路由契约及架构净化守护测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class FileAndAIControllerContractTest {

    /**
     * 模拟文件批量导入与统计导出应用服务
     */
    @Mock
    private FileApplicationService fileApplicationService;

    /**
     * 模拟 AI 问答应用服务
     */
    @Mock
    private AIApplicationService aiApplicationService;

    /**
     * 被测文件控制层
     */
    private FileController fileController;

    /**
     * 被测智能控制层
     */
    private AIController aiController;

    /**
     * 初始化测试环境
     */
    @BeforeEach
    void setUp() {
        fileController = new FileController(fileApplicationService);
        aiController = new AIController(aiApplicationService);
    }

    // 场景：测试 FileController 与 AIController 共 10 个接口的路由、Sentinel 资源名与 Sa-Token 鉴权注解契约
    @Test
    void fileAndAIControllers_shouldPreserveRoutesSentinelAndSaTokenAnnotations() throws Exception {
        // 1. 准备测试数据并获取类级注解
        RequestMapping fileMapping = FileController.class.getAnnotation(RequestMapping.class);
        RequestMapping aiMapping = AIController.class.getAnnotation(RequestMapping.class);

        // 2. 校验类级路径前缀并获取 10 个接口方法反射对象
        Assertions.assertNotNull(fileMapping);
        Assertions.assertArrayEquals(new String[]{"/file"}, fileMapping.value());
        Assertions.assertNotNull(aiMapping);
        Assertions.assertArrayEquals(new String[]{"/ai"}, aiMapping.value());
        Method uploadFileMethod = FileController.class.getMethod("uploadFile", MultipartFile.class, UploadFileRequest.class);
        Method uploadTopicMethod = FileController.class.getMethod("uploadFileTopic", MultipartFile.class);
        Method selectedCsvMethod = FileController.class.getMethod("getSelectTopicStudentListCsv", HttpServletResponse.class);
        Method unselectedCsvMethod = FileController.class.getMethod("getUnSelectTopicStudentListCsv", HttpServletResponse.class);
        Method exportUserMethod = FileController.class.getMethod("exportUserList", HttpServletResponse.class);
        Method exportTopicMethod = FileController.class.getMethod("exportTopicList", HttpServletResponse.class);
        Method exportSurplusMethod = FileController.class.getMethod("exportSurplusTopicList", HttpServletResponse.class);
        Method exportEnSelectMethod = FileController.class.getMethod("exportStudentTopicListEnSelect", HttpServletResponse.class);
        Method exportUnSelectMethod = FileController.class.getMethod("exportStudentTopicListUnSelect", HttpServletResponse.class);
        Method aiSendMethod = AIController.class.getMethod("aiSend", AiSendRequest.class);

        // 3. 断言 10 个接口的 HTTP 路径、Sentinel 资源名与 Sa-Token 角色鉴权完全符合契约
        assertPostEndpoint(uploadFileMethod, "/upload", "file.user.import", new String[]{"admin"});
        assertPostEndpoint(uploadTopicMethod, "/upload/topic", "file.topic.import", new String[]{"teacher"});
        assertPostEndpoint(selectedCsvMethod, "/get/select/topic/student/list", "file.selection.selected-export", new String[]{"admin", "dept"});
        assertPostEndpoint(unselectedCsvMethod, "/get/unselect/topic/student/list", "file.selection.unselected-export", new String[]{"admin", "dept"});
        assertPostEndpoint(exportUserMethod, "/export/user_list", "file.export.user-list", new String[]{"admin"});
        assertPostEndpoint(exportTopicMethod, "/export/topic_list", "file.export.topic-list", new String[]{"admin"});
        assertPostEndpoint(exportSurplusMethod, "/export/surplus_topic_list", "file.export.surplus-topic-list", new String[]{"admin"});
        assertPostEndpoint(exportEnSelectMethod, "/export/student_topic_list/en_select", "file.export.student-en-select", new String[]{"admin"});
        assertPostEndpoint(exportUnSelectMethod, "/export/student_topic_list/un_select", "file.export.student-un-select", new String[]{"admin"});
        assertPostEndpoint(aiSendMethod, "/send", "ai.chat.send", new String[]{"student"});
    }

    // 场景：测试 FileController 与 AIController 在 Wave 5 完成后仅保留单一应用服务依赖（达成 ARCH-04 与 RATE-001）
    @Test
    void fileAndAIControllers_shouldHaveZeroTransactionTemplateAndZeroSentineManager() {
        // 1. 准备测试数据并获取两个 Controller 的声明字段
        Field[] fileFields = FileController.class.getDeclaredFields();
        Field[] aiFields = AIController.class.getDeclaredFields();

        // 2. 转换为列表检查字段数量与名称
        List<Field> fileFieldList = Arrays.asList(fileFields);
        List<Field> aiFieldList = Arrays.asList(aiFields);

        // 3. 断言均仅包含单一应用服务构造注入依赖
        Assertions.assertEquals(1, fileFieldList.size());
        Assertions.assertEquals("fileApplicationService", fileFieldList.get(0).getName());
        Assertions.assertEquals(1, aiFieldList.size());
        Assertions.assertEquals("aiApplicationService", aiFieldList.get(0).getName());
    }

    // 场景：测试 FileController 导入导出方法与 AIController 问答方法正确委托应用服务并输出 CSV 流
    @Test
    void fileAndAIControllers_shouldDelegateAndStreamCsvProperly() throws Exception {
        // 1. 准备测试数据与模拟返回值
        MockMultipartFile csvFile = new MockMultipartFile("file", "users.csv", "text/csv", "a,b\n".getBytes(StandardCharsets.UTF_8));
        UploadFileRequest uploadRequest = new UploadFileRequest();
        AiSendRequest aiSendRequest = new AiSendRequest();
        aiSendRequest.setContent("你好");
        BaseResponse<String> importSuccess = TheResult.success(
                cn.edu.nfu.topicselection.exception.CodeBindMessageEnums.SUCCESS,
                "批量添加成功"
        );
        BaseResponse<String> aiNotYet = TheResult.notyet("AI 问答功能暂未开放");
        User unselectedUser = new User();
        unselectedUser.setUserAccount("=stu01");
        unselectedUser.setUserName("张三");
        unselectedUser.setProject("软件工程");
        unselectedUser.setDept("计算机系");
        Map<String, Object> rowMap = new LinkedHashMap<>();
        rowMap.put("帐号", "=admin");
        Mockito.when(fileApplicationService.uploadFile(csvFile, uploadRequest)).thenReturn(importSuccess);
        Mockito.when(fileApplicationService.listUnselectedStudentCsvUsers()).thenReturn(Collections.singletonList(unselectedUser));
        Mockito.when(fileApplicationService.exportUserListRows()).thenReturn(Collections.singletonList(rowMap));
        Mockito.when(aiApplicationService.aiSend(aiSendRequest)).thenReturn(aiNotYet);

        // 2. 调用导入、CSV 导出与 AI 问答方法
        BaseResponse<String> resUpload = fileController.uploadFile(csvFile, uploadRequest);
        MockHttpServletResponse responseUnselected = new MockHttpServletResponse();
        fileController.getUnSelectTopicStudentListCsv(responseUnselected);
        MockHttpServletResponse responseUserExport = new MockHttpServletResponse();
        fileController.exportUserList(responseUserExport);
        BaseResponse<String> resAi = aiController.aiSend(aiSendRequest);

        // 3. 断言返回响应及 CSV 响应流（含公式注入转义单引号）完全正确
        Assertions.assertSame(importSuccess, resUpload);
        Assertions.assertSame(aiNotYet, resAi);
        String unselectedCsv = new String(responseUnselected.getContentAsByteArray(), StandardCharsets.UTF_8);
        String userExportCsv = new String(responseUserExport.getContentAsByteArray(), StandardCharsets.UTF_8);
        Assertions.assertTrue(unselectedCsv.contains("'=stu01"));
        Assertions.assertTrue(userExportCsv.contains("'=admin"));
    }

    /**
     * 校验 POST 接口方法的路径、Sentinel 限流注解与 Sa-Token 鉴权注解
     *
     * @param method        目标反射方法
     * @param expectedPath  期望的 POST 路径
     * @param expectedRes   期望的 Sentinel 资源名
     * @param expectedRoles 期望的 SaCheckRole 角色数组
     */
    private void assertPostEndpoint(Method method, String expectedPath, String expectedRes, String[] expectedRoles) {
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(new String[]{expectedPath}, postMapping.value());

        SentinelRateLimit rateLimit = method.getAnnotation(SentinelRateLimit.class);
        Assertions.assertNotNull(rateLimit);
        Assertions.assertEquals(expectedRes, rateLimit.resource());

        Assertions.assertNotNull(method.getAnnotation(SaCheckLogin.class));
        SaCheckRole saCheckRole = method.getAnnotation(SaCheckRole.class);
        Assertions.assertNotNull(saCheckRole);
        Assertions.assertArrayEquals(expectedRoles, saCheckRole.value());
    }

}
