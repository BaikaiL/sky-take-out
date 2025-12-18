package com.sky.task;

import com.sky.entity.Orders;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

	@Autowired
	private OrderService orderService;

	/**
	 * 处理支付超时订单
	 */
	@Scheduled(cron = "0 * * * * ?")
	public void processingTimeoutOrders() {
		log.info("处理支付超时订单开始");
		LocalDateTime checkTime = LocalDateTime.now().minusMinutes(15);
		List<Orders> list = orderService.lambdaQuery().eq(Orders::getStatus, Orders.PENDING_PAYMENT)
				.lt(Orders::getOrderTime, checkTime)
				.list();

		if (list != null && !list.isEmpty()) {
			for (Orders orders : list) {
				orders.setStatus(Orders.CANCELLED);
				orders.setCancelReason("支付超时");
				orders.setCancelTime(LocalDateTime.now());
			}
			orderService.updateBatchById(list);
		}

		log.info("处理支付超时订单结束");
	}

	/**
	 * 处理处于派送中的订单
	 */
	@Scheduled(cron = "0 0 1 * * ?")
	private void processDeliveryOrders(){
		log.info("处理本日未确认订单开始");
		LocalDateTime checkTime = LocalDateTime.now().minusHours(1);
		List<Orders> list = orderService.lambdaQuery().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
				.lt(Orders::getDeliveryTime, checkTime)
				.list();
		if (list != null && !list.isEmpty()) {
			for (Orders orders : list) {
				orders.setStatus(Orders.COMPLETED);
				orders.setDeliveryTime(LocalDateTime.now());
			}
			orderService.updateBatchById(list);
		}
	}

}
