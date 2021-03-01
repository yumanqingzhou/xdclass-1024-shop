package net.xdclass.controller;


import io.swagger.annotations.ApiOperation;
import net.xdclass.service.BannerService;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.BannerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 二当家小D
 * @since 2021-02-26
 */
@RestController
@RequestMapping("/api/banner/v1")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @ApiOperation("商品轮播图列表")
    @GetMapping("banner")
    public JsonData list() {
        List<BannerVO> bannerVOList=bannerService.list();
        return JsonData.buildSuccess(bannerVOList);

    }
}

