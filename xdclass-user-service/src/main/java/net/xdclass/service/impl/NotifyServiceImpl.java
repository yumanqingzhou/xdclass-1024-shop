package net.xdclass.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.component.MailService;
import net.xdclass.constant.CacheKey;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.service.NotifyService;
import net.xdclass.utils.CheckUtil;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JsonData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {
    @Autowired
    private MailService mailService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 验证码的内容
     */
    private static final String CONTENT= "您的验证码是%s,有效时间是60秒,打死也不要告诉任何人";
    /**
     * 验证码的标题
     */
    private static final String SUBJECT= "注册验证码";

    /**
     * 验证码10分钟有效
     */
    private static final int CODE_EXPIRED = 60 * 1000 * 10;

    /**
     * 发送邮件验证码
     * @param sendCodeType 该验证码是那种类型的验证码 比如用户注册 还是支付验证等
     * @param to 收件人
     * @return
     */
    @Override
    public JsonData sendMail(SendCodeEnum sendCodeType, String to) {
        //格式化好缓存验证码的键
        String cacheKey=String.format(CacheKey.CHECK_CODE_KEY,sendCodeType,to);
        //该验证码是否被发送过
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        //如果验证码不为空
        if (StringUtils.isNotBlank(cacheValue)){
          //判断是否发送时间小于60秒
            long ttl=Long.parseLong(cacheValue.split("_")[1]);
            //当前系统时间
            long currentTimestamp = CommonUtil.getCurrentTimestamp();
            if (currentTimestamp-ttl<=1000*60){
                log.info("重复发送验证码,时间间隔:{} 秒",(CommonUtil.getCurrentTimestamp()-ttl)/1000);
                return JsonData.buildResult(BizCodeEnum.CODE_LIMITED);
            }
        }
        //生成验证码
        String randomCode = CommonUtil.getRandomCode(4);
        log.info("邮箱验证码 code={}",randomCode);
        //生成时间戳 防刷
        long currentTimestamp = CommonUtil.getCurrentTimestamp();
        String cacheCode=randomCode+"_"+currentTimestamp;
        redisTemplate.opsForValue().set(cacheKey,cacheCode,CODE_EXPIRED,TimeUnit.MILLISECONDS);

        //获取当前系统时间
        if (CheckUtil.isEmail(to)){
            //邮箱验证
            mailService.sendMail(to,SUBJECT,String.format(CONTENT,randomCode));
            return JsonData.buildSuccess();
        }else if (CheckUtil.isPhone(to)){
            //手机验证
            return JsonData.buildSuccess();
        }
        return JsonData.buildResult(BizCodeEnum.CODE_ERROR);
    }

    /**
     * 注册校验验证码
     * @param sendCodeType
     * @param mail
     * @param code
     * @return
     */
    @Override
    public Boolean checkCode(SendCodeEnum sendCodeType, String mail, String code) {
        String cacheKey=String.format(CacheKey.CHECK_CODE_KEY,sendCodeType.name(),mail);
        String cacheCode = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cacheCode)){
            //比较验证码
            String codeRedis = cacheCode.split("_")[0];
            if (codeRedis.equalsIgnoreCase(code)){
                //删除验证码
                redisTemplate.delete(cacheKey);
                return true;
            }
        }
        return false;
    }


}
