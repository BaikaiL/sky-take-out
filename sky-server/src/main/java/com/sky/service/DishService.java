package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.result.Result;

public interface DishService extends IService<Dish> {
	Result saveWithFlavor(DishDTO dishDTO);
}
