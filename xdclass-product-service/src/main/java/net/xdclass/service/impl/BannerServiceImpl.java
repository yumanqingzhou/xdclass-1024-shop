package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.xdclass.model.BannerDO;
import net.xdclass.mapper.BannerMapper;
import net.xdclass.service.BannerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.vo.BannerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
@Service
public class BannerServiceImpl implements BannerService {
    @Autowired
    private BannerMapper bannerMapper;

    /**
     * 轮播图列表
     * @return
     */
    @Override
    public List<BannerVO> list() {
        List<BannerDO> bannerDOS = bannerMapper.selectList(new QueryWrapper<BannerDO>().orderByAsc("weight"));
        List<BannerVO> bannerVOs = bannerDOS.stream().map(obj -> {
            BannerVO bannerVO = new BannerVO();
            BeanUtils.copyProperties(obj, bannerVO);
            return bannerVO;
        }).collect(Collectors.toList());
        return bannerVOs;
    }
}
