package com.sky.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService extends IService<Dish> {
	Result saveWithFlavor(DishDTO dishDTO);

	Result<Page<DishVO>> pageQuery(DishPageQueryDTO dishPageQueryDTO);

	Result deleteDish(List<Long> ids);
}
