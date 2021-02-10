package net.xdclass.service;

import net.xdclass.enums.SendCodeEnum;
import net.xdclass.utils.JsonData;

public interface NotifyService {
    JsonData sendMail(SendCodeEnum sendCodeType, String to);
}
