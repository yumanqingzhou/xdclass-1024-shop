package net.xdclass.service;

import net.xdclass.model.BannerDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.vo.BannerVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
public interface BannerService{

    List<BannerVO> list();

}
