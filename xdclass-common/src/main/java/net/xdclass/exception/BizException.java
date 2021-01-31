package net.xdclass.exception;

import lombok.Data;
import net.xdclass.enums.BizCodeEnum;
import org.springframework.stereotype.Component;

@Data
public class BizException extends RuntimeException{
    public BizException() {
    }

    private int code;
    private String msg;

    public BizException(int code,String msg){
        super(msg);
        this.code=code;
        this.msg=msg;
    }

    public BizException(BizCodeEnum bizCodeEnum){
        super(bizCodeEnum.getMessage());
        this.code=bizCodeEnum.getCode();
        this.msg=bizCodeEnum.getMessage();
    }
}
