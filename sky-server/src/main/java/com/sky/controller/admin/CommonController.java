package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static com.sky.constant.MessageConstant.UPLOAD_FAILED;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class CommonController {

	@Autowired
	private AliOssUtil aliOssUtil;

	@PostMapping("/upload")
	public Result<String> upload(MultipartFile file) {
		log.info("文件上传：{}", file);

		try {
			// 1. 获取原始文件名
			String originalFilename = file.getOriginalFilename();

			// 2. 截取文件扩展名 (如 .jpg, .png)
			String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

			// 3. 构造新文件名 (UUID + 扩展名)，防止文件名冲突覆盖
			String objectName = UUID.randomUUID() + extension;

			// 4. 调用工具类上传，返回访问路径
			String filePath = aliOssUtil.upload(file.getBytes(), objectName);

			// 5. 返回 URL 给前端
			return Result.success(filePath);

		} catch (IOException e) {
			log.error(UPLOAD_FAILED + "{}", e.getMessage());
		}

		return Result.error(UPLOAD_FAILED);
	}
}

