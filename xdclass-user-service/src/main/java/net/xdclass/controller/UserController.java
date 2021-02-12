package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.service.FileService;
import net.xdclass.service.UserService;
import net.xdclass.utils.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author jyt
 * @since 2021-01-30
 */
@Api(tags = "用户模块")
@RestController
@RequestMapping("/userDO")
public class UserController {
    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;

    @ApiOperation("用户头像上传")
    @PostMapping("upload")
    public JsonData uploadUserImg(@ApiParam(value = "文件上传",required = true)@RequestPart("file") MultipartFile file){
        String result = fileService.uploadUserImg(file);
        if (result!=null){
           return JsonData.buildSuccess(result);
        }
        return JsonData.buildResult(BizCodeEnum.FILE_UPLOAD_USER_IMG_FAIL);
    }

    @ApiOperation("用户注册")
    @PostMapping("register")
    public JsonData register(@ApiParam("用户注册上传对象")@RequestBody UserRegisterRequest registerRequest){
        JsonData register = userService.register(registerRequest);
        return register;
    }

    @ApiOperation("用户登录")
    @PostMapping("login")
    public JsonData login(@ApiParam("用户登录对象")@RequestBody UserLoginRequest userLoginRequest){
        JsonData jsonData=userService.login(userLoginRequest);
        return jsonData;

    }


}

