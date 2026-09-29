package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.file.UploadFileRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.FileApplicationService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件批量导入与统计导出控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /**
     * 注入文件批量导入与统计导出应用服务依赖
     */
    private final FileApplicationService fileApplicationService;

    /**
     * 构造文件批量导入与统计导出控制层实例
     *
     * @param fileApplicationService 文件批量导入与统计导出应用服务
     */
    public FileController(FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    /// 文件批量导入与统计导出接口 ///

    /**
     * 根据模板文件批量添加角色账号
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @param request       文件上传请求参数
     * @return 批量导入结果说明
     */
    @SentinelRateLimit(resource = "file.user.import")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile, UploadFileRequest request) {
        return fileApplicationService.uploadFile(multipartFile, request);
    }

    /**
     * 根据模板文件批量添加毕设选题
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @return 批量导入结果说明
     */
    @SentinelRateLimit(resource = "file.topic.import")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/upload/topic")
    public BaseResponse<String> uploadFileTopic(@RequestPart("file") MultipartFile multipartFile) {
        return fileApplicationService.uploadFileTopic(multipartFile);
    }

    /**
     * 获取已经选择的学生题目列表
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.selection.selected-export")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "topic_leader"}, mode = SaMode.OR)
    @PostMapping("/get/select/topic/student/list")
    public void getSelectTopicStudentListCsv(HttpServletResponse httpServletResponse) {
        List<List<String>> rows = fileApplicationService.listSelectedStudentTopicCsvRows();

        // 设置 HTTP 响应的内容类型和文件名
        httpServletResponse.setContentType("text/csv");
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename*=UTF-8''%E5%B7%B2%E9%80%89%E9%A2%98%E5%AD%A6%E7%94%9F%E5%88%97%E8%A1%A8.csv");

        try (ServletOutputStream outputStream = httpServletResponse.getOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("学号", "姓名", "专业", "学院", "题目", "指导老师"))) {

            // 将已选题学生信息写入 CSV 文件
            for (List<String> row : rows) {
                csvPrinter.printRecord(row.stream().map(FileController::sanitizeCsvCell).collect(Collectors.toList()));
            }

            csvPrinter.flush();
        } catch (IOException e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.OPERATION_ERROR, "导出 CSV 失败");
        }
    }

    /**
     * 获取尚未选择的学生题目列表
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.selection.unselected-export")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "topic_leader"}, mode = SaMode.OR)
    @PostMapping("/get/unselect/topic/student/list")
    public void getUnSelectTopicStudentListCsv(HttpServletResponse httpServletResponse) {
        List<User> unselectedUsers = fileApplicationService.listUnselectedStudentCsvUsers();

        httpServletResponse.setContentType("text/csv");
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename*=UTF-8''%E6%B2%A1%E6%9C%89%E9%80%89%E9%A2%98%E5%AD%A6%E7%94%9F%E5%88%97%E8%A1%A8.csv");

        try (ServletOutputStream outputStream = httpServletResponse.getOutputStream();
             Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("用户账号", "用户名", "专业", "学院"))) {

            for (User user : unselectedUsers) {
                csvPrinter.printRecord(
                        sanitizeCsvCell(user.getUserAccount()),
                        sanitizeCsvCell(user.getUserName()),
                        sanitizeCsvCell(user.getMajorId()),
                        sanitizeCsvCell(user.getCollegeId())
                );
            }

            csvPrinter.flush();
        } catch (IOException e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.OPERATION_ERROR, "导出 CSV 失败");
        }
    }

    /**
     * 导出系统内所有帐号
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.export.user-list")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/export/user_list")
    public void exportUserList(HttpServletResponse httpServletResponse) {
        writeMapRowsToCsv(httpServletResponse, "系统内所有帐号.csv", fileApplicationService.exportUserListRows());
    }

    /**
     * 导出系统内所有题目
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.export.topic-list")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/export/topic_list")
    public void exportTopicList(HttpServletResponse httpServletResponse) {
        writeMapRowsToCsv(httpServletResponse, "所有未出题教师.csv", fileApplicationService.exportTopicListRows());
    }

    /**
     * 导出系统内剩余题目
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.export.surplus-topic-list")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/export/surplus_topic_list")
    public void exportSurplusTopicList(HttpServletResponse httpServletResponse) {
        writeMapRowsToCsv(httpServletResponse, "所有剩余的题目.csv", fileApplicationService.exportSurplusTopicListRows());
    }

    /**
     * 导出系统内已选学生
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.export.student-en-select")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/export/student_topic_list/en_select")
    public void exportStudentTopicListEnSelect(HttpServletResponse httpServletResponse) {
        writeMapRowsToCsv(httpServletResponse, "已选学生.csv", fileApplicationService.exportSelectedStudentRows());
    }

    /**
     * 导出系统内未选学生
     *
     * @param httpServletResponse HTTP 响应对象
     */
    @SentinelRateLimit(resource = "file.export.student-un-select")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/export/student_topic_list/un_select")
    public void exportStudentTopicListUnSelect(HttpServletResponse httpServletResponse) {
        writeMapRowsToCsv(httpServletResponse, "未选学生.csv", fileApplicationService.exportUnselectedStudentRows());
    }

    /// CSV 写出与公式注入防护辅助方法 ///

    /**
     * 将 SQL 导出结果映射行集合按 UTF-8 编码写出至 HTTP 响应 CSV 流
     *
     * @param httpServletResponse HTTP 响应对象
     * @param fileName            导出 CSV 文件名
     * @param rows                导出字段映射行列表
     */
    private void writeMapRowsToCsv(HttpServletResponse httpServletResponse, String fileName, List<Map<String, Object>> rows) {
        httpServletResponse.setContentType("text/csv");
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        try (ServletOutputStream outputStream = httpServletResponse.getOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader(rows.isEmpty() ? new String[]{"无数据"} : rows.get(0).keySet().toArray(new String[0])))) {

            // 遍历行写入 CSV
            for (Map<String, Object> row : rows) {
                List<Object> values = new ArrayList<>();
                for (String key : row.keySet()) {
                    values.add(sanitizeCsvCell(row.get(key)));
                }
                csvPrinter.printRecord(values);
            }

            csvPrinter.flush();
        } catch (IOException e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.OPERATION_ERROR, "导出 CSV 失败");
        }
    }

    /**
     * 过滤 CSV 单元格内容以防止公式注入攻击
     *
     * @param value 原始单元格值
     * @return 安全处理后的单元格值
     */
    static Object sanitizeCsvCell(Object value) {
        if (!(value instanceof CharSequence)) {
            return value;
        }
        String text = value.toString();
        int firstContentIndex = 0;
        while (firstContentIndex < text.length() && Character.isWhitespace(text.charAt(firstContentIndex))) {
            firstContentIndex++;
        }
        if (firstContentIndex < text.length()
                && "=+-@".indexOf(text.charAt(firstContentIndex)) >= 0) {
            return "'" + text;
        }
        return text;
    }

}
