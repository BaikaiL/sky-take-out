package com.sky.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.context.BaseContext; // 引入苍穹外卖封装的ThreadLocal工具类
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器
 * 用于MyBatis-Plus自动填充公共字段
 */
@Slf4j
@Component // 必须加这个注解，交给Spring管理
public class AutoFillMetaObjectHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		log.info("开始进行公共字段自动填充(insert)...");

		// 自动填充创建时间、更新时间
		this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
		this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

		// 自动填充创建人、更新人
		// 从 BaseContext (ThreadLocal) 中获取当前登录用户的 ID
		Long currentId = BaseContext.getCurrentId();

		this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
		this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		log.info("开始进行公共字段自动填充(update)...");

		// 更新时，只填充更新时间和更新人
		this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

		Long currentId = BaseContext.getCurrentId();
		this.strictUpdateFill(metaObject, "updateUser", Long.class, currentId);
	}
}
