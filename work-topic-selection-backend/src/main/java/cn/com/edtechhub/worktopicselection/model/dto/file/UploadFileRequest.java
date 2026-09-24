package cn.com.edtechhub.worktopicselection.model.dto.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author wobushi041
 */
@Data
public class UploadFileRequest implements Serializable {

    /**
     * 上传文件
     */
    private MultipartFile file;

    /**
     * 用户角色状态
     */
    private Integer status;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}