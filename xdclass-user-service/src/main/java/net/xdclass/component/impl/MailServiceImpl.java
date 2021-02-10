package net.xdclass.component.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MailServiceImpl implements MailService {
    /**
     * Spring Boot 提供了一个发送邮件的简单抽象，直接注入即可使用
     */
    @Autowired
    private JavaMailSender javaMailSender;
    /**
     * 发送邮件功能组件
     * @param to 发给谁
     * @param subject 主题
     * @param content 内容
     */
    @Value("${spring.mail.from}")
    private String from;

    @Override
    public void sendMail(String to, String subject, String content) {
        //创建信息模板对象
        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        //收件人
        simpleMailMessage.setTo(to);
        //发件人
        simpleMailMessage.setFrom(from);
        //内容
        simpleMailMessage.setText(content);
        //主题
        simpleMailMessage.setSubject(subject);
        //发送
        javaMailSender.send(simpleMailMessage);
        log.info("邮件发成功:{}",simpleMailMessage.toString());
    }
}
