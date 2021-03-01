package net.xdclass.interceptor;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.model.LoginUser;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JWTUtil;
import net.xdclass.utils.JsonData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    public static ThreadLocal<LoginUser> threadLocal = new ThreadLocal();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accessToken = request.getHeader("token");
        if (accessToken == null) {
            accessToken = request.getParameter("token");
            System.out.println(accessToken);
        }
        if (StringUtils.isNotBlank(accessToken)) {
            //不为空解密
            Claims claims = JWTUtil.checkJWT(accessToken);
            if (claims != null) {
                //能获取到对象
                Long id = Long.valueOf(claims.get("id").toString());
                log.info("userID={}", id);
                String headImg = (String) claims.get("head_img");
                String name = (String) claims.get("name");
                String mail = (String) claims.get("mail");
                LoginUser loginUser = new LoginUser();
                loginUser.setName(name);
                loginUser.setId(id);
                loginUser.setMail(mail);
                loginUser.setHeadImg(headImg);
                threadLocal.set(loginUser);
                return true;
            } else {
                //未登录
                CommonUtil.sendJsonMsg(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNREGISTER));
                return false;
            }
        }

        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
