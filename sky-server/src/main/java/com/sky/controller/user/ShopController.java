package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController") // ⚠️注意：指定Bean名称
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

	public static final String KEY = "SHOP_STATUS";

	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * 获取店铺的营业状态
	 * @return
	 */
	@GetMapping("/status")
	@ApiOperation("获取店铺的营业状态")
	public Result<Integer> getStatus() {
		// 从 Redis 中获取状态
		Integer status = (Integer) redisTemplate.opsForValue().get(KEY);

		// 如果 Redis 为空，默认返回 0 (打烊)，防止前端报错
		if (status == null) {
			status = 0;
		}

		log.info("用户端获取店铺营业状态为：{}", status);
		return Result.success(status);
	}
}
