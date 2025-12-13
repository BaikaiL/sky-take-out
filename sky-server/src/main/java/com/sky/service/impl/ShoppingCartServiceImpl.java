package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

	@Autowired
	private DishService dishService;
	@Autowired
	private SetmealService setmealService;

	/**
	 * 添加购物车商品
	 * @param shoppingCartDTO
	 * @return
	 */
	@Override
	public Result add(ShoppingCartDTO shoppingCartDTO) {

		ShoppingCart shoppingCart = new ShoppingCart();
		BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
		Long userId = BaseContext.getCurrentId();
		shoppingCart.setUserId(userId);

		// 查询当前用户是否已经添加过此菜品或者套餐
		ShoppingCart cart = lambdaQuery().eq(ShoppingCart::getUserId, shoppingCart.getUserId())
				.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId())
				.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId())
				.eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor())
				.one();

		// 如果已经存在，则数量加1
		if (cart != null) {
			cart.setNumber(cart.getNumber() + 1);
			updateById(cart);
		}
		// 如果不存在，则添加到购物车，数量默认为1
		else {
			// 判断添加的是菜品还是套餐
			if (shoppingCartDTO.getDishId() != null) {
				Dish dish = dishService.getById(shoppingCartDTO.getDishId());
				shoppingCart.setName(dish.getName());
				shoppingCart.setImage(dish.getImage());
				shoppingCart.setAmount(dish.getPrice());

			} else {
				Setmeal setmeal = setmealService.getById(shoppingCartDTO.getSetmealId());
				shoppingCart.setName(setmeal.getName());
				shoppingCart.setImage(setmeal.getImage());
				shoppingCart.setAmount(setmeal.getPrice());
			}

			shoppingCart.setNumber(1);
			shoppingCart.setUserId(userId);
			save(shoppingCart);
		}
		return Result.success();
	}

	@Override
	public Result<List<ShoppingCart>> listShoppingCart() {
		Long userId = BaseContext.getCurrentId();
		List<ShoppingCart> list = query().eq("user_id", userId).list();
		return Result.success(list);
	}

	@Override
	public Result clean() {
		// 1. 创建条件构造器
		LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
		// 2. 设置条件：user_id 等于 userId
		queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

		// 3. 执行删除
		remove(queryWrapper);
		return Result.success();
	}
}
