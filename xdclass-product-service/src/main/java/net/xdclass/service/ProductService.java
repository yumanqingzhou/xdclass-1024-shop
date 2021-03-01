package net.xdclass.service;

import net.xdclass.model.ProductDO;
import com.baomidou.mybatisplus.extension.service.IService;
import net.xdclass.vo.ProductVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
public interface ProductService {

    Map<String, Object> page(int page, int size);

    ProductVO findDetailById(Long productId);

    List<ProductVO> findProductByIdBatch(List<Long> productIDList);
}
