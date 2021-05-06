package net.xdclass.service;

import net.xdclass.enums.BizCodeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.model.ProductMessage;
import net.xdclass.model.ProductTaskDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.request.LockProductRequest;
import net.xdclass.utils.JsonData;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jyt
 * @since 2021-04-07
 */
public interface ProductTaskService {

    /**
     * 锁定商品库存
     *
     * @param productRequest
     * @return
     */
    JsonData lockProductStock(LockProductRequest productRequest);

    Boolean releaseProductStock(ProductMessage productMessage);
}
