package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.service.UserService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReportServiceImpl implements ReportService {

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private UserService userService;

	@Autowired
	private WorkspaceService workspaceService;

	/**
	 * 营业额统计
	 * @param begin
	 * @param end
	 * @return TurnoverReportVO
	 */
	@Override
	public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {

		// 1. 先计算两个日期之间相差多少天（包含当天，所以要 +1）
		List<LocalDate> dateList = getLocalDates(begin, end);

		// 2. 查数据库 (一次查询搞定！)
		// 注意时间范围：开始当天的 00:00:00 到 结束当天的 23:59:59
		LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
		LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

		QueryWrapper<Orders> wrapper = new QueryWrapper<>();
		wrapper.select("DATE_FORMAT(order_time, '%Y-%m-%d') as date", "SUM(amount) as turnover")
				.eq("status", Orders.COMPLETED)
				.ge("order_time", LocalDateTime.of(begin, LocalTime.MIN))
				.le("order_time", LocalDateTime.of(end, LocalTime.MAX))
				.groupBy("DATE_FORMAT(order_time, '%Y-%m-%d')");

		// 查询 Map 列表
		List<Map<String, Object>> dbList = orderMapper.selectMaps(wrapper);

		// 3. 将数据库结果转为 Map<日期String, 金额Double>，方便 O(1) 查找
		// 注意：数据库返回的金额可能是 null (如果是 BigDecimal 建议处理一下)
		Map<String, Double> turnoverMap = dbList.stream()
				.collect(Collectors.toMap(
						map -> (String) map.get("date"),
						map -> {
							Object turnover = map.get("turnover");
							return turnover == null ? 0.0 : ((BigDecimal) turnover).doubleValue();
						}
				));

		// 4. 数据对齐 (核心逻辑)
		List<Double> turnoverList = new ArrayList<>();
		List<String> dateStringList = new ArrayList<>();

		for (LocalDate date : dateList) {
			String dateStr = date.toString(); // "2025-12-18"
			dateStringList.add(dateStr);

			// 从 Map 中取值，如果没取到，说明当天没生意，填 0.0
			Double turnover = turnoverMap.getOrDefault(dateStr, 0.0);
			turnoverList.add(turnover);
		}

		// 5. 封装 VO 返回
		return TurnoverReportVO.builder()
				.dateList(String.join(",", dateStringList))
				.turnoverList(StringUtils.join(turnoverList, ","))
				.build();

	}


	/**
	 * 用户统计
	 * @param begin
	 * @param end
	 * @return UserReportVO
	 */
	@Override
	public UserReportVO userStatistics(LocalDate begin, LocalDate end) {

		// 1. 先计算两个日期之间相差多少天（包含当天，所以要 +1）
		List<LocalDate> dateList = getLocalDates(begin, end);

		LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
		LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

		// 统计出begin之前有多少用户
		int totalUserCount = userService.lambdaQuery().lt(User::getCreateTime, beginTime).count();

		// 再统计出begin~end期间有多少新增用户
		QueryWrapper<User> wrapper = new QueryWrapper<>();
		wrapper.select("DATE_FORMAT(create_time, '%Y-%m-%d') as date", "count(*) as count")
				.ge("create_time", beginTime)
				.le("create_time", endTime)
				.groupBy("DATE_FORMAT(create_time, '%Y-%m-%d')");
		List<Map<String, Object>> mapList = userService.getBaseMapper().selectMaps(wrapper);

		// 转为 Map<String, Integer> 方便查找
		Map<String, Integer> newUserMap = mapList.stream().collect(Collectors.toMap(
				map -> (String) map.get("date"),
				map -> ((Long) map.get("count")).intValue()
		));

		// 循环列表进行对齐和累加
		List<Integer> newUserList = new ArrayList<>();
		List<Integer> totalUserList = new ArrayList<>();

		for (LocalDate date : dateList) {
			String dateStr = date.toString();

			// 1. 获取当天新增
			Integer newUser = newUserMap.getOrDefault(dateStr, 0);

			// 2. 累加总人数 (当前总数 = 之前累计 + 今天新增)
			totalUserCount += newUser;

			newUserList.add(newUser);
			totalUserList.add(totalUserCount);
		}

		return UserReportVO.builder()
				.dateList(StringUtils.join(dateList, ","))
				.newUserList(StringUtils.join(newUserList, ","))
				.totalUserList(StringUtils.join(totalUserList, ","))
				.build();
	}

	/**
	 * 订单统计
	 * @param begin
	 * @param end
	 * @return OrderReportVO
	 */
	@Override
	public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {

		// 1. 先计算两个日期之间相差多少天（包含当天，所以要 +1）
		List<LocalDate> dateList = getLocalDates(begin, end);

		LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
		LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

		// 统计总订单
		QueryWrapper<Orders> totalWrapper = new QueryWrapper<>();
		totalWrapper.select("DATE_FORMAT(order_time, '%Y-%m-%d') as date", "count(id) as count")
				.between("order_time", beginTime, endTime)
				.groupBy("DATE_FORMAT(order_time, '%Y-%m-%d')");

		List<Map<String, Object>> totalOrderMapList = orderMapper.selectMaps(totalWrapper);

		// 统计有效订单
		QueryWrapper<Orders> validWrapper = new QueryWrapper<>();
		validWrapper.select("DATE_FORMAT(order_time, '%Y-%m-%d') as date", "count(id) as count")
				.between("order_time", beginTime, endTime)
				.eq("status", Orders.COMPLETED) // 只查已完成的
				.groupBy("DATE_FORMAT(order_time, '%Y-%m-%d')");

		List<Map<String, Object>> validOrderMapList = orderMapper.selectMaps(validWrapper);

		Map<String, Integer> totalOrderMap = convertListToMap(totalOrderMapList);
		Map<String, Integer> validOrderMap = convertListToMap(validOrderMapList);

		Integer totalOrderCount = 0;
		Integer validOrderCount = 0;

		// 循环日期列表，进行对齐和计算
		List<Integer> orderCountList = new ArrayList<>();       // 每日订单数列表
		List<Integer> validOrderCountList = new ArrayList<>();  // 每日有效订单数列表

		for (LocalDate date : dateList) {
			String dateStr = date.toString();

			Integer totalOrder = totalOrderMap.getOrDefault(dateStr, 0);
			Integer validOrder = validOrderMap.getOrDefault(dateStr, 0);

			orderCountList.add(totalOrder);
			validOrderCountList.add(validOrder);

			totalOrderCount += totalOrder;
			validOrderCount += validOrder;
		}

		// 计算订单完成率
		Double orderCompletionRate = 0.0;
		if (totalOrderCount != 0) {
			orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
		}

		return OrderReportVO.builder()
				.dateList(StringUtils.join(dateList, ","))
				.orderCountList(StringUtils.join(orderCountList, ","))
				.validOrderCountList(StringUtils.join(validOrderCountList, ","))
				.totalOrderCount(totalOrderCount)
				.validOrderCount(validOrderCount)
				.orderCompletionRate(orderCompletionRate)
				.build();
	}

	/**
	 * 销量排名
	 * @param begin
	 * @param end
	 * @return SalesTop10ReportVO
	 */
	@Override
	public SalesTop10ReportVO topDishStatistics(LocalDate begin, LocalDate end) {

		// 1. 先计算两个日期之间相差多少天（包含当天，所以要 +1）
		List<LocalDate> dateList = getLocalDates(begin, end);

		LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
		LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

		List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

		// 提取菜名
		List<String> names = salesTop10.stream()
				.map(GoodsSalesDTO::getName)
				.collect(Collectors.toList());
		String nameList = StringUtils.join(names, ",");

		// 提取销量
		List<Integer> numbers = salesTop10.stream()
				.map(GoodsSalesDTO::getNumber)
				.collect(Collectors.toList());
		String numberList = StringUtils.join(numbers, ",");

		return SalesTop10ReportVO.builder()
				.nameList(nameList)
				.numberList(numberList)
				.build();
	}

	@Override
	public void exportBusinessData(HttpServletResponse response) {

		LocalDateTime begin = LocalDateTime.of(LocalDate.now().minusDays(30), LocalTime.MIN);
		LocalDateTime end = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

		BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);

		// 输入流对象
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

		try {
			XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

			// 获取第一个工作表
			 XSSFSheet sheet = workbook.getSheetAt(0);
			 // 填充数据
			sheet.getRow(1).getCell(1).setCellValue(businessData.getTurnover());

			XSSFRow row = sheet.getRow(3);
			row.getCell(2).setCellValue(businessData.getTurnover());
			row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
			row.getCell(6).setCellValue(businessData.getNewUsers());

			row = sheet.getRow(4);
			row.getCell(2).setCellValue(businessData.getValidOrderCount());
			row.getCell(4).setCellValue(businessData.getUnitPrice());

			// 查询订单相关数据 (营业额、有效订单、总订单)
			List<Map<String, Object>> orderStats = orderMapper.getBusinessStatistics(begin, end);

			//查询用户相关数据 (新增用户)
			QueryWrapper<User> wrapper = new QueryWrapper<>();
			wrapper.select("DATE_FORMAT(create_time, '%Y-%m-%d') as date", "count(*) as count")
					.ge("create_time", begin)
					.le("create_time", end)
					.groupBy("DATE_FORMAT(create_time, '%Y-%m-%d')");
			List<Map<String, Object>> userStats = userService.getBaseMapper().selectMaps(wrapper);

			// 将数据库结果转为 Map<日期, 数据> 方便查找
			Map<String, Map<String, Object>> orderDataMap = orderStats.stream()
					.collect(Collectors.toMap(m -> (String) m.get("date"), m -> m));

			Map<String, Integer> userMap = userStats.stream()
					.collect(Collectors.toMap(
							m -> (String) m.get("date"),
							m -> ((Number) m.get("count")).intValue()
					));

			List<BusinessDataVO> reportList = new ArrayList<>();
			List<LocalDate> dateList = getLocalDates(begin.toLocalDate(), end.toLocalDate());
			for (LocalDate date : dateList) {
				String dateStr = date.toString();

				// --- 提取数据 (如果没有数据则补0) ---
				Map<String, Object> dayOrderData = orderDataMap.getOrDefault(dateStr, new HashMap<>());

				// 营业额
				Double turnover = dayOrderData.get("turnover") == null ? 0.0 : ((Number) dayOrderData.get("turnover")).doubleValue();
				// 有效订单
				Integer validOrderCount = dayOrderData.get("validOrderCount") == null ? 0 : ((Number) dayOrderData.get("validOrderCount")).intValue();
				// 总订单
				Integer totalOrderCount = dayOrderData.get("totalOrderCount") == null ? 0 : ((Number) dayOrderData.get("totalOrderCount")).intValue();
				// 新增用户
				Integer newUsers = userMap.getOrDefault(dateStr, 0);

				// --- 计算衍生指标 ---
				// 订单完成率 = 有效 / 总
				Double orderCompletionRate = 0.0;
				// 平均客单价 = 营业额 / 有效
				Double unitPrice = 0.0;

				if (totalOrderCount != 0) {
					orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
				}
				if (validOrderCount != 0) {
					unitPrice = turnover / validOrderCount;
				}

				// --- 添加到列表 ---
				reportList.add(BusinessDataVO.builder()
						.date(date)
						.turnover(turnover)
						.validOrderCount(validOrderCount)
						.orderCompletionRate(orderCompletionRate)
						.unitPrice(unitPrice)
						.newUsers(newUsers)
						.build());
			}

			// 填充下方的明细列表 (假设从第 8 行开始)
			int rowIndex = 7;
			for (BusinessDataVO vo : reportList) {
				row = sheet.getRow(rowIndex);
				if (row == null) { row = sheet.createRow(rowIndex); }

				// 根据 image_bfcb72.png 的列顺序填充
				row.getCell(1).setCellValue(vo.getDate().toString());        // 日期
				row.getCell(2).setCellValue(vo.getTurnover());             // 营业额
				row.getCell(3).setCellValue(vo.getValidOrderCount());      // 有效订单
				row.getCell(4).setCellValue(vo.getOrderCompletionRate());  // 完成率
				row.getCell(5).setCellValue(vo.getUnitPrice());            // 客单价
				row.getCell(6).setCellValue(vo.getNewUsers());             // 新增用户

				rowIndex++;
			}

			ServletOutputStream outputStream = response.getOutputStream();
			workbook.write(outputStream);


			// 关闭资源
			workbook.close();
			inputStream.close();
			outputStream.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将 List<Map<String, Object>> 转为 Map<String, Integer>
	 * @param list
	 * @return Map<String, Integer>
	 */
	private Map<String, Integer> convertListToMap(List<Map<String, Object>> list) {
		Map<String, Integer> map = new HashMap<>();
		if (list != null) {
			for (Map<String, Object> item : list) {
				String dateKey = (String) item.get("date");
				// 注意类型转换，count 可能是 Long
				Integer countVal = ((Number) item.get("count")).intValue();
				map.put(dateKey, countVal);
			}
		}
		return map;
	}


	/**
	 * 获取两个日期之间的所有日期
	 * @param begin 开始日期
	 * @param end 结束日期
	 * @return List<LocalDate> 日期列表
	 */
	private static List<LocalDate> getLocalDates(LocalDate begin, LocalDate end) {
		long days = ChronoUnit.DAYS.between(begin, end) + 1;

		// 2. 生成流
		List<LocalDate> dateList = Stream.iterate(begin, date -> date.plusDays(1))
				.limit(days) // 限制生成数量
				.collect(Collectors.toList());
		return dateList;
	}
}
