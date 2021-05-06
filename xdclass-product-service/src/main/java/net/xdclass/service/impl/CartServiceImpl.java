package net.xdclass.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.api.R;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.constant.CacheKey;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.exception.BizException;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.ProductMapper;
import net.xdclass.model.LoginUser;
import net.xdclass.model.ProductDO;
import net.xdclass.request.CartItemRequest;
import net.xdclass.service.CartService;
import net.xdclass.service.ProductService;
import net.xdclass.vo.CartItemVO;
import net.xdclass.vo.CartVO;
import net.xdclass.vo.ProductVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import springfox.documentation.spring.web.json.Json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductService productService;

    /**
     * 添加商品到购物车
     *
     * @param cartItemRequest
     */
    @Override
    public void addCart(CartItemRequest cartItemRequest) {
        long productId = cartItemRequest.getProductId();
        int buyNum = cartItemRequest.getBuyNum();
        //获取到我的购物车
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        Object object = myCartOps.get(productId);
        String result = "";
        result = (String) object;
        if (StringUtils.isBlank(result)) {
            //空的则为新增
            ProductDO productDO = productMapper.selectById(productId);
            if (productDO != null) {
                //有该商品则构建
                CartItemVO cartItemVO = new CartItemVO();
                cartItemVO.setAmount(productDO.getAmount());
                cartItemVO.setProductId(productDO.getId());
                cartItemVO.setProductImg(productDO.getCoverImg());
                cartItemVO.setProductTitle(productDO.getTitle());
                cartItemVO.setBuyNum(buyNum);
                String cartItemVOStr = JSON.toJSONString(cartItemVO);
                //存入缓存
                myCartOps.put(productDO.getId(), cartItemVOStr);

            } else {
                //没有该商品信息
                throw new BizException(BizCodeEnum.CART_FAIL);
            }

        } else {
            //修改数量
            CartItemVO cartItemVO = JSON.parseObject(result, CartItemVO.class);
            cartItemVO.setBuyNum(cartItemVO.getBuyNum() + buyNum);
            myCartOps.put(cartItemVO.getProductId(), JSON.toJSONString(cartItemVO));
        }
    }

    /**
     * 清空我的购物车
     */
    @Override
    public void clear() {
        String cartKey = getCartKey();
        //删除哈希结构的键 对应后续值也会删掉
        redisTemplate.delete(cartKey);
    }

    /**
     * 查看我的购物车
     * 1.获取购物项的时候 查看最新价格
     *
     * @return
     */
    @Override
    public CartVO findMyCart() {
        List<CartItemVO> cartItemVOS = buildCartItem(false);
        CartVO cartVO = new CartVO();
        cartVO.setCartItems(cartItemVOS);
        return cartVO;
    }

    /**
     * 删除某个购物项
     *
     * @param productId
     */
    @Override
    public void deleteItem(long productId) {
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        myCartOps.delete(productId);
    }

    /**
     * 修改购物项数量
     *
     * @param cartItemRequest
     */
    @Override
    public void changeItemNum(CartItemRequest cartItemRequest) {
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        String obj = (String) myCartOps.get(cartItemRequest.getProductId());
        if (obj == null) {
            throw new BizException(BizCodeEnum.CART_FAIL);
        }
        CartItemVO cartItemVO = JSON.parseObject(obj, CartItemVO.class);
        cartItemVO.setBuyNum(cartItemRequest.getBuyNum());
        myCartOps.put(cartItemRequest.getProductId(), JSON.toJSONString(cartItemVO));
    }

    /**
     * 确认订单购物项
     * 1.获取选中的购物车购物项最新价格和详细信息
     * 2.删除购物车中选中的购物项
     *
     * @param productIds
     * @return
     */
    @Override
    public List<CartItemVO> confirmOrderCartItems(List<Long> productIds) {
        //获取最新的价格
        List<CartItemVO> cartItemVOS = this.buildCartItem(true);
        //根据需要的商品id进行过滤，并清空对应的购物项
        List<CartItemVO> cartItemVOList = cartItemVOS.stream().filter(obj -> {
            if (productIds.contains(obj.getProductId())) {
                //删除购物项
                this.deleteItem(obj.getProductId());
                return true;
            } else {
                return false;
            }
        }).collect(Collectors.toList());
        return cartItemVOList;
    }

    /**
     * 构建购物项
     *
     * @param latesPrice 是否查看最新价格
     * @return
     */
    public List<CartItemVO> buildCartItem(boolean latesPrice) {
        BoundHashOperations<String, Object, Object> myCartOps = getMyCartOps();
        //获取全部的购物项 就是<String <String,values>>的values
        List<Object> itemList = myCartOps.values();
        //购物项集合
        List<CartItemVO> cartItemVOS = new ArrayList<>();
        //构建所有购物项的ID集合 方便批量查询最新价格
        List<Long> productIDList = new ArrayList<>();
        for (Object item : itemList) {
            CartItemVO cartItemVO = JSON.parseObject((String) item, CartItemVO.class);
            cartItemVOS.add(cartItemVO);
            productIDList.add(cartItemVO.getProductId());
        }

//        itemList.stream().map(item->{
//            CartItemVO cartItemVO = JSON.parseObject((String) item, CartItemVO.class);
//            return cartItemVO;
//        }).collect(Collectors.toList());

        //判断是否查询最新价格
        if (latesPrice) {
            this.setProductLatesPrice(cartItemVOS, productIDList);
        }

        return cartItemVOS;
    }

    /**
     * 查询商品最新价格
     *
     * @param cartItemVOS
     * @param productIDList
     */
    public void setProductLatesPrice(List<CartItemVO> cartItemVOS, List<Long> productIDList) {
        List<ProductVO> productVOS = productService.findProductByIdBatch(productIDList);
        //将productVOS 转成 一个map 键为商品的ID 用ID作为分组 Function.identity()标识商品ID为 唯一标识符
        // 用购物项集合和查询出的商品集合对比 如果id在商品集合里有 就设置最新的价格
        Map<Long, ProductVO> map = productVOS.stream().collect(Collectors.toMap(ProductVO::getId, Function.identity()));
        //遍历购物项集合设置新价格
        cartItemVOS.stream().forEach(item -> {
            ProductVO productVO = map.get(item.getProductId());
            item.setProductTitle(productVO.getTitle());
            item.setProductImg(productVO.getCoverImg());
            item.setAmount(productVO.getAmount());//价格
        });
    }

    /**
     * 获取Redis缓存哈希结构key 就是用户ID
     * hash结构如下
     * 用户ID--{商品ID,value}{商品ID2,value2}...
     *
     * @return
     */
    private String getCartKey() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        Long userId = loginUser.getId();
        String cartHashKey = String.format(CacheKey.CART_KEY, userId);
        return cartHashKey;
    }

    /**
     * 获取我的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getMyCartOps() {
        String cartKey = getCartKey();
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(cartKey);
        return ops;
    }
}
