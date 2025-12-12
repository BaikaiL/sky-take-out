package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.Result;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

	@Autowired
	private DishFlavorService dishFlavorService; // 注入口味的 Service

	@Override
	@Transactional
	public Result saveWithFlavor(DishDTO dishDTO) {
		Dish dish = new Dish();
		BeanUtils.copyProperties(dishDTO, dish);
		// 保存一条菜品数据
		save(dish);

		// 保存多条口味数据
		List<DishFlavor> flavors = dishDTO.getFlavors();
		Long dishId = dish.getId();
		if (flavors != null && !flavors.isEmpty()) {
			// 遍历集合，给每个口味赋予 dishId
			flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));

			//  直接调用 Service 的批量保存方法
			dishFlavorService.saveBatch(flavors);
		}
		return Result.success();
	}
}
