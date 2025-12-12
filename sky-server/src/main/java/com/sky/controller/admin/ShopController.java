package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController") // ⚠️注意：指定Bean名称，防止与用户端冲突
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

	public static final String KEY = "SHOP_STATUS";

	@Autowired
	private RedisTemplate redisTemplate;

	/**
	 * 设置店铺的营业状态
	 * @param status 1为营业，0为打烊
	 * @return
	 */
	@PutMapping("/{status}")
	public Result setStatus(@PathVariable Integer status) {
		log.info("设置店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");

		// 将状态存入 Redis
		redisTemplate.opsForValue().set(KEY, status);

		return Result.success();
	}

	/**
	 * 获取店铺的营业状态
	 * @return
	 */
	@GetMapping("/status")
	public Result<Integer> getStatus() {
		// 从 Redis 中获取状态
		Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
		log.info("获取到店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");

		// 防止 Redis 中没有数据（第一次启动），默认为 0（打烊）或 1（营业），视需求而定
		if(status == null){
			status = 0;
		}



		return Result.success(status);
	}
}
