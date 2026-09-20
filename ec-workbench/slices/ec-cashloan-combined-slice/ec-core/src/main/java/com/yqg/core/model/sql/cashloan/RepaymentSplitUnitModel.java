package com.yqg.core.model.sql.cashloan;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.RepaymentSplitUnit;
import com.yqg.core.model.generated.tables.records.RepaymentSplitUnitRecord;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.RepaySplitUnitVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RepaymentSplitUnitModel extends YqgBaseModel {

  private static final RepaymentSplitUnit TABLE = Tables.REPAYMENT_SPLIT_UNIT;

  public RepaymentSplitUnitRecord init(RepaySplitUnitVO repaySplitUnitVO) {
    long now = Clock.now();
    RepaymentSplitUnitRecord record = create().newRecord(TABLE);
    record.setUserId(repaySplitUnitVO.getUserId());
    record.setDeductType(repaySplitUnitVO.deductType.name());
    record.setAmount(repaySplitUnitVO.getAmount());
    record.setTranNo(repaySplitUnitVO.getTransNo());
    record.setRepayStatus(repaySplitUnitVO.repayStatus.code);
    record.setTimeCreated(now);
    record.setTimeUpdated(now);
    return record;
  }

  public RepaymentSplitUnitRecord selectForUpdateOrThrowByTransNoAndType(String tranNo, UnionRepaymentType unionRepaymentType) {
    RepaymentSplitUnitRecord record = create()
        .selectFrom(TABLE)
        .where(TABLE.TRAN_NO.eq(tranNo))
        .and(TABLE.DEDUCT_TYPE.eq(unionRepaymentType.name()))
        .forUpdate()
        .fetchOne();
    if (record == null) {
      throw EcException.error("RepaymentSplitUnit record is not exist, tranNo is {}", tranNo);
    }
    return record;
  }

  public void batchInsert(List<RepaySplitUnitVO> repaySplitUnitVOList) {
    List<RepaymentSplitUnitRecord> initRecords = new ArrayList<>();
    repaySplitUnitVOList.forEach(repaySplitUnitVO -> {
      initRecords.add(init(repaySplitUnitVO));
    });
    create().batchInsert(initRecords).execute();
  }

  public void batchUpdate(List<RepaymentSplitUnitRecord> records) {
    create().batchUpdate(records).execute();
  }

  public List<RepaymentSplitUnitRecord> fetchByTransNo(String transNo) {
    return create().selectFrom(TABLE)
        .where(TABLE.TRAN_NO.eq(transNo))
        .fetch();
  }

  public void updateStatusByRecord(RepaymentSplitUnitRecord record, ProcessStatus status) {
    record.setRepayStatus(status.code);
    record.setTimeUpdated(Clock.now());
    record.update();
  }

  public RepaymentSplitUnitRecord fetchById(Long id) {
    return create().selectFrom(TABLE)
        .where(TABLE.ID.eq(id))
        .fetchOne();
  }

  public List<RepaymentSplitUnitRecord> fetchByIds(List<Long> ids) {
    return create().selectFrom(TABLE)
        .where(TABLE.ID.in(ids))
        .fetch();
  }
}
