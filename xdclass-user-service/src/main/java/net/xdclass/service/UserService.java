package net.xdclass.service;

import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.UserVO;

public interface UserService {
    JsonData register(UserRegisterRequest registerRequest);

    JsonData login(UserLoginRequest userLoginRequest);

    UserVO findUserDetail();
}
