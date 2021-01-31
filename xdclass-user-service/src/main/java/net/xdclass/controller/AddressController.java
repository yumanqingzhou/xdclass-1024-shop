package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.model.AddressDO;
import net.xdclass.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 电商-公司收发货地址表 前端控制器
 * </p>
 *
 * @author jyt
 * @since 2021-01-30
 * /api/address/v1/ api toC端 v1代表第一版本
 */
@Api(tags = "用户收货地址模块")
@RestController
@RequestMapping("/api/address/v1/")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @ApiOperation("根据ID查找地址详情")
    @GetMapping("find/{address_id}")
    public AddressDO detail(@ApiParam(value = "地址ID",required = true)
                            @PathVariable("address_id") Long addressId){
        return addressService.detail(addressId);
    }
}

