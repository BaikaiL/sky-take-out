package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;

public interface OrderService extends IService<Orders> {

	Result<OrderSubmitVO> submit(OrdersSubmitDTO ordersSubmitDTO);

	OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

	void paySuccess(String outTradeNo);
}
