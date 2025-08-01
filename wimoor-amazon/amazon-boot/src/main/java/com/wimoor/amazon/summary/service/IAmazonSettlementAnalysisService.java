package com.wimoor.amazon.summary.service;

import com.wimoor.amazon.finances.pojo.dto.FinDataMonthDTO;
import com.wimoor.amazon.finances.pojo.entity.AmzSettlementAccReport;
import com.wimoor.common.mvc.ProgressHelper;

import java.util.List;
import java.util.Map;

public interface IAmazonSettlementAnalysisService {
    public void confirm(AmzSettlementAccReport item);
	public void refreshAll();
	public void checkSummaryData();
	public void refreshAllByManual();
	public void refreshData(ProgressHelper progressHelper, FinDataMonthDTO dto);
	public void checkSummaryDataByManual(List<Map<String, Object>>  list);
	public  void doSummaryDataByManual(List<Map<String, Object>> list);
}
