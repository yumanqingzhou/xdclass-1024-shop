package net.xdclass.exception;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.JsonData;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;


@ControllerAdvice
@Slf4j
@RestController
public class CustomExceptionHandler {

    @ExceptionHandler(Exception.class)
    public JsonData handler(Exception e){
        if (!(e instanceof BizException)) {
            log.error("非业务异常 {}", e);
            return JsonData.buildError("未知错误");
        }else {
            BizException bizException=(BizException) e;
            log.error("业务异常 {}",((BizException) e).getMsg());
            return JsonData.buildCodeAndMsg(bizException.getCode(),bizException.getMsg());
        }
    }
}
