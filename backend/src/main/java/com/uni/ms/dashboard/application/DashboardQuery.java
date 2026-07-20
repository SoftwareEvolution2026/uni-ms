package com.uni.ms.dashboard.application;

import com.uni.ms.dashboard.api.DashboardStatisticsResponse;

public interface DashboardQuery {

    DashboardStatisticsResponse loadStatistics();
}
