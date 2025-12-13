package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

//    @Autowired
//    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;


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
}
