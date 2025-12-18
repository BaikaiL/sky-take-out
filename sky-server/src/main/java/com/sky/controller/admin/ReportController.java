package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
public class ReportController {

	@Autowired
	private ReportService reportService;

	@GetMapping("/turnoverStatistics")
	public Result<TurnoverReportVO> turnoverStatistics(
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
		return Result.success(reportService.turnoverStatistics(begin, end));
	}

	@GetMapping("/userStatistics")
	public Result<UserReportVO> userStatistics(
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
		return Result.success(reportService.userStatistics(begin, end));
	}

	@GetMapping("/ordersStatistics")
	public Result<OrderReportVO> orderStatistics(
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
		return Result.success(reportService.orderStatistics(begin, end));
	}

	@GetMapping("/top10")
	public Result<SalesTop10ReportVO> topDishStatistics(
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
		return Result.success(reportService.topDishStatistics(begin, end));
	}

	@GetMapping("/export")
	public void exportBusinessData(HttpServletResponse response) {
		reportService.exportBusinessData(response);
	}
}
