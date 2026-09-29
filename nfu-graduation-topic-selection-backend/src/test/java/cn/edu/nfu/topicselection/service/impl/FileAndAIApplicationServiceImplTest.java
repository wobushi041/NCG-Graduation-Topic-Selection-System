package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.ai.AiSendRequest;
import cn.edu.nfu.topicselection.model.request.file.UploadFileRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.SqlExportService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FileApplicationServiceImpl 与 AIApplicationServiceImpl 单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class FileAndAIApplicationServiceImplTest {

    /**
     * 模拟 SQL 导出服务
     */
    @Mock
    private SqlExportService sqlExportService;

    /**
     * 模拟用户服务
     */
    @Mock
    private UserService userService;

    /**
     * 模拟密码服务
     */
    @Mock
    private PasswordService passwordService;

    /**
     * 模拟选题服务
     */
    @Mock
    private TopicService topicService;

    /**
     * 模拟系部服务
     */
    @Mock
    private CollegeService collegeService;

    /**
     * 模拟专业服务
     */
    @Mock
    private MajorService majorService;

    /**
     * 模拟学生选题关联服务
     */
    @Mock
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 被测文件应用服务实现
     */
    private FileApplicationServiceImpl fileApplicationService;

    /**
     * 被测 AI 应用服务实现
     */
    private AIApplicationServiceImpl aiApplicationService;

    /**
     * 初始化测试环境
     */
    @BeforeEach
    void setUp() {
        fileApplicationService = new FileApplicationServiceImpl(
                sqlExportService,
                userService,
                passwordService,
                topicService,
                collegeService,
                majorService,
                studentTopicSelectionService
        );
        aiApplicationService = new AIApplicationServiceImpl();
    }

    // 场景：测试 FileApplicationServiceImpl 仅在 uploadFile 写方法上标注 @Transactional 且只读导出方法无写事务（达成 ARCH-04 与 ARCH-05）
    @Test
    void fileApplicationServiceImpl_shouldAnnotateOnlyUploadFileWithTransactional() throws Exception {
        // 1. 准备测试数据并获取类级与方法级注解
        Transactional classTx = FileApplicationServiceImpl.class.getAnnotation(Transactional.class);
        Method uploadFileMethod = FileApplicationServiceImpl.class.getMethod("uploadFile", MultipartFile.class, UploadFileRequest.class);
        Method exportUserMethod = FileApplicationServiceImpl.class.getMethod("exportUserListRows");

        // 2. 获取方法上的 @Transactional 注解
        Transactional uploadFileTx = uploadFileMethod.getAnnotation(Transactional.class);
        Transactional exportUserTx = exportUserMethod.getAnnotation(Transactional.class);

        // 3. 断言类级与导出方法无 @Transactional 且 uploadFile 具备 @Transactional(rollbackFor = Exception.class)
        Assertions.assertNull(classTx);
        Assertions.assertNotNull(uploadFileTx);
        Assertions.assertArrayEquals(new Class<?>[]{Exception.class}, uploadFileTx.rollbackFor());
        Assertions.assertNull(exportUserTx);
    }

    // 场景：测试 uploadFile 成功解析合法学生 CSV 并保存用户返回成功响应
    @Test
    @SuppressWarnings("unchecked")
    void uploadFile_withValidStudentCsv_shouldSaveUserAndReturnSuccess() {
        // 1. 准备测试数据
        String csvContent = "学号,姓名,系部,专业,临时密码\n20260001,李四,计算机系,软件工程,TempPass#202601\n";
        MockMultipartFile csvFile = new MockMultipartFile("file", "students.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        UploadFileRequest request = new UploadFileRequest();
        request.setStatus(UserRoleEnum.STUDENT.getCode());
        Mockito.when(passwordService.isPasswordValid("TempPass#202601")).thenReturn(true);
        Mockito.when(passwordService.encodePassword("TempPass#202601")).thenReturn("encoded_pw");
        Mockito.when(userService.getOne(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(null);
        Mockito.when(collegeService.getOne(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(new College());
        Mockito.when(majorService.getOne(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(new Major());

        // 2. 调用 uploadFile 方法
        BaseResponse<String> response = fileApplicationService.uploadFile(csvFile, request);

        // 3. 断言保存成功并返回预期结果
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), response.getCode());
        Assertions.assertEquals("批量添加成功", response.getData());
        Mockito.verify(userService).saveOrUpdate(ArgumentMatchers.any());
    }

    // 场景：测试 uploadFile 当系部不存在时抛出 ILLEGAL_OPERATION_ERROR 异常以触发事务回滚
    @Test
    @SuppressWarnings("unchecked")
    void uploadFile_whenCollegeNotExists_shouldThrowIllegalOperationError() {
        // 1. 准备测试数据
        String csvContent = "学号,姓名,系部,专业,临时密码\n20260002,王五,不存在系部,软件工程,TempPass#202602\n";
        MockMultipartFile csvFile = new MockMultipartFile("file", "students.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));
        UploadFileRequest request = new UploadFileRequest();
        request.setStatus(UserRoleEnum.STUDENT.getCode());
        Mockito.when(passwordService.isPasswordValid("TempPass#202602")).thenReturn(true);
        Mockito.when(userService.getOne(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(null);
        Mockito.when(collegeService.getOne(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(null);

        // 2. 调用 uploadFile 方法并捕获业务异常
        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> fileApplicationService.uploadFile(csvFile, request)
        );

        // 3. 断言错误码与提示信息正确
        Assertions.assertEquals(CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, ex.getCodeBindMessageEnums());
        Assertions.assertTrue(ex.getMessage().contains("学院 [不存在系部] 在系统中不存在"));
    }

    // 场景：测试 uploadFileTopic、exportUserListRows 与 aiSend 的行为及参数校验
    @Test
    void uploadFileTopicAndExportAndAiSend_shouldBehaveAsExpected() {
        // 1. 准备测试数据
        AiSendRequest blankAiRequest = new AiSendRequest();
        blankAiRequest.setContent("   ");
        AiSendRequest validAiRequest = new AiSendRequest();
        validAiRequest.setContent("请帮我推荐选题方向");
        Mockito.when(sqlExportService.executeQuery(ArgumentMatchers.anyString()))
                .thenReturn(Collections.singletonList(Collections.singletonMap("帐号", "admin")));

        // 2. 调用被测方法并校验异常与返回值
        BaseResponse<String> topicImportRes = fileApplicationService.uploadFileTopic(null);
        List<Map<String, Object>> exportedUsers = fileApplicationService.exportUserListRows();
        BusinessException blankEx = Assertions.assertThrows(
                BusinessException.class,
                () -> aiApplicationService.aiSend(blankAiRequest)
        );
        BaseResponse<String> aiRes = aiApplicationService.aiSend(validAiRequest);

        // 3. 断言结果完全符合预期
        Assertions.assertTrue(topicImportRes.getMessage().contains("该功能有缺陷暂时不开放使用！"));
        Assertions.assertEquals(1, exportedUsers.size());
        Assertions.assertEquals("请不要发送空消息", blankEx.getMessage());
        Assertions.assertTrue(aiRes.getMessage().contains("AI 问答功能暂未开放"));
    }

}
