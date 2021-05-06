package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.xdclass.controller.ProductController;
import net.xdclass.model.ProductDO;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.xdclass.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ProductServiceImpl  implements ProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public Map<String, Object> page(int page, int size) {
        Page<ProductDO> pageInfo=new Page<>(page,size);
        Page<ProductDO> myPage = productMapper.selectPage(pageInfo,null);
        Map<String,Object> result=new HashMap<>();
        if (myPage.getRecords()!=null){
            List<ProductVO> collect = myPage.getRecords()
                    .stream().map(obj -> BeanProcess(obj)).collect(Collectors.toList());
            result.put("current_data",collect);
        }
        result.put("total_record", myPage.getTotal());
        result.put("total_page", myPage.getPages());

        return result;

    }

    /**
     * 根据Id找商品详情
     * @param productId
     * @return
     */
    @Override
    public ProductVO findDetailById(Long productId) {
        ProductDO productDO = productMapper.selectById(productId);
        ProductVO productVO = this.BeanProcess(productDO);
        return productVO;
    }

    /**
     * 根据ID批量查询商品
     * @param productIDList
     * @return
     */
    @Override
    public List<ProductVO> findProductByIdBatch(List<Long> productIDList) {
        List<ProductDO> productDOS = productMapper.selectBatchIds(productIDList);
        List<ProductVO> productVOList = productDOS.stream().map(obj -> BeanProcess(obj)).collect(Collectors.toList());
        return productVOList;
    }


    /**
     * 商品对象转换
     * @param productDO
     * @return
     */
    private ProductVO BeanProcess(ProductDO productDO) {
        ProductVO productVO=new ProductVO();
        BeanUtils.copyProperties(productDO,productVO);
        productVO.setStock( productDO.getStock() - productDO.getLockStock());
        return productVO;
    }


}
