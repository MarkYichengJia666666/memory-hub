package com.miyou.controllers.cashloan.newhomepage.elementmodel.creditgain;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpSceneType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.service.cashloan.creditgain.CreditGainBeforeRepaySnapshotService;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpContext;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpResult;
import com.yqg.core.service.cashloan.creditgain.CreditGainOriginTraceService;
import com.yqg.core.service.cashloan.creditgain.CreditGainPerceptionExpService;
import com.yqg.core.service.cashloan.creditgain.CreditGainRepayTriggerSources;
import com.yqg.core.service.cashloan.creditgain.CreditGainRiskTraceDigest;
import com.yqg.core.service.cashloan.creditgain.CreditGainScene;
import com.yqg.core.service.cashloan.creditgain.CreditGainSeenService;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.userflow.domain.loan.model.amount.CreditGainPopup;
import com.yqg.core.userflow.domain.loan.service.amount.ICreditGainDisplayService;
import com.yqg.core.userflow.domain.user.model.UserAmountInfo;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 还款升额首页弹层 Processor（增强额度获得感 US4，TAPD-369589）。
 *
 * <p>仅挂结果态 Selector。命中「实验二实验组（复贷含续借）+ 还款触发 + 剩余可借高于还款前快照 + 当日频控未消费」
 * 时下发弹层 Element，否则不弹（走 US2 中性样式）。频控为 userId + 印尼自然日维度，一天至多一次。
 * 任何异常 fail-open：不弹层、不阻断结果态渲染。
 */
@Slf4j
@Component
public class CreditGainUpgradePopupProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private CreditGainPerceptionExpService creditGainPerceptionExpService;

  @Autowired
  private CreditGainSeenService creditGainSeenService;

  @Autowired
  private CreditGainOriginTraceService creditGainOriginTraceService;

  @Autowired
  private CreditGainBeforeRepaySnapshotService creditGainBeforeRepaySnapshotService;

  @Autowired
  private IUserInfoService userInfoService;

  @Autowired
  private ICreditGainDisplayService creditGainDisplayService;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    try {
      if (!shouldRenderUpgradePopup(homePageContext)) {
        return;
      }
      pageCardV3VO.addElementForMainCard(buildUpgradePopupElement());
    } catch (Exception e) {
      log.warn("[CreditGainUpgradePopup] render failed, fail-open to no popup, userId={}",
          homePageContext == null ? null : homePageContext.getUserId(), e);
    }
  }

  /**
   * 是否下发升额弹层，命中时同时消费当日频控标记。每个短路点打 debug 日志，供排查「还款升额但没弹」卡在哪一步。
   */
  private boolean shouldRenderUpgradePopup(HomePageContext homePageContext) {
    Long userId = homePageContext.getUserId();
    Long accountId = homePageContext.getLoanAccountId();
    // 先过本地廉价门槛、实验入组 RPC 放最后：只有真会弹的用户才入组，避免污染实验样本。
    // 取源头 trace：还款测额被前置风控拒后由回捞给额时，最新那条是 PRE_RISK_REJECT，直接判会认成「非还款」漏弹。
    LoanUserRiskTraceVO originTrace = creditGainOriginTraceService.resolveOriginTrace(
        homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO());
    if (originTrace == null || originTrace.id == null) {
      log.debug("[CreditGainUpgradePopup] skip: no risk trace, userId={}, accountId={}, originRiskTrace={}",
          userId, accountId, CreditGainRiskTraceDigest.of(originTrace));
      return false;
    }
    if (!CreditGainRepayTriggerSources.isRepaySemantic(originTrace.triggerSource)) {
      log.debug("[CreditGainUpgradePopup] skip: triggerSource not repay semantic, userId={}, accountId={}, "
          + "originRiskTrace={}", userId, accountId, CreditGainRiskTraceDigest.of(originTrace));
      return false;
    }
    // 仅日志用，标识本次获额事件；频控走 userId + 自然日维度，不按事件区分。
    String eventKey = String.valueOf(originTrace.id);
    if (!isCreditUpgraded(accountId, userId, eventKey)) {
      return false;
    }
    if (!hitReloanCreditGainExperiment(homePageContext)) {
      return false;
    }
    if (homePageLeaveJudgeTool.willLeaveHomePage(homePageContext)) {
      log.debug("[CreditGainUpgradePopup] skip: will leave home page, userId={}, accountId={}", userId, accountId);
      return false;
    }
    boolean consumed = creditGainSeenService.tryConsumeUpgradePopup(userId);
    if (!consumed) {
      log.debug("[CreditGainUpgradePopup] skip: seen quota already consumed today, userId={}, accountId={}",
          userId, accountId);
    }
    return consumed;
  }

  /**
   * 是否命中实验二（复贷含续借）实验组；异常 fail-open 返回 false。首贷戳额路由到实验三，key 不等天然不弹。
   */
  private boolean hitReloanCreditGainExperiment(HomePageContext homePageContext) {
    Long userId = homePageContext.getUserId();
    try {
      CreditGainExpResult result = creditGainPerceptionExpService.resolve(buildExpContext(homePageContext));
      boolean hit = result.isExperimentGroup()
          && UserFlowExperimentEnum.CREDIT_GAIN_RELOAN_RECREDIT.getKey().equals(result.getHitExpKey());
      if (!hit) {
        log.debug("[CreditGainUpgradePopup] skip: experiment not hit, userId={}, isExperimentGroup={}, "
            + "hitExpKey={}, expectedKey={}", userId, result.isExperimentGroup(), result.getHitExpKey(),
            UserFlowExperimentEnum.CREDIT_GAIN_RELOAN_RECREDIT.getKey());
      }
      return hit;
    } catch (Exception e) {
      log.warn("[CreditGainUpgradePopup] resolve experiment failed, fail-open to false, userId={}", userId, e);
      return false;
    }
  }

  /**
   * 升额判定：当前与快照同取 {@link UserAmountInfo#getRemainingCreditsForVirtual()}，严格大于才算升额；
   * 缺快照 / 缺当前值 / 相等 / 更小一律不弹。
   */
  private boolean isCreditUpgraded(Long accountId, Long userId, String traceId) {
    BigDecimal beforeRepayRemaining = creditGainBeforeRepaySnapshotService.getBeforeRepayRemaining(accountId);
    if (beforeRepayRemaining == null) {
      log.debug("[CreditGainUpgradePopup] skip: no before-repay snapshot, userId={}, accountId={}, traceId={}",
          userId, accountId, traceId);
      return false;
    }
    UserAmountInfo currentAmountInfo = userInfoService.getUserAmountInfoByAccountId(accountId);
    BigDecimal currentRemaining = currentAmountInfo == null ? null : currentAmountInfo.getRemainingCreditsForVirtual();
    if (currentRemaining == null) {
      log.debug("[CreditGainUpgradePopup] skip: no current remaining credits, userId={}, accountId={}, "
          + "traceId={}, beforeRepayRemaining={}", userId, accountId, traceId, beforeRepayRemaining);
      return false;
    }
    boolean upgraded = BigDecimalHelper.greaterThan(currentRemaining, beforeRepayRemaining);
    if (!upgraded) {
      log.debug("[CreditGainUpgradePopup] skip: not upgraded, userId={}, accountId={}, traceId={}, "
          + "beforeRepayRemaining={}, currentRemaining={}", userId, accountId, traceId, beforeRepayRemaining,
          currentRemaining);
    } else {
      log.debug("[CreditGainUpgradePopup] upgraded, userId={}, accountId={}, traceId={}, "
          + "beforeRepayRemaining={}, currentRemaining={}", userId, accountId, traceId, beforeRepayRemaining,
          currentRemaining);
    }
    return upgraded;
  }

  /**
   * 组装判组入参：场景恒为 {@link CreditGainScene#REPAY_RECREDIT}——调用方已先判还款语义，走到这里必然是还款测额。
   */
  private CreditGainExpContext buildExpContext(HomePageContext homePageContext) {
    UserDeviceContextVO deviceContext = homePageContext.getUserDeviceContextVO();
    return CreditGainExpContext.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(deviceContext == null ? null : deviceContext.getDeviceToken())
        .sourceType(deviceContext == null ? null : deviceContext.getSourceType())
        .build(deviceContext == null ? null : deviceContext.getBuild())
        .reloan(userInfoService.isReloanUserByAccountId(homePageContext.getLoanAccountId()))
        .scene(CreditGainScene.REPAY_RECREDIT)
        .build();
  }

  /**
   * 构建弹层 Element：图片 Element 承载配图 + {@link PopUpAction}，前端按 {@code action.params} 弹层。
   * {@code sceneType} 恒为 US4 场景，供前端区分不同弹层，展示时长亦按该场景取运营配置。
   */
  private IElement buildUpgradePopupElement() {
    CreditGainPopup popup = creditGainDisplayService.getUpgradePopup();
    PopUpAction.PopUpParam popUpParam = PopUpAction.PopUpParam.builder()
        .title(popup.getTitle())
        .content(popup.getContent())
        .imageUrl(popup.getImageUrl())
        .redirectUrl(popup.getRedirectUrl())
        .sceneType(PopUpSceneType.CREDIT_GAIN_UPGRADE_POPUP)
        .displayDurationSeconds(popup.getDisplayDurationSeconds())
        .build();
    return ElementBuilder.image()
        .id(MainCardElementId.UPGRADE_POPUP)
        .imageUrl(popup.getImageUrl())
        .action(new PopUpAction(popUpParam))
        .build();
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.CREDIT_GAIN_UPGRADE_POPUP;
  }
}
