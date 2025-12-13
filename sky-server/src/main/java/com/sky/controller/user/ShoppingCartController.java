package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShoppingCartController {

	@Autowired
	private ShoppingCartService shoppingCartService;

	@PostMapping("/add")
	public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
		log.info("添加购物车：{}", shoppingCartDTO);
		return shoppingCartService.add(shoppingCartDTO);
	}

	@GetMapping("/list")
	public Result<List<ShoppingCart>> listShoppingCart() {
		log.info("查询购物车");
		return shoppingCartService.listShoppingCart();
	}

	@DeleteMapping("/clean")
	public Result clean() {
		log.info("清空购物车");
		return shoppingCartService.clean();
	}
}
