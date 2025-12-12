package com.sky.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

	@Autowired
	private DishService dishService;

	/**
	 * 新增菜品
	 * @param dishDTO
	 * @return
	 */
	@PostMapping
	public Result saveWithFlavor(@RequestBody DishDTO dishDTO){
		log.info("新增菜品：{}", dishDTO);
		return dishService.saveWithFlavor(dishDTO);
	}

	/**
	 * 菜品分页查询
	 * @param dishPageQueryDTO
	 * @return
	 */
	@GetMapping("/page")
	public Result<Page<DishVO>> pageQuery( DishPageQueryDTO dishPageQueryDTO){
		log.info("菜品分页查询：{}", dishPageQueryDTO);
		return dishService.pageQuery(dishPageQueryDTO);
	}

	/**
	 * 删除菜品
	 * @param ids
	 * @return
	 */
	@DeleteMapping
	public Result deleteDish(@RequestParam List<Long> ids){
		log.info("删除菜品：{}", ids);
		return dishService.deleteDish(ids);
	}

	@GetMapping("/{id}")
	public Result<DishVO> getDishById(@PathVariable Long id){
		log.info("根据id查询菜品和口味: {}", id);
		return dishService.getDishById(id);
	}

	@PutMapping
	public Result updateDish(@RequestBody DishDTO dishDTO){
		log.info("更新菜品信息：{}",dishDTO);
		return dishService.updateDish(dishDTO);
	}
}
