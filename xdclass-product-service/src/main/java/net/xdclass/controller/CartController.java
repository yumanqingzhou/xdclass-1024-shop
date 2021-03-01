package net.xdclass.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.xdclass.request.CartItemRequest;
import net.xdclass.service.CartService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车模块
 */
@Api("购物车模块")
@RestController
@RequestMapping("/api/cart/v1")
public class CartController {
    @Autowired
    private CartService cartService;

    @ApiOperation("添加购物车")
    @PostMapping("add")
    public JsonData addCart(@RequestBody CartItemRequest cartItemRequest){
        System.out.println("=========");
        cartService.addCart(cartItemRequest);
        return JsonData.buildSuccess();
    }

    @ApiOperation("清空购物车")
    @PostMapping("/clear")
    public JsonData clearMyCart(){
        cartService.clear();
        return JsonData.buildSuccess();
    }

    @ApiOperation("查看我的购物车")
    @PostMapping("/myCart")
    public JsonData findMyCart(){
        CartVO cartVO=cartService.findMyCart();
        return JsonData.buildSuccess(cartVO);
    }


    @ApiOperation("删除购物车的某个购物项")
    @DeleteMapping("/deleteItem/{product_id}")
    public JsonData deleteItem(@PathVariable("product_id")long productId){
        cartService.deleteItem(productId);
        return JsonData.buildSuccess();
    }

    @ApiOperation("修改购物项数量")
    @PostMapping("/changeItem")
    public JsonData changeItem(@RequestBody CartItemRequest cartItemRequest){
        cartService.changeItemNum(cartItemRequest);
        return JsonData.buildSuccess();
    }
}
