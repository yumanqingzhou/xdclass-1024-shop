package net.xdclass.service;

import net.xdclass.request.AddressAddReqeust;
import net.xdclass.vo.AddressVO;

import java.util.List;

public interface AddressService {

    AddressVO detail(Long id);

    void add(AddressAddReqeust addressAddReqeust);

    Integer delete(Long addressId);

    List<AddressVO> listUserAllAddress();
}
