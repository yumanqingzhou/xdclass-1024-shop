package net.xdclass.component;

/**
 * 发送邮件
 */
public interface MailService {
    void sendMail(String to,String subject, String content);
}
