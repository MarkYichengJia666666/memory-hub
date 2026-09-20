package com.yqg.core.service.cashloan;

import com.google.common.collect.ImmutableMap;
import com.yqg.core.configure.EcKafkaConfig;
import com.yqg.core.model.mongo.MongoKafkaMessageModel;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.core.service.cashloan.event.CashLoanEvent;
import com.yqg.core.service.cashloan.event.CashLoanEventType;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import com.yqg.core.service.kafka.producer.IKafkaMessageService;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by xiahonggao on 2017/04/05.
 * <p>
 * All cash loan events are partitioned by account id.
 */
@Slf4j
@Service
public class CashLoanEventService {

  @Autowired
  private IKafkaMessageService messageService;
  @Autowired
  private MongoKafkaMessageModel mongoKafkaMessageModel;
  @Autowired
  private EcKafkaConfig ecKafkaConfig;

  private static final Map<CashLoanOrderStatus, CashLoanEventType> ORDER_TO_EVENT =
      ImmutableMap.<CashLoanOrderStatus, CashLoanEventType>builder()
          .put(CashLoanOrderStatus.RESERVE, CashLoanEventType.ORDER_RESERVED)
          .put(CashLoanOrderStatus.INIT, CashLoanEventType.ORDER_INITIALIZED)
          .put(CashLoanOrderStatus.READY, CashLoanEventType.ORDER_READY)
          .put(CashLoanOrderStatus.REJECT, CashLoanEventType.ORDER_REJECTED)
          .put(CashLoanOrderStatus.COMPLETE, CashLoanEventType.ORDER_COMPLETED)
          .put(CashLoanOrderStatus.CHECK, CashLoanEventType.ORDER_CHECK)
          .build();

  private static final Map<CashLoanRepaymentStatus, CashLoanEventType> REPAYMENT_TO_NEW_EVENT =
      ImmutableMap.<CashLoanRepaymentStatus, CashLoanEventType>builder()
          .put(CashLoanRepaymentStatus.SUCCEED, CashLoanEventType.REPAYMENT_SUCCEED_NEW)
          .put(CashLoanRepaymentStatus.FAIL, CashLoanEventType.REPAYMENT_FAILED_NEW)
          .build();

  public void publishOrderStatusEvent(CashLoanOrderVO orderVO) {
    CashLoanEvent event = CashLoanEvent.genOrderEvent(ORDER_TO_EVENT.get(orderVO.status), orderVO);
    publishEvent(orderVO.accountId, event);
  }

  public void publishOrderOverdue(CashLoanOrderVO orderVO) {
    CashLoanEvent event = CashLoanEvent.genOrderEvent(CashLoanEventType.ORDER_OVERDUE, orderVO);
    publishEvent(orderVO.accountId, event);
  }

  public void publishOrderCalcPostInterest(CashLoanOrderVO orderVO) {
    CashLoanEvent event = CashLoanEvent.genOrderEvent(CashLoanEventType.ORDER_CALC_POST_INTEREST, orderVO);
    publishEvent(orderVO.accountId, event);
  }

  public void publishRepaymentStatusNewEvent(List<Long> orderIds, List<OrderInstalment> orderInstalmentList, RepaymentVO repaymentVO) {
    CashLoanEvent event = CashLoanEvent.genRepayEvent(REPAYMENT_TO_NEW_EVENT.get(repaymentVO.status), orderIds, orderInstalmentList, repaymentVO);
    publishEvent(repaymentVO.accountId, event);
  }

  private void publishEvent(Long accountId, CashLoanEvent event) {
    String message = JsonUtils.toString(event);
    //message长度过大时，存入mongo
    try {
      Integer messageByteLength = message.getBytes("utf-8").length;
      if (messageByteLength > ecKafkaConfig.getByteLengthForStoreMessageInMongo()) {
        String objectId = mongoKafkaMessageModel.insert(message);
        message = JsonUtils.toString(CashLoanEvent.fromMongo(objectId));
        log.info("message too long, so use mongo store, length: {}", messageByteLength);
      }
    } catch (Exception e) {
      throw EcException.error("error when get message length and insert into mongo, accountId: {}", accountId, e);
    }
    messageService.schedule(
        KafkaTopicGenerator.getTopic(KafkaTopic.CASH_LOAN_EVENT),
        accountId.toString(),
        message
    );
  }

}
