package com.yqg.core.service.cashloan.repay;

import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.RepaymentSplitUnitRecord;
import com.yqg.core.model.sql.cashloan.RepaymentSplitUnitModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.JbpRepaymentEventEntity;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentPlanVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import com.yqg.core.service.kafka.producer.KafkaMessageService;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class JbpRepaymentHandler extends UnionRepaymentHandler {

  @Autowired
  private KafkaMessageService kafkaMessageService;
  @Autowired
  private RepaymentSplitUnitModel repaymentSplitUnitModel;

  @Override
  public UnionRepaymentType getSupportedType() {
    return UnionRepaymentType.JBP;
  }

  // 模板方法：处理请求
  @RunInTransaction
  public PaymentProcessResult handleRepayment(UnionRepaymentVO unionRepaymentVO, Long repayUnitId, DeductIntentionVO deductIntentionVO) {
    if (Objects.isNull(deductIntentionVO)) {
      throw EcException.error("Cannot run JbpRepaymentHandler when deductIntentionVO is null! TransId:{},repayUnitId:{}", unionRepaymentVO.transNo, repayUnitId);
    }
    kafkaMessageService.schedule(
        KafkaTopicGenerator.getTopic(KafkaTopic.EC_COMBINED_REPAY_EVENT), unionRepaymentVO.transNo, buildJbpRepayEvent(unionRepaymentVO, deductIntentionVO));
    RepaymentSplitUnitRecord record = repaymentSplitUnitModel.fetchById(repayUnitId);
    repaymentSplitUnitModel.updateStatusByRecord(record, ProcessStatus.PROCESSED);
    return PaymentProcessResult.from(ProcessStatus.PROCESSED, null);
  }

  private String buildJbpRepayEvent(UnionRepaymentVO unionRepaymentVO, DeductIntentionVO deductIntentionVO) {
    List<UnionRepaymentPlanVO> planVOS = deductIntentionVO.deductPlan.stream().filter(vo -> vo.unionRepaymentType == UnionRepaymentType.JBP).collect(Collectors.toList());
    EcAsserts.assertTrue(planVOS.size() == 1, "UnionRepaymentVO size can only be 1! TransNo:{}", unionRepaymentVO.transNo);
    UnionRepaymentPlanVO planVO = planVOS.get(0);
    EcAsserts.assertTrue(planVO.businessIds.size() == 1, "UnionRepaymentPlanVO size can only be 1! TransNo:{}", unionRepaymentVO.transNo);
    JbpRepaymentEventEntity entity = JbpRepaymentEventEntity.build(unionRepaymentVO, planVO.businessIds.get(0));
    return JsonUtils.toString(entity);
  }
}
