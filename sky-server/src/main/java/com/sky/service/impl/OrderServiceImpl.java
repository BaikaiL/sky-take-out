package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderDetailService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.service.UserService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sky.constant.MessageConstant.*;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

	@Autowired
	 private AddressBookMapper addressBookMapper;

	@Autowired
	private ShoppingCartService shoppingCartService;

	@Autowired
	private OrderDetailService orderDetailService;

	@Autowired
	private UserService userService;

	@Autowired
	private WeChatPayUtil weChatPayUtil;

	@Autowired
	private WebSocketServer webSocketServer;


	/* =========================================================================
	 * 用户端 (User) 业务逻辑
	 * ========================================================================= */

	/**
	 * 用户下单
	 * @param ordersSubmitDTO
	 * @return
	 */
	@Override
	@Transactional
	public Result<OrderSubmitVO> submit(OrdersSubmitDTO ordersSubmitDTO) {

		// 处理业务异常：1.地址为空 2.购物车为空
		AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
		if (addressBook == null) {
			throw new AddressBookBusinessException(ADDRESS_BOOK_IS_NULL);
		}

		Long userId = BaseContext.getCurrentId();

		List<ShoppingCart> shoppingCarts = shoppingCartService.query().eq("user_id", userId).list();
		if (shoppingCarts.isEmpty()) {
			throw new ShoppingCartBusinessException(SHOPPING_CART_IS_NULL);
		}

		// 插入订单数据
		Orders orders = new Orders();
		BeanUtils.copyProperties(ordersSubmitDTO, orders);
		orders.setUserId(userId);
		orders.setOrderTime(LocalDateTime.now());
		orders.setPayStatus(Orders.UN_PAID);
		orders.setStatus(Orders.PENDING_PAYMENT);
		orders.setNumber(String.valueOf(System.currentTimeMillis()));
		orders.setConsignee(addressBook.getConsignee());
		orders.setPhone(addressBook.getPhone());
		orders.setAddress(addressBook.getDetail());
		save(orders);

		// 插入订单明细数据
		ArrayList<OrderDetail> orderDetails = new ArrayList<>();
		for (ShoppingCart shoppingCart : shoppingCarts) {
			OrderDetail orderDetail = new OrderDetail();
			BeanUtils.copyProperties(shoppingCart, orderDetail);
			orderDetail.setOrderId(orders.getId());
			orderDetails.add(orderDetail);
		}

		orderDetailService.saveBatch(orderDetails);

		// 删除购物车数据
		shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));

		// 封装VO返回
		OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
				.id(orders.getId())
				.orderNumber(orders.getNumber())
				.orderAmount(orders.getAmount())
				.orderTime(orders.getOrderTime())
				.build();
		return Result.success(orderSubmitVO);
	}

	/**
	 * 订单支付
	 *
	 * @param ordersPaymentDTO
	 * @return
	 */
	public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
		// 当前登录用户id
		Long userId = BaseContext.getCurrentId();
		User user = userService.getById(userId);

		//调用微信支付接口，生成预支付交易单
		/*JSONObject jsonObject = weChatPayUtil.pay(
				ordersPaymentDTO.getOrderNumber(), //商户订单号
				new BigDecimal(0.01), //支付金额，单位 元
				"苍穹外卖订单", //商品描述
				user.getOpenid() //微信用户的openid
		);*/

		// 模拟调用微信支付
		JSONObject jsonObject = new JSONObject();

		if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
			throw new OrderBusinessException("该订单已支付");
		}

		OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
		vo.setPackageStr(jsonObject.getString("package"));

		return vo;
	}

	/**
	 * 支付成功，修改订单状态
	 *
	 * @param outTradeNo
	 */
	@Override
	public void paySuccess(String outTradeNo) {

		// 根据订单号查询订单
//		Orders ordersDB = orderMapper.getByNumber(outTradeNo);
		Orders ordersDB = query().eq("number", outTradeNo).one();

		// 根据订单id更新订单的状态、支付方式、支付状态、结账时间
		Orders orders = Orders.builder()
				.id(ordersDB.getId())
				.status(Orders.TO_BE_CONFIRMED)
				.payStatus(Orders.PAID)
				.checkoutTime(LocalDateTime.now())
				.build();
//		update(orders);
		updateById(orders);

		// 使用websocket向商家推送订单提醒
		Map map = new HashMap<>();
		map.put("type", 1);
		map.put("orderId", ordersDB.getId());
		map.put("content", "订单号：" + ordersDB.getNumber());
		webSocketServer.sendToAllClient(JSON.toJSONString(map));
	}

	@Override
	public Result<PageResult> pageQuery(int page, int pageSize, Integer status) {

		// 构造分页对象
		Page<Orders> pageInfo = new Page<>(page, pageSize);

		// 构造查询条件
		LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
		queryWrapper.eq(status != null, Orders::getStatus, status);
		// 执行分页查询
		pageInfo = this.page(pageInfo, queryWrapper);


		List<OrderVO> list = getOrderVOList(pageInfo);

		// 返回分页结果
		return Result.success(new PageResult(pageInfo.getTotal(), list));

	}



	@Override
	public Result cancelOrder(Long id) {

		// 查询订单状态
		Orders orders = getById(id);
		if(orders == null){
			throw new OrderBusinessException(ORDER_NOT_FOUND);
		}

		/*待支付和待接单状态下，用户可直接取消订单
		商家已接单状态下，用户取消订单需电话沟通商家
		派送中状态下，用户取消订单需电话沟通商家
		如果在待接单状态下取消订单，需要给用户退款
		取消订单后需要将订单状态修改为“已取消”*/
//		用户无法自行取消订单，需要联系商家
		if(orders.getStatus() > 2){
			throw new OrderBusinessException(ORDER_STATUS_ERROR);
		}

		if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
			// 给用户退款
			//调用微信支付退款接口
			/*weChatPayUtil.refund(
					orders.getNumber(), //商户订单号
					orders.getNumber(), //商户退款单号
					new BigDecimal(0.01),//退款金额，单位 元
					new BigDecimal(0.01));//原订单金额

			//支付状态修改为 退款*/
			orders.setPayStatus(Orders.REFUND);
		}

		orders.setStatus(Orders.CANCELLED);
		orders.setCancelReason("用户取消");
		orders.setCancelTime(LocalDateTime.now());
		updateById(orders);

		return Result.success();
	}

	@Override
	public Result repetition(Long id) {

		//再来一单就是将原订单中的商品重新加入到购物车中

		// 获取原订单数据
		Orders orders = getById(id);
		if(orders == null){
			throw new OrderBusinessException(ORDER_NOT_FOUND);
		}
		List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orders.getId()).list();
		if(orderDetailList == null || orderDetailList.isEmpty()){
			throw new OrderBusinessException(ORDER_NOT_FOUND);
		}

		// 获取订单商品
		ArrayList<ShoppingCart> shoppingCarts = new ArrayList<>();
		for (OrderDetail orderDetail : orderDetailList) {
			ShoppingCart shoppingCart = new ShoppingCart();
			BeanUtils.copyProperties(orderDetail, shoppingCart);
			shoppingCart.setUserId(BaseContext.getCurrentId());
			shoppingCarts.add(shoppingCart);
		}

		shoppingCartService.saveBatch(shoppingCarts);
		return Result.success();

	}


	/* =========================================================================
	 * 管理端 (Admin) 业务逻辑
	 * ========================================================================= */

	@Override
	@SuppressWarnings("unchecked")
	public PageResult adminConditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

		Page<Orders> pageInfo = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

		// 2. 构建查询条件 Wrapper
		LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
		wrapper.like(StringUtils.hasText(ordersPageQueryDTO.getNumber()), Orders::getNumber, ordersPageQueryDTO.getNumber()) // 订单号模糊查询
				.like(StringUtils.hasText(ordersPageQueryDTO.getPhone()), Orders::getPhone, ordersPageQueryDTO.getPhone())   // 手机号模糊查询
				.eq(ordersPageQueryDTO.getStatus() != null, Orders::getStatus, ordersPageQueryDTO.getStatus())               // 状态精确查询
				.ge(ordersPageQueryDTO.getBeginTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())      // 开始时间 >=
				.le(ordersPageQueryDTO.getEndTime() != null, Orders::getOrderTime, ordersPageQueryDTO.getEndTime())          // 结束时间 <=
				.eq(ordersPageQueryDTO.getUserId() != null, Orders::getUserId, ordersPageQueryDTO.getUserId())               // 用户ID
				.orderByDesc(Orders::getOrderTime);                                            // 按下单时间降序

		// 3. 执行查询
		this.page(pageInfo, wrapper);

		// 4. 封装 OrderVO (原有逻辑复用)
		List<OrderVO> orderVOList = getOrderVOList(pageInfo);

		return new PageResult(pageInfo.getTotal(), orderVOList);
	}


	/**
	 * 统计订单数据(待确认，已确认，派送中)
	 * @return
	 */
	@Override
	public OrderStatisticsVO statistics() {

		// 计算各个状态的订单数量
		int toBeConfirm = count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.TO_BE_CONFIRMED));
		int confirm = count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.CONFIRMED));
		int delivery = count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));

		return new OrderStatisticsVO(toBeConfirm, confirm, delivery);
	}

	/**
	 * 订单确认
	 * @param ordersConfirmDTO
	 */
	@Override
	public void confirm(OrdersConfirmDTO ordersConfirmDTO) {

		Orders orders = getById(ordersConfirmDTO.getId());
		if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
			throw new OrderBusinessException(ORDER_STATUS_ERROR);
		}

		Orders newOrders = new Orders();
		newOrders.setId(orders.getId());
		newOrders.setStatus(Orders.CONFIRMED);
		updateById(newOrders);
	}

	/**
	 * 商家订单拒接
	 * @param ordersRejectionDTO
	 */
	@Override
	public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
		/*商家拒单其实就是将订单状态修改为“已取消”
		只有订单处于“待接单”状态时可以执行拒单操作
		商家拒单时需要指定拒单原因
		商家拒单时，如果用户已经完成了支付，需要为用户退款*/

		// 判断订单状态
		Orders orders = getById(ordersRejectionDTO.getId());
		if (!orders.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
			throw new OrderBusinessException(ORDER_STATUS_ERROR);
		}

		// 判断支付状态
		if (orders.getPayStatus().equals(Orders.PAID)) {
			// 调用微信支付退款接口
			//用户已支付，需要退款
			/*String refund = weChatPayUtil.refund(
					ordersDB.getNumber(),
					ordersDB.getNumber(),
					new BigDecimal(0.01),
					new BigDecimal(0.01));*/
			log.info("订单申请退款：{}", orders.getNumber());
		}

		Orders newOrders = new Orders();
		newOrders.setStatus(Orders.CANCELLED);
		newOrders.setCancelReason(ordersRejectionDTO.getRejectionReason());
		newOrders.setCancelTime(LocalDateTime.now());
		updateById(newOrders);
	}

	@Override
	public void cancel(OrdersCancelDTO ordersCancelDTO) {
		/*取消订单其实就是将订单状态修改为“已取消”
		商家取消订单时需要指定取消原因
		商家取消订单时，如果用户已经完成了支付，需要为用户退款*/
		Orders orders = getById(ordersCancelDTO.getId());

		// 判断支付状态
		if (orders.getPayStatus().equals(Orders.PAID)) {
			// 调用微信支付退款接口
			//用户已支付，需要退款
			/*String refund = weChatPayUtil.refund(
					ordersDB.getNumber(),
					ordersDB.getNumber(),
					new BigDecimal(0.01),
					new BigDecimal(0.01));*/
			log.info("订单申请退款：{}", orders.getNumber());
		}

		Orders newOrders = new Orders();
		newOrders.setStatus(Orders.CANCELLED);
		newOrders.setCancelReason(ordersCancelDTO.getCancelReason());
		newOrders.setCancelTime(LocalDateTime.now());
		updateById(newOrders);
	}

	@Override
	public void delivery(Long id) {
		/*派送订单其实就是将订单状态修改为“派送中”
		只有状态为“待派送”的订单可以执行派送订单操作*/

		Orders orders = getById(id);

		if (orders == null || !orders.getStatus().equals(Orders.CONFIRMED)) {
			throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
		}

		Orders newOrders = new Orders();
		newOrders.setStatus(Orders.DELIVERY_IN_PROGRESS);
		newOrders.setId(orders.getId());
		updateById(newOrders);
	}

	@Override
	public void complete(Long id) {
		/*完成订单其实就是将订单状态修改为“已完成”
		只有状态为“派送中”的订单可以执行订单完成操作*/

		Orders orders = getById(id);

		if (orders == null || !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
			throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
		}

		Orders newOrders = new Orders();
		newOrders.setStatus(Orders.COMPLETED);
		newOrders.setId(orders.getId());

		newOrders.setDeliveryTime(LocalDateTime.now());
		updateById(newOrders);
	}

	@Override
	public Result reminder(Long id) {
		Orders orders = getById(id);

		if (orders == null) {
			throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
		}

		Map map = new HashMap<>();
		map.put("type", 2);
		map.put("orderId", id);
		map.put("content", "订单号：" + orders.getNumber());
		webSocketServer.sendToAllClient(JSON.toJSONString(map));

		return Result.success();
	}

	// ================== 用户商家共有/辅助方法 (复用原有逻辑，稍作修改) ==================

	/**
	 * 获取订单详情
	 * @param id
	 * @return
	 */
	@Override
	public Result<OrderVO> getOrderDetail(Long id) {

		OrderVO orderVO = new OrderVO();
		Orders orders = getById(id);

		List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orders.getId()).list();
		BeanUtils.copyProperties(orders, orderVO);
		orderVO.setOrderDetailList(orderDetailList);

		return Result.success(orderVO);
	}

	/**
	 * 查找每个orders 的 orderDetailList
	 * 封装到 OrderVO 中
	 * @param pageInfo
	 * @return
	 */
	private List<OrderVO> getOrderVOList(Page<Orders> pageInfo) {
		List<OrderVO> list = new ArrayList<>();
		// 查询出订单明细，并封装入OrderVO进行响应
		if (pageInfo.getRecords() != null && !pageInfo.getRecords().isEmpty()) {
			for (Orders orders : pageInfo.getRecords()) {
				List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orders.getId()).list();
				OrderVO orderVO = new OrderVO();
				BeanUtils.copyProperties(orders, orderVO);

				// 获取菜品信息
				List<String> orderDishList = orderDetailList.stream().map(x -> {
					return x.getName() + "*" + x.getNumber() + ";";
				}).collect(Collectors.toList());

				orderVO.setOrderDishes(String.join("", orderDishList));
				orderVO.setOrderDetailList(orderDetailList);

				// 获取详细地址信息
				AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
				if(addressBook != null) {
					orderVO.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
				}
				list.add(orderVO);
			}
		}
		return list;
	}

}
