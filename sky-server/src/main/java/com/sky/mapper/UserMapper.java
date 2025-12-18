package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

	/**
	 * 根据动态条件统计用户数量
	 * keys: begin, end
	 */
	Integer countByMap(Map map);
}
