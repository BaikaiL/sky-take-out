package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;

import java.util.List;

public interface ShoppingCartService extends IService<ShoppingCart> {
	Result add(ShoppingCartDTO shoppingCartDTO);

	Result<List<ShoppingCart>> listShoppingCart();

	Result clean();
}
