package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
	TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);

	UserReportVO userStatistics(LocalDate begin, LocalDate end);

	OrderReportVO orderStatistics(LocalDate begin, LocalDate end);

	SalesTop10ReportVO topDishStatistics(LocalDate begin, LocalDate end);
}
