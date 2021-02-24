package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.lang.Collections;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.model.CouponRecordDO;
import net.xdclass.mapper.CouponRecordMapper;
import net.xdclass.model.LoginUser;
import net.xdclass.service.CouponRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.vo.CouponRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-15
 */
@Service
public class CouponRecordServiceImpl extends ServiceImpl<CouponRecordMapper, CouponRecordDO> implements CouponRecordService {

    @Autowired
    private CouponRecordMapper couponRecordMapper;
    /**
     * 分页查询已领优惠券列表
     * @param page
     * @param size
     * @return
     */
    @Override
    public Map<String, Object> page(Integer page, Integer size) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        //封装分页信息
        Page<CouponRecordDO> pageInfo = new Page<>(page, size);
        Page<CouponRecordDO> myPage = couponRecordMapper.selectPage(pageInfo, new QueryWrapper<CouponRecordDO>()
                .eq("user_id", loginUser.getId())
                .orderByDesc("create_time"));
        Map<String,Object> pageMap=new HashMap<>();
        pageMap.put("total_record",myPage.getTotal());
        pageMap.put("total_page",myPage.getPages());
        pageMap.put("current_data",myPage.getRecords().stream().map(obj->beanProcess(obj)).collect(Collectors.toList()));
        return pageMap;
    }

    /**
     * 根据领取记录ID 查询已领取优惠券详情
     * @param recordId
     * @return
     */
    @Override
    public CouponRecordVO findById(Long recordId) {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();//判断是否当前用户 防止水平越权攻击
        QueryWrapper<CouponRecordDO> wrapper = new QueryWrapper<>();
        wrapper.eq("id",recordId).eq("user_id",loginUser.getId());
        CouponRecordDO couponRecordDO = couponRecordMapper.selectOne(wrapper);
        CouponRecordVO couponRecordVO = this.beanProcess(couponRecordDO);
        if (couponRecordVO==null){
            return null;
        }
        return couponRecordVO;
    }

    private CouponRecordVO beanProcess(CouponRecordDO couponRecordDO) {
        CouponRecordVO couponRecordVO =new CouponRecordVO();
        BeanUtils.copyProperties(couponRecordDO,couponRecordVO);
        return couponRecordVO;
    }


}
