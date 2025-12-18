package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService extends IService<Orders> {

	Result<OrderSubmitVO> submit(OrdersSubmitDTO ordersSubmitDTO);

	OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

	void paySuccess(String outTradeNo);

	Result<PageResult> pageQuery(int page, int pageSize, Integer status);

	Result<OrderVO> getOrderDetail(Long id);

	Result cancelOrder(Long id);

	Result repetition(Long id);

	PageResult adminConditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

	OrderStatisticsVO statistics();

	void confirm(OrdersConfirmDTO ordersConfirmDTO);

	void rejection(OrdersRejectionDTO ordersRejectionDTO);

	void cancel(OrdersCancelDTO ordersCancelDTO);

	void delivery(Long id);

	void complete(Long id);

	Result reminder(Long id);
}
