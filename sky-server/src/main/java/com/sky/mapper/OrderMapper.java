package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

	List<Map<String, Object>> sumTurnoverByDate(@Param("begin") LocalDateTime beginTime,
	                                            @Param("end") LocalDateTime endTime,
	                                            @Param("status") Integer status);

	/**
	 * 统计指定时间区间内的销量前10
	 */
	List<GoodsSalesDTO> getSalesTop10(@Param("begin") LocalDateTime begin,
	                                  @Param("end") LocalDateTime end);
}
