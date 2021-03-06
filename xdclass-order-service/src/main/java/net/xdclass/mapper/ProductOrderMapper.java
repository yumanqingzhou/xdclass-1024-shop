package net.xdclass.mapper;

import net.xdclass.model.ProductOrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 二当家小D
 * @since 2021-03-01
 */
@Repository
public interface ProductOrderMapper extends BaseMapper<ProductOrderDO> {

    void updateOrderPayState(@Param("outTradeNo") String outTradeNo, @Param("newStatus") String newStatus, @Param("oldstatus") String oldstatus);
}
