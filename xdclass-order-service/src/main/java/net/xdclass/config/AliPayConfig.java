package net.xdclass.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;


@Slf4j
public class AliPayConfig {

    /*
     * 支付宝网关地址  TODO
     */
    public static final  String PAY_GATEWAY="https://openapi.alipaydev.com/gateway.do";
    /*
     * 支付宝 APPID TODO
     */
    public static final  String APPID="2021000118684111";
    /*
    应用私钥
     */
    public static  final String APP_PRI_KEY="MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCSeI0aAxlzyNC8pAPavVgctoYuaQQpIX0H6yeGV+loBNY50lQ8K4m8vDgmrCG0aQO7pBhXwIlVVcX9Su+i+PGenarr10HfMkIeGTrXC+8rh6HbyJv0gZDegh9fD5ZqxcB/M+9CpyY9/k6ES/LdnQCYueJsmdpKmwmJDS/BMwHGOUdaCN4Ehrnb//srM4xILFKm2EWWazdd4DxGUNrBYo7jfYJb0+Ibg+flKq7YD0qF8LsQpyohWmazRKnZz/9uAiE7HeR7IDQhcYdcIwiwHpQxjKmP6cF+dg/ScLRONUQ71CcWKkmROBJUgjViB88o0lRDvlu2Gqt0rUZXrQ9TCxBXAgMBAAECggEAJfb/Q93HzB4iFLNCmID5hL++uerYfDzE5reKECVbR23xhR1CXjI+yl/TAhsdxWBu6kUXVi9+qVLJNeUlhg2WuzFM60JpPYwOXTTW7oSWYqcOSiSOX+rxfOsnbIFi0JwfUBlLC/C9VdZDzcDN26llTTDpQpMCZNIYv6BeX2p0LEep44O6gKEGkisk6r2atO5iUe2wkNIbdypKuUAgCKsZDwrdC5Eaut6hwisCKHSV8cQHYkKvTLS9cigghzdcQTyfRaN6X8PeXaAhbBUX7SEIBsTh03cYLRWNcZgJd55QCEz2Tg58HCMD0tjACy2ey6fVQwGAbRl2IhSENaldZS+GwQKBgQDOYlpv6RJlmvOzTDCXNDiSS6yVwQ3C/NgzL63Sj9ynyTGN0WghPTR1xHCq9gkGc26NRRDDWPfLXERzVpwYZRZdnhE8v7bfjHOwvko/UAok7al7DvIswXmidh0Qi876dLX6mRF1/4H1MS8IFaGSCGbngCxf0SxF3qYHwJKH/uhiYQKBgQC1rumUuib8LhgBEod6rv6ZJ0iGKAAuDoKDjpWCdljr/5u4pQhrle/j7VM+3HuB1yG59dBHPBCICUobi+ZnVbTLfchALhfX0nx1bLmLsGPtqceInoVxj7QoK6Rqc7h42SwW8+4KCgQeWH/gk40yLnDOZF/afUOWA5R24BSmfF/dtwKBgB63cLJbRAuvRjoXStwWP502ehdHxl6Q9zSXEg3Wr4ahYSi0VAUucNJeTE6Fk85L+9Y2w0nvweGYd++aolzXHr8MOZCLEBKxer6Bc9d8eCYK4UCZdt3GZe4SGj1OMZ4BJNyJT5n6T0NI8LEJVyS+72HhJ7mTDj3P53Ib7TMkcz3BAoGAEIRUS+cYgDiYihBrLKYYE0hU+Y1NZuJ+zwL5aKDluJ7GQ2UNfOpmoqwCs2ZL/CPYtxU32CIHxrZBfHudeKt8AjvvJpaKKF7EXdCClcZ1bzfOIgd3TjmoQTamfd8DWEk2DugiLdt3QGL/TSVc4sECFGFfrXdR26N7rcr1VaVc+cUCgYBUlfi7Z1pWTQ9S0tchy49HsVwuYkOGl1vxgKPrQQYPn0eCeWf+WFMvWnFcDZVnhCSNE+PAoA8L3NasxFD44hCqRYLZyQl+rAasr0VGbVKIe52wW0Unk051SZRpc5Jt7MU2LGnfYz5EAr6DG9DtFzloXZI/3r5CQpf51J8Q9SFv9Q==";

    /*
    支付宝公钥
     */
    public static  final String ALIPAY_PUB_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvQmP3l4YNJltOLn2SJ3yP3+NfQtnozbI4cH3w3Q9jsD/7KdHTHUCXfU0TkXbzcO5BpXSjo3IlXKzYNfhC538sV8q6sVanqMaXmyrv5kWL4mwHykdmusXnQvQfpOegLWewiIHH79SCX/VMWnTtKxY4imPhWYRhWLUNOXRPBAhN/3Xg/hALAhPHzYYvUUJEfEALvmh/hNfCKH1pdLxsNIWYtLzJSyr6G59NzjD17gALzjUVdloHLHj2wraC80uxUAZThoG4Ds6ofdT244mjZH9jm3/k5c8l/HbpwOM0u8+/8JGqPU8/8uLChC91rsEg8gy/66KiYZw5gRo0LiyHbLe6QIDAQAB";
    /*
     * 签名类型
     */
    public static final  String SIGN_TYPE="RSA2";
    /*
     * 字符编码
     */
    public static final  String CHARSET="UTF-8";

    /*
     * 返回参数格式
     */
    public static final  String FORMAT="json";

    private volatile  static AlipayClient instance=null;

    /*
    单例模式客户端 构造方式私有化
     */
    private AliPayConfig(){

    }

    public static AlipayClient getInstance(){
        if (instance==null){
            synchronized (AliPayConfig.class){
                if (instance==null){
                    instance= new DefaultAlipayClient(PAY_GATEWAY,APPID,APP_PRI_KEY,FORMAT,CHARSET,ALIPAY_PUB_KEY,SIGN_TYPE);
                }
            }
        }
        return instance;
    }

}
