package net.xdclass.mapper;

import net.xdclass.model.ProductDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.xdclass.vo.ProductVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
@Repository
public interface ProductMapper extends BaseMapper<ProductDO> {

    List<ProductVO> findProductsByIdBatch(@Param("productIds") List<Long> productIds);

    int lockProductStock(@Param("productId") long productId, @Param("buyNum") int buyNum);

    void unlockProductStock(@Param("productId") Long productId, @Param("buyNum") Integer buyNum);
}
