package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


	private final static String WECHAT_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

	@Autowired
	private JwtProperties jwtProperties;
	@Autowired
	private WeChatProperties weChatProperties;

	@Override
	public Result<UserLoginVO> wechatLogin(UserLoginDTO userLoginDTO) {

		String openid = getOpenid(userLoginDTO);
		// 判断openid是否为空
		if(openid == null){
			throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
		}

		// 判断是否是新用户
		User user = lambdaQuery()
				.eq(User::getOpenid, openid)
				.one();
		// 如果是新用户，自动完成注册
		if(user == null){
			user = User.builder()
					.openid(openid)
					.build();
			save(user);
		}

		Long userId = user.getId();

		// 创建jwt令牌
		Map<String, Object> map = new HashMap<>();
		map.put(JwtClaimsConstant.USER_ID, userId);
		String token = JwtUtil.createJWT(
				jwtProperties.getUserSecretKey(),
				jwtProperties.getUserTtl(),
				map
		);

		// 封装vo对象
		UserLoginVO userLoginVO = UserLoginVO.builder()
				.id(userId)
				.openid(userLoginDTO.getCode())
				.token(token)
				.build();
		return Result.success(userLoginVO);
	}

	private String getOpenid(UserLoginDTO userLoginDTO) {
		// 调用微信接口实现登录
		Map<String, String> Login_Map = new HashMap<>();
		Login_Map.put("appid", weChatProperties.getAppid());
		Login_Map.put("secret", weChatProperties.getSecret());
		Login_Map.put("js_code", userLoginDTO.getCode());
		Login_Map.put("grant_type", "authorization_code");

		String result = HttpClientUtil.doGet(WECHAT_LOGIN_URL, Login_Map);

		log.info("微信接口返回数据：{}", result);

		// 解析
		JSONObject jsonObject = JSON.parseObject(result);
		return jsonObject.getString("openid");
	}

}

