package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.AddressStatusEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.AddressMapper;
import net.xdclass.model.AddressDO;
import net.xdclass.model.LoginUser;
import net.xdclass.request.AddressAddReqeust;
import net.xdclass.service.AddressService;
import net.xdclass.vo.AddressVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressMapper addressMapper;

    /**
     * 查询地址详情
     * @param id
     * @return
     */
    @Override
    public AddressVO detail(Long id) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Long userId = loginUser.getId();
        AddressDO addressDO = addressMapper.selectOne(new QueryWrapper<AddressDO>().eq("id", id).eq("user_id",userId));
        if (addressDO!=null){
            AddressVO addressVO=new AddressVO();
            BeanUtils.copyProperties(addressDO,addressVO);
            return addressVO;
        }
        return null;
    }

    /**
     * 新增收货地址
     *
     * @param addressAddReqeust
     */
    @Override
    public void add(AddressAddReqeust addressAddReqeust) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Long id = loginUser.getId();
        AddressDO addressDO=new AddressDO();
        BeanUtils.copyProperties(addressAddReqeust,addressDO);
        addressDO.setCreateTime(new Date());
        addressDO.setUserId(id);

        if (addressAddReqeust.getDefaultStatus()== AddressStatusEnum.DEFAULT_STATUS.getCode()){
            //查找是否有默认收货地址 假如用户有默认收货地址 则把旧的默认地址修改  换成新的
            AddressDO defaultAddressDO = addressMapper.selectOne(new QueryWrapper<AddressDO>().eq("user_id", id)
                    .eq("default_status", AddressStatusEnum.DEFAULT_STATUS.getCode()));
            if (defaultAddressDO!=null){
                //让之前的默认收货地址失效
                defaultAddressDO.setDefaultStatus(AddressStatusEnum.COMMON_STATUS.getCode());
                addressMapper.updateById(defaultAddressDO);
            }
        }else {
            //保存
            int insert = addressMapper.insert(addressDO);
            log.info("新增收货地址 addressDO={}",addressDO);

        }
    }

    /**
     * 根据地址ID删除指定收货地址
     * @param addressId
     * @return
     */
    @Override
    public Integer delete(Long addressId) {
       if (addressId==null){
           throw new BizException(0000,"地址参数非法");
       }
        //删除当前登录用户地址 防止水平越权攻击
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        QueryWrapper<AddressDO> wrapper = new QueryWrapper<AddressDO>().eq("id", addressId)
                .eq("user_id", loginUser.getId());
        Integer i = addressMapper.deleteById(wrapper);
        return i;
    }

    /**
     * 查询当前用户所有收货地址
     * @return
     */
    @Override
    public List<AddressVO> listUserAllAddress() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Long id = loginUser.getId();
        QueryWrapper<AddressDO> wrapper = new QueryWrapper<AddressDO>().eq("user_id", id);
        List<AddressDO> addressDOS = addressMapper.selectList(wrapper);
        if (addressDOS!=null){
            //map是一个映射概念
            List<AddressVO> addressVOS = addressDOS.stream().map(obj -> {
                AddressVO addressVO = new AddressVO();
                BeanUtils.copyProperties(obj, addressVO);
                return addressVO;
            }).collect(Collectors.toList());
            return addressVOS;
        }

        return null;
    }


}
