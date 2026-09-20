package com.yqg.core.service.creditsdecreasequickorderinfo;

import com.yqg.core.model.generated.tables.records.CreditsDecreaseQuickOrderInfoRecord;
import com.yqg.core.model.sql.cashloan.CreditsDecreaseQuickOrderInfoModel;
import com.yqg.core.service.creditsdecreasequickorderinfo.enums.CreditsDecreaseQuickOrderStatus;
import com.yqg.core.service.creditsdecreasequickorderinfo.vo.CreditsDecreaseQuickOrderInfoVO;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditsDecreaseQuickOrderInfoService {
  @Autowired
  private CreditsDecreaseQuickOrderInfoModel creditsDecreaseQuickOrderInfoModel;

  public CreditsDecreaseQuickOrderInfoVO getValidOrExpiredRecordByLoanAccountId(Long loanAccountId) {
    List<CreditsDecreaseQuickOrderInfoRecord> records = creditsDecreaseQuickOrderInfoModel.findInitRecordByLoanAccountId(loanAccountId);
    if (CollectionUtils.isEmpty(records)) {
      return null;
    }
    for (CreditsDecreaseQuickOrderInfoRecord record : records) {
      if (record.getExpiredTime() <= Clock.now()) {
        creditsDecreaseQuickOrderInfoModel.updateStatus(record, CreditsDecreaseQuickOrderStatus.AUTO_EXPIRED);
      } else {
        return CreditsDecreaseQuickOrderInfoVO.from(record);
      }
    }
    return null;
  }

  public void ignoreCreditsDecreaseQuickOrder(Long loanAccountId) {
    List<CreditsDecreaseQuickOrderInfoRecord> records = creditsDecreaseQuickOrderInfoModel.findInitRecordByLoanAccountId(loanAccountId);
    if (CollectionUtils.isEmpty(records)) {
      return;
    }
    for (CreditsDecreaseQuickOrderInfoRecord record : records) {
      creditsDecreaseQuickOrderInfoModel.updateStatus(record, CreditsDecreaseQuickOrderStatus.MANUAL_EXPIRED);
    }
  }

  public void createOrderSuccess(Long orderId, Long id) {
    CreditsDecreaseQuickOrderInfoRecord record = creditsDecreaseQuickOrderInfoModel.fetchByIdOrThrow(id);
    creditsDecreaseQuickOrderInfoModel.createOrderSuccess(record, orderId);
  }

  public CreditsDecreaseQuickOrderInfoVO fetchByIdOrThrow(Long id) {
    return CreditsDecreaseQuickOrderInfoVO.from(creditsDecreaseQuickOrderInfoModel.fetchByIdOrThrow(id));
  }

  public void expireInitQuickOrder(Long loanAccountId) {
    List<CreditsDecreaseQuickOrderInfoRecord> records = creditsDecreaseQuickOrderInfoModel.findInitRecordByLoanAccountId(loanAccountId);
    if (CollectionUtils.isEmpty(records)) {
      return;
    }
    for (CreditsDecreaseQuickOrderInfoRecord record : records) {
      creditsDecreaseQuickOrderInfoModel.updateStatus(record, CreditsDecreaseQuickOrderStatus.NORMAL_ORDER_EXPIRED);
    }
  }
}
