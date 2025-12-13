package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
//    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    Result<List<DishItemVO>> getDishItemById(Long id);

    Result<List<Setmeal>> listByCategoryId(Long categoryId);

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);


    /**
     * 分页查询
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询套餐和关联的菜品数据
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     */
    void updateWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐起售、停售
     */
    void startOrStop(Integer status, Long id);
}
