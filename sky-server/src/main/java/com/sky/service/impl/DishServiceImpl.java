package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.Result;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.sky.constant.StatusConstant.ENABLE;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

	@Autowired
	private DishFlavorService dishFlavorService; // 注入口味的 Service

	@Autowired
	private SetmealDishMapper setmealDishMapper; // 注入套餐菜品关系 Mapper (用于检查关联)

	@Autowired
	private RedisTemplate redisTemplate;

	// 菜品根据种类进行分类
	public final static String DISH_KEY = "dish:";

	/**
	 * 新增菜品
	 * @param dishDTO
	 * @return
	 */
	@Override
	@Transactional
	public Result saveWithFlavor(DishDTO dishDTO) {

		// 删除缓存数据库
//		String key = DISH_KEY + dishDTO.getCategoryId();
//		redisTemplate.delete(key);

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

	@Override
	public Result<Page<DishVO>> pageQuery(DishPageQueryDTO dishPageQueryDTO) {

		Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

		this.baseMapper.pageQuery(page,dishPageQueryDTO);

		return Result.success(page);
	}

	@Override
	@Transactional
	public Result deleteDish(List<Long> ids) {

		// 1.检查当前菜品是否起售
		// SQL: SELECT count(*) FROM dish WHERE id IN (?,?,?) AND status = 1
		Integer count = this.lambdaQuery()
				.in(Dish::getId, ids)
				.eq(Dish::getStatus, ENABLE)
				.count();

		if(count > 0) {
			// 如果存在起售中的菜品，抛出业务异常
			throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
		}

		// 2.检查菜品是否被套餐关联
		// SQL: SELECT count(*) FROM setmeal_dish WHERE dish_id IN (?,?,?)
		LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
		lambdaQueryWrapper.in(SetmealDish::getDishId, ids);

		Integer setmealCount = setmealDishMapper.selectCount(lambdaQueryWrapper);
		if(setmealCount > 0){
			throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
		}

		// 删除菜品表中的数据 (dish)
		removeByIds(ids);

		// 删除菜品关联的口味数据 (dish_flavor)
		dishFlavorService.lambdaUpdate()
				.in(DishFlavor::getDishId, ids)
				.remove(); // Service 提供的 remove 方法
		// 删除缓存数据库
//		Set<String> dish = redisTemplate.keys("dish:*");
//		redisTemplate.delete(dish);
		return Result.success();
	}

	@Override
	public Result<DishVO> getDishById(Long id) {

		DishVO dishVO = new DishVO();
		// 1.查询dish
		Dish dish = getById(id);
		BeanUtils.copyProperties(dish, dishVO);
		// 2.查询对应的口味
		List<DishFlavor> flavors = dishFlavorService.lambdaQuery()
				.eq(DishFlavor::getDishId, id)
				.list();
		dishVO.setFlavors(flavors);

		return Result.success(dishVO);
	}

	@Override
	@Transactional
	public Result updateDish(DishDTO dishDTO) {
//		Set<String> dishSet = redisTemplate.keys("dish:*");
//		redisTemplate.delete(dishSet);
		// 1.更新菜品数据
		Dish dish = new Dish();
		BeanUtils.copyProperties(dishDTO, dish);
		Long dishId = dishDTO.getId();
		updateById(dish);
		// 2.更新口味数据
		dishFlavorService.lambdaUpdate()
				.in(DishFlavor::getDishId, dishId)
				.remove(); // Service 提供的 remove 方法

		List<DishFlavor> flavors = dishDTO.getFlavors();
		if (flavors != null && !flavors.isEmpty()) {
			// 遍历集合，给每个口味赋予 dishId
			flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
			//  直接调用 Service 的批量保存方法
			dishFlavorService.saveBatch(flavors);
		}
		return Result.success();
	}

	/**
	 * 条件查询菜品和口味
	 * @param categoryId
	 * @return
	 */
	public List<DishVO> listWithFlavor(Long categoryId) {


		// 先查缓存数据库
//		List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(DISH_KEY + categoryId);
//		if (list != null) {
//			return list;
//		}

		// 缓存数据库没有，查数据库
		LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<Dish>()
				.eq(Dish::getCategoryId, categoryId)
				.eq(Dish::getStatus, ENABLE);
		List<Dish> dishList = list(lambdaQueryWrapper);

		List<DishVO> dishVOList = new ArrayList<>();

		// 给每个菜品设置口味
		for (Dish d : dishList) {
			DishVO dishVO = new DishVO();
			BeanUtils.copyProperties(d,dishVO);


//			List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
			//根据菜品id查询对应的口味
			List<DishFlavor> flavors = dishFlavorService.lambdaQuery()
					.eq(DishFlavor::getDishId, d.getId())
					.list();
			dishVO.setFlavors(flavors);
			dishVOList.add(dishVO);
		}

		// 保存到缓存数据库
//		redisTemplate.opsForValue().set(DISH_KEY + categoryId, dishVOList, 7, TimeUnit.DAYS);

		// 返回
		return dishVOList;
	}

	@Override
	public Result setStatus(Integer status, Long id) {

		Dish dish = Dish.builder()
				.id(id)
				.status(status)
				.build();

		updateById(dish);
//		Set<String> dishSet = redisTemplate.keys("dish:*");
//		redisTemplate.delete(dishSet);
		return Result.success();

	}
}
