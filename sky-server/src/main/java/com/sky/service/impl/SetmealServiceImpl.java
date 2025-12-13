package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealDishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

//    @Autowired
//    private SetmealMapper setmealMapper;
//    @Autowired
//    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealDishService setmealDishService;

//    /**
//     * 条件查询
//     * @param setmeal
//     * @return
//     */
//    public List<Setmeal> list(Setmeal setmeal) {
//        List<Setmeal> list = setmealMapper.listByCondition(setmeal);
//        return list;
//    }

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    public Result<List<DishItemVO>> getDishItemById(Long id) {
        List<DishItemVO> dishItemVO = this.baseMapper.getDishItemBySetmealId(id);
        return Result.success(dishItemVO);
    }

    @Override
    public Result<List<Setmeal>> listByCategoryId(Long categoryId) {


        List<Setmeal> list = lambdaQuery().eq(Setmeal::getCategoryId, categoryId)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .list();

        return Result.success(list);
    }

    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        save(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(sd -> sd.setSetmealId(setmealId)); // 设置关联ID
            setmealDishService.saveBatch(setmealDishes);
        }

    }

    /**
     * 分页查询
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO dto) {
        Page<Setmeal> pageInfo = new Page<>(dto.getPage(), dto.getPageSize());

        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dto.getName()), Setmeal::getName, dto.getName())
                .eq(dto.getCategoryId() != null, Setmeal::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, Setmeal::getStatus, dto.getStatus())
                .orderByDesc(Setmeal::getUpdateTime);

        this.page(pageInfo, wrapper);

        return new PageResult(pageInfo.getTotal(), pageInfo.getRecords());
    }

    /**
     * 批量删除套餐
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1. 判断是否起售中
        // select count(*) from setmeal where id in (...) and status = 1
        Integer count = this.lambdaQuery()
                .in(Setmeal::getId, ids)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .count();

        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }

        // 2. 删除套餐表数据
        this.removeByIds(ids);

        // 3. 删除关系表数据 (setmeal_dish)
        // delete from setmeal_dish where setmeal_id in (...)
        setmealDishService.lambdaUpdate()
                .in(SetmealDish::getSetmealId, ids)
                .remove();
    }

    /**
     * 根据ID查询
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 1. 查套餐
        Setmeal setmeal = this.getById(id);

        // 2. 查关联菜品
        List<SetmealDish> setmealDishes = setmealDishService.lambdaQuery()
                .eq(SetmealDish::getSetmealId, id)
                .list();

        // 3. 组装
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        // 1. 修改套餐表
        this.updateById(setmeal);

        // 2. 修改关联表：先删后加
        Long setmealId = setmealDTO.getId();

        // 2.1 删除旧关系
        setmealDishService.lambdaUpdate()
                .eq(SetmealDish::getSetmealId, setmealId)
                .remove();

        // 2.2 添加新关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(sd -> sd.setSetmealId(setmealId));
            setmealDishService.saveBatch(setmealDishes);
        }
    }

    /**
     * 起售停售
     */
    @Override
    public void startOrStop(Integer status, Long id) {

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        this.updateById(setmeal);
    }
}
