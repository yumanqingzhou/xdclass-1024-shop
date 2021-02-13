package net.xdclass.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.model.AddressDO;
import net.xdclass.request.AddressAddReqeust;
import net.xdclass.service.AddressService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.AddressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public JsonData detail(@ApiParam(value = "地址ID",required = true)
                            @PathVariable("address_id") Long addressId){
        AddressVO detail = addressService.detail(addressId);
        if (detail!=null){
            return JsonData.buildSuccess(detail);
        }else {
            return JsonData.buildResult(BizCodeEnum.ADDRESS_NO_EXITS);
        }
    }

    @ApiOperation("新增收货地址")
    @PostMapping("add")
    public JsonData add(@ApiParam(value = "收货地址对象") @RequestBody AddressAddReqeust addressAddReqeust){
        addressService.add(addressAddReqeust);
        return JsonData.buildSuccess();
    }

    @ApiOperation("删除指定收货地址")
    @GetMapping("/del/{address_id}")
    public JsonData delete(@ApiParam(value = "地址ID",required = true)
                           @PathVariable("address_id") Long addressId){
        Integer rows = addressService.delete(addressId);
        if (rows!=null){
            return JsonData.buildSuccess();
        }else {
            return JsonData.buildResult(BizCodeEnum.ADDRESS_DEL_FAIL);
        }
    }

    @ApiOperation("查询当前登录用户所有收货地址")
    @GetMapping("/list")
    public JsonData findAlladdress(){
        List<AddressVO> addressVOS = addressService.listUserAllAddress();
        return JsonData.buildSuccess(addressVOS);
    }
}

