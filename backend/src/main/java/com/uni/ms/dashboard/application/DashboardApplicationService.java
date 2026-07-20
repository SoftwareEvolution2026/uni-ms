package com.uni.ms.dashboard.application;

import com.uni.ms.dashboard.api.DashboardStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final DashboardQuery dashboardQuery;

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getStatistics() {
        return dashboardQuery.loadStatistics();
    }
}
