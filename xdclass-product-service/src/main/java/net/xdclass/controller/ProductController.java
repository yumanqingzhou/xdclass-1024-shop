package net.xdclass.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.xdclass.request.LockProductRequest;
import net.xdclass.service.ProductService;
import net.xdclass.service.ProductTaskService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
@ApiOperation("商品模块")
@RestController
@RequestMapping("/api/product/v1")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductTaskService productTaskService;

    @GetMapping("page")
    public JsonData pageProductList(@RequestParam(value = "page",defaultValue = "1")int page,
                                    @RequestParam(value = "size",defaultValue = "6")int size){

       Map<String,Object> result= productService.page(page,size);
       return JsonData.buildSuccess(result);
    }

    @GetMapping("detail/{product_id}")
    public JsonData pageProductList(@PathVariable("product_id")Long productId){
        ProductVO productVO=productService.findDetailById(productId);
        return JsonData.buildSuccess(productVO);
    }

    /**
     * 锁定商品
     * @param productRequest
     * @return
     */
    @PostMapping("lock_products")
    public JsonData lockProducts(@ApiParam("锁定商品对象集合")@RequestBody LockProductRequest productRequest){
       JsonData jsonData= productTaskService.lockProductStock(productRequest);
       return jsonData;
    }


}

