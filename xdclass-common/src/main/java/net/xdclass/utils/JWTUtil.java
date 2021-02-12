package net.xdclass.utils;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.LoginUser;

import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
public class JWTUtil {

    /*
        jwt过期时间 方便测试70天 正常7天 毫秒单位
     */
    public static  final long EXPIRE=1000 * 60 * 60 * 24 *7 *10;

    /*
        加密秘钥
     */
    public static  final String SECRET="xdclass.net1024";

     /*
        token前缀
     */
     public static  final String TOKEN_PREFIX="xdclass1024shop";

    /**
     * 令牌颁发人
     */
    private static final String SUBJECT = "xdclass";

    /**
     * 生成JWT
     * @param loginUser
     * @return
     */
    public static String geneJsonWebToken(LoginUser loginUser){
        if (loginUser != null) {
            Long id = loginUser.getId();
            String token = Jwts.builder().setSubject(SUBJECT)
                    //构建对象内容体
                    .claim("name", loginUser.getName())
                    .claim("head_img", loginUser.getHeadImg())
                    .claim("id", loginUser.getId())
                    .claim("mail", loginUser.getMail())
                    //设置生成时间
                    .setIssuedAt(new Date())
                    //设置过期时间
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                    //设置加密算法和秘钥
                    .signWith(SignatureAlgorithm.HS256, SECRET).compact();
            return TOKEN_PREFIX+token;
        }else {
            throw new NullPointerException("登录对象为空");
        }

    }

    /**
     * 校验token
     * @param token
     * @return
     */
    public static Claims checkJWT(String token){

        try {
            //解密JWT
            Claims body = Jwts.parser().setSigningKey(SECRET)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
            return body;
        } catch (Exception e) {
            log.info("JWT解密失败");
            e.printStackTrace();
            return null;
        }
    }
}
