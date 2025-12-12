package com.sky.controller.user;

import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/user")
@Slf4j
public class UserController {

	@Autowired
	private UserService userService;

	@PostMapping("/login")
	public Result<UserLoginVO> wechatLogin(@RequestBody UserLoginDTO userLoginDO){
		log.info("微信登录，用户数据：{}", userLoginDO);
		return userService.wechatLogin(userLoginDO);
	}
}
