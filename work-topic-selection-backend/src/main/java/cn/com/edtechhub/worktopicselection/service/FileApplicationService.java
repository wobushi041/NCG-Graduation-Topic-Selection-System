package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.file.UploadFileRequest;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件批量导入与统计导出应用服务接口
 *
 * @author wobushi041
 */
public interface FileApplicationService {

    /// 文件批量导入服务契约 ///

    /**
     * 根据上传的 CSV 模板文件批量导入学生或教师账号
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @param request       文件上传请求参数
     * @return 批量导入结果响应
     */
    BaseResponse<String> uploadFile(MultipartFile multipartFile, UploadFileRequest request);

    /**
     * 根据上传的 CSV 模板文件批量导入毕业设计选题
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @return 批量导入结果响应
     */
    BaseResponse<String> uploadFileTopic(MultipartFile multipartFile);

    /// 统计报表导出数据查询服务契约 ///

    /**
     * 查询当前登录用户权限范围内已选题学生的 CSV 导出行数据
     *
     * @return 已选题学生 CSV 行数据列表
     */
    List<List<String>> listSelectedStudentTopicCsvRows();

    /**
     * 查询当前登录用户权限范围内未选题学生的用户列表以供 CSV 导出
     *
     * @return 未选题学生实体列表
     */
    List<User> listUnselectedStudentCsvUsers();

    /**
     * 查询系统内所有有效账号的导出数据行
     *
     * @return 所有账号导出字段映射列表
     */
    List<Map<String, Object>> exportUserListRows();

    /**
     * 查询系统内所有有效题目的导出数据行
     *
     * @return 所有题目导出字段映射列表
     */
    List<Map<String, Object>> exportTopicListRows();

    /**
     * 查询系统内所有剩余可选题目的导出数据行
     *
     * @return 剩余题目导出字段映射列表
     */
    List<Map<String, Object>> exportSurplusTopicListRows();

    /**
     * 查询系统内所有已选学生的详细选题导出数据行
     *
     * @return 已选学生详细导出字段映射列表
     */
    List<Map<String, Object>> exportSelectedStudentRows();

    /**
     * 查询系统内所有未选学生的详细导出数据行
     *
     * @return 未选学生详细导出字段映射列表
     */
    List<Map<String, Object>> exportUnselectedStudentRows();

}
