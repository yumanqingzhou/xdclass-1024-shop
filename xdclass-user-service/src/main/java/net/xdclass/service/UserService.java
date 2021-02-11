package net.xdclass.service;

import net.xdclass.request.UserRegisterRequest;
import net.xdclass.utils.JsonData;

public interface UserService {
    JsonData register(UserRegisterRequest registerRequest);
}
