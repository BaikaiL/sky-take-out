package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderMapper;
import com.sky.result.Result;
import com.sky.service.OrderDetailService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.service.UserService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.omg.PortableServer.POAPackage.AdapterNonExistent;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.sky.constant.MessageConstant.ADDRESS_BOOK_IS_NULL;
import static com.sky.constant.MessageConstant.SHOPPING_CART_IS_NULL;

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

	}

}
