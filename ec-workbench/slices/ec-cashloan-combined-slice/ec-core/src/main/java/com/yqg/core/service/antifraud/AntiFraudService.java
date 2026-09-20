package com.yqg.core.service.antifraud;

import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.AntiFraudOverdueCaseLogRecord;
import com.yqg.core.model.generated.tables.records.AntiFraudOverdueCaseRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.sql.antifraud.AntiFraudOverdueCaseLogModel;
import com.yqg.core.model.sql.antifraud.AntiFraudOverdueCaseModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.user.enums.MergeAccountFailedReason;
import com.yqg.core.service.user.enums.MergeAccountUserType;
import com.yqg.core.service.user.vo.AntiFraudBlackListLogVo;
import com.yqg.core.service.user.vo.MergeAccountFailedReasonVO;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.enums.antifraud.AntiFraudOverdueCaseStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chenxianrui
 * @date 2025/6/17
 */
@Service
@Slf4j
public class AntiFraudService {
  @Autowired
  private AntiFraudOverdueCaseModel antiFraudOverdueCaseModel;
  @Autowired
  private AntiFraudOverdueCaseLogModel antiFraudOverdueCaseLogModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private CashLoanConfig cashLoanConfig;

  public Boolean overdueCaseCreate(Long userId, Long collectionCaseId, Long collectionCaseTimeCreated) {
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserIdForUpdateOrThrow(userId);
      AntiFraudOverdueCaseLogRecord caseLogRecord = antiFraudOverdueCaseLogModel.findByCollectionCaseId(collectionCaseId);
      if (Objects.nonNull(caseLogRecord)) {
        log.info("Existing AntiFraudOverdueCaseLogRecord found for collectionCaseId: {}", collectionCaseId);
        return true;
      }
      AntiFraudOverdueCaseRecord antiFraudOverdueCaseRecord = antiFraudOverdueCaseModel.findByUserId(userId);
      if (Objects.isNull(antiFraudOverdueCaseRecord)) {
        AntiFraudOverdueCaseRecord overdueCaseRecord = antiFraudOverdueCaseModel.insert(userId, loanAccountRecord.getId());
        antiFraudOverdueCaseLogModel.insert(overdueCaseRecord.getId(), collectionCaseId, userId, loanAccountRecord.getId(), collectionCaseTimeCreated);
        return true;
      }

      AntiFraudOverdueCaseStatus oldStatus = AntiFraudOverdueCaseStatus.valueOf(antiFraudOverdueCaseRecord.getReviewStatus());
      AntiFraudOverdueCaseStatus newStatus = oldStatus;

      if (oldStatus == AntiFraudOverdueCaseStatus.REVIEWED) {
        newStatus = AntiFraudOverdueCaseStatus.REVIEWED_UPDATED;
      }
      antiFraudOverdueCaseLogModel.insert(antiFraudOverdueCaseRecord.getId(), collectionCaseId, userId, loanAccountRecord.getId(), collectionCaseTimeCreated);
      antiFraudOverdueCaseModel.updateStatus(antiFraudOverdueCaseRecord, newStatus);
      return true;
    });
  }

  public void checkAntiFraudNikBlackList(Long userId,String deviceToken,String idNo) {
    if (hitAntiFraudNikBlackList(idNo)) {
      try {
        DwLogUtil.newLog(LogBusinessType.ANTI_FRAUD_NIK_BLACK_LIST, AntiFraudBlackListLogVo.builder().userId(userId).deviceToken(deviceToken).idNo(idNo).createTime(Clock.now()).build());
      } catch (Exception e) {
        log.warn("log merge account failed reason failed.", e);
      }
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("您的KTP不符合要求！"));
    }
  }

  private boolean hitAntiFraudNikBlackList(String idNo) {
    return cashLoanConfig.getAntiFraudNikBlackList().contains(idNo);
  }
}
