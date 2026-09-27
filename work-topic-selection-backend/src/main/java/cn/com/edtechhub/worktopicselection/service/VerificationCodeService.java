package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.vo.EmailVerificationVO;

public interface VerificationCodeService {
    String sendPasswordResetCode(String account, String clientIp);
    String sendEmailCode(String email, String clientIp);
    EmailVerificationVO verifyEmailCode(String email, String code);
    boolean consumeEmailProof(String email, String proofToken);
    long consumePasswordResetCode(String account, String resetCode);
    String normalizeEmail(String email);
}
