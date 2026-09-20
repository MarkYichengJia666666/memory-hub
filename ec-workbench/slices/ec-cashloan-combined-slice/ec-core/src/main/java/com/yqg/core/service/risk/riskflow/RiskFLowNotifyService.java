package com.yqg.core.service.risk.riskflow;

import com.google.common.collect.Lists;
import com.yqg.core.model.core.RevType;
import com.yqg.core.model.generated.tables.records.AdminUserRecord;
import com.yqg.core.model.sql.adminuser.AdminUserModel;
import com.yqg.core.service.event.CommonEventService;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import com.yqg.core.service.tool.EmailService;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.enums.CommonEventCustomerType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.SysEnvironment;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskFLowNotifyService {

  private static final String EMAIL_RECEIVERS_SPLITTER = ";|,";
  private static final String CHANGE_AUTO_RISK_FLOW_TITLE = "【ec】[%s] 风控变更通知";
  private static final String CHANGE_AUTO_RISK_FLOW_MESSAGE = "[%s]在%s %s 风控规则：[%s] <br> 详情:<br> %s";
  private static final String BATCH_SET_OBSERVER_MESSAGE = "[%s]在%s 变更观察规则集：<br>涉及riskFlowId: %s <br>观察规则集Id: %s";

  @Autowired
  private EmailService emailService;
  @Autowired
  private ISiteVars ecSiteVars;
  @Autowired
  private AdminUserModel adminUserModel;
  @Autowired
  private CommonEventService commonEventService;

  public void sendRiskFlowOperationByEmail(RiskFlowVO riskFlowVO, Long userOpt, RevType revType) {
    String address = ecSiteVars.getString("admin.autoRiskFlowChangeEmailNotification", "");
    if (StringUtils.isNotEmpty(address)) {
      String title = String.format(CHANGE_AUTO_RISK_FLOW_TITLE, SysEnvironment.getEnv());
      AdminUserRecord userRecord = adminUserModel.findById(userOpt);
      String riskFlowStr = riskFlowVO == null ? "" : JsonUtils.toString(riskFlowVO);
      String message = String.format(CHANGE_AUTO_RISK_FLOW_MESSAGE,
          userRecord != null ? userRecord.getFullName() : "未知用户,userID:" + userOpt,
          Clock.date(Clock.now(), EcTimeZone.getDefaultTimeZone()).toString("yyyy年MM月dd日HH:mm"),
          revType.name(),
          riskFlowVO.name,
          riskFlowStr.length() <= 10000 ? riskFlowStr : riskFlowStr.substring(0, 10000)
      );
      commonEventService.insert(title, message, CommonEventCustomerType.RISK_FLOW_CHANGE_LOG);
      emailService.asyncSend(Lists.newArrayList(address.split(EMAIL_RECEIVERS_SPLITTER)), title, message);
    }
  }

  public void sendRiskFlowBatchSetObserverByEmail(List<Long> riskFlowIds, List<Long> ruleSetIds, Long userOpt) {
    String address = ecSiteVars.getString("admin.autoRiskFlowChangeEmailNotification", "");
    if (StringUtils.isNotEmpty(address)) {
      String title = String.format(CHANGE_AUTO_RISK_FLOW_TITLE, SysEnvironment.getEnv());
      AdminUserRecord userRecord = adminUserModel.findById(userOpt);
      String message = String.format(BATCH_SET_OBSERVER_MESSAGE,
          userRecord != null ? userRecord.getFullName() : "未知用户,userID:" + userOpt,
          Clock.date(Clock.now(), EcTimeZone.getDefaultTimeZone()).toString("yyyy年MM月dd日HH:mm"),
          JsonUtils.toString(riskFlowIds),
          JsonUtils.toString(ruleSetIds)
      );
      emailService.asyncSend(Lists.newArrayList(address.split(EMAIL_RECEIVERS_SPLITTER)), title, message);
    }
  }
}
