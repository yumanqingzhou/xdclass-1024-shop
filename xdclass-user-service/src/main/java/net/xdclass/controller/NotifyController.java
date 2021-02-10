package net.xdclass.controller;

import com.baomidou.kaptcha.Kaptcha;
import com.google.code.kaptcha.Producer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.MailService;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.service.NotifyService;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@Api(tags = "通知模块")
@RestController
@RequestMapping("/api/user/v1")
@Slf4j
public class NotifyController {
    //生产验证码对象 Producer 是DefaultKaptcha 接口的实现类
    @Autowired
    private Producer captchaProducer;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private NotifyService notifyService;
    /**
     * 图形验证码有效期10分钟
     */
    private static final long CAPTCHA_CODE_EXPIRED = 60 * 1000 * 10;

    @ApiOperation("获取图形验证码")
    @GetMapping("captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        //根据配置生成一个验证码
        String captchaText = captchaProducer.createText();
        log.info("图形验证码:{}", captchaText);
        //加入Redis缓存
        redisTemplate.opsForValue().set(getCaptchaKey(request), captchaText, CAPTCHA_CODE_EXPIRED, TimeUnit.MILLISECONDS);
        //创建图形
        BufferedImage image = captchaProducer.createImage(captchaText);
        //向外输出
        ServletOutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            //图形流输出验证图片
            ImageIO.write(image, "jpg", outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            log.error("获取图形验证码异常:{}", e);
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送邮箱验证码
     * @param to 发给谁
     * @param captcha 图形验证码
     * @param request
     * @return
     */
    @ApiOperation("获取邮箱验证码")
    @GetMapping("send_code")
    public JsonData sendRegisterCode(@ApiParam("收信人") @RequestParam(value = "to", required = true) String to,
                                     @ApiParam("图形验证码") @RequestParam(value = "captcha", required = true) String captcha,
                                     HttpServletRequest request) {
        //=====================================校验验证码是否成功==========================================
        //1.1获取缓存验证码中的键
        String captchaKey = getCaptchaKey(request);
        //1.2取值对比
        String cacheCaptcha  = redisTemplate.opsForValue().get(captchaKey);
        if (captcha!=null&&cacheCaptcha!=null&&cacheCaptcha.equalsIgnoreCase(captcha)){
            //删除这个键
            redisTemplate.delete(captchaKey);
            //发送验证码
            return notifyService.sendMail(SendCodeEnum.USER_REGISTER,to);

        }
        return JsonData.buildResult(BizCodeEnum.CODE_CAPTCHA);
    }

    /**
     * 制作Redis缓存图形验证码的KEY
     *
     * @param request
     * @return
     */
    private String getCaptchaKey(HttpServletRequest request) {
        String ipAddr = CommonUtil.getIpAddr(request);
        String userAgent = request.getHeader("User-Agent");
        String key = "user-service:captcha:" + CommonUtil.MD5(ipAddr + userAgent);
        log.info("ip={}", ipAddr);
        log.info("userAgent={}", userAgent);
        log.info("key={}", key);
        return key;
    }
}
