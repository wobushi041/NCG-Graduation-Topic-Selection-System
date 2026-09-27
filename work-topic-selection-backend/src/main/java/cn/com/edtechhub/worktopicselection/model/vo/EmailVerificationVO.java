package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailVerificationVO {
    private String proofToken;
    private long expiresInSeconds;
}
