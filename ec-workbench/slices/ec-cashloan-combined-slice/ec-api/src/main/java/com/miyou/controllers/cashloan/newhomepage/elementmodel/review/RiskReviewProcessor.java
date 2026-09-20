package com.miyou.controllers.cashloan.newhomepage.elementmodel.review;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.homepage.config.ApiRequestPath;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CountDownTimeElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ProgressBarElement;
import com.yqg.core.service.cashloan.auth.BindCardDelayService;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpContext;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpResult;
import com.yqg.core.service.cashloan.creditgain.CreditGainOriginTraceService;
import com.yqg.core.service.cashloan.creditgain.CreditGainPerceptionExpService;
import com.yqg.core.service.cashloan.creditgain.CreditGainRiskTraceDigest;
import com.yqg.core.service.cashloan.creditgain.CreditGainScene;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageWholeProcessTool;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.userflow.domain.loan.model.amount.CreditGainReviewCard;
import com.yqg.core.userflow.domain.loan.service.amount.ICreditGainDisplayService;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskReviewProcessor extends AbstractPageCardV3Processor {
  @Autowired
  private BindCardDelayService bindCardDelayService;
  @Autowired
  private HomepageWholeProcessTool homepageWholeProcessTool;
  @Autowired
  private CreditGainPerceptionExpService creditGainPerceptionExpService;
  @Autowired
  private CreditGainOriginTraceService creditGainOriginTraceService;
  @Autowired
  private IUserInfoService userInfoService;
  @Autowired
  private ICreditGainDisplayService creditGainDisplayService;

  private final static List<TT> AUTO_REVIEW_BAR_NODES = Lists.newArrayList(TT.gen("申请已提交"),TT.gen("信息已加密"),TT.gen("审核完成"));

  @Override
  public void process(PageCardV3VO cardV3VO, HomePageContext homePageContext) {
    // 当前审核包含二次风控和一次风控，将订单二次风控的排除
    if (orderRiskCheck(homePageContext.getUserCashLoanOrderContext().getLatestOrderVO())) {
      return;
    }
    //绑卡后置用户在完件流程跳过了绑卡环节并且还未绑卡或当前没有绑卡的用户，展示跳转绑卡按钮
    if (bindCardDelayService.isMissingBankCard(homePageContext.getUserId(), homePageContext.getSdkType())) {
      addBindCardElements(cardV3VO);
      return;
    }
    if (homepageWholeProcessTool.showDownloadButton()) {
      // 通用元素
      addCommonElements(cardV3VO);
    }
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    if (creditsInfoVO.isInManualReviewStatus()) {
      //人审
      addManualReviewElements(cardV3VO);
      return;
    }
    if (Clock.now() - creditsInfoVO.getTimeUpdated() < Clock.MILLS_PER_MINUTE) {
      // 机审 1min之内
      addAutoReviewWithinOneMinuteElements(cardV3VO, homePageContext, creditsInfoVO);
    } else {
      // 机审 1min之外
      addAutoReviewBeyondOneMinuteElements(cardV3VO, homePageContext);
    }
  }

  private boolean orderRiskCheck(CashLoanOrderVO latestOrderVO) {
    if (latestOrderVO == null) {
      return false;
    }
    if (latestOrderVO.status == CashLoanOrderStatus.RESERVE) {
      return true;
    }
    return false;
  }

  private void addBindCardElements(PageCardV3VO cardV3VO) {
    cardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("立即填写您的账号"))
        .action(new RedirectAction(RedirectAction.LinkActionParam.builder().redirectUrl(elementConfig.getRedirectPage(NativePathConstant.BIND_CARD_PAGE)).build()))
        .build());
    cardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MainCardElementId.MAIN_ICON)
        .imageUrl(elementConfig.getReviewElementConfig(MainCardElementId.MAIN_ICON.name(), "bindCardIcon"))
        .build());
    cardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("审核中，预计1分钟内出额度"))
        .build());
  }

  private void addCommonElements(PageCardV3VO cardV3VO) {
    cardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("查看审核进度"))
        .action(new RedirectAction(RedirectAction.LinkActionParam.builder().redirectUrl(elementConfig.getRedirectPage(NativePathConstant.SUB_HOME_REVIEW_PAGE)).build()))
        .build());
    cardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MainCardElementId.MAIN_ICON)
        .imageUrl(elementConfig.getReviewElementConfig(MainCardElementId.MAIN_ICON.name(), "reviewIcon"))
        .build());
  }

  private void addManualReviewElements(PageCardV3VO cardV3VO) {
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.TITLE)
            .type(ElementType.TEXT)
            .text(TT.gen("工作人员审核中"))
            .build());
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.SUB_TITLE)
            .type(ElementType.TEXT)
            .text(TT.gen("我们将在24小时内联系您，请注意来电并及时接听"))
            .build());
  }

  private void addAutoReviewWithinOneMinuteElements(PageCardV3VO cardV3VO, HomePageContext homePageContext, LoanUserCreditsInfoVO creditsInfoVO) {
    // 源头 trace 在此解析一次后传下去：判组与选文案同源，且回溯最多查一次库
    LoanUserRiskTraceVO originTrace = creditGainOriginTraceService.resolveOriginTrace(
        homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO());
    if (hitCreditGainReviewExperiment(homePageContext, originTrace)) {
      addCreditGainReviewElements(cardV3VO, homePageContext, creditsInfoVO, originTrace);
      return;
    }
    addOnlineAutoReviewWithinOneMinuteElements(cardV3VO, homePageContext, creditsInfoVO);
  }

  /**
   * 线上既有「机审 1 分钟内」审核中样式（未命中实验组时保持不变）。
   */
  private void addOnlineAutoReviewWithinOneMinuteElements(PageCardV3VO cardV3VO, HomePageContext homePageContext, LoanUserCreditsInfoVO creditsInfoVO) {
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.TITLE)
            .type(ElementType.TEXT)
            .text(TT.gen("额度申请审核中"))
            .build());
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.AUTH_REVIEW_INFO)
            .elementParam(ProgressBarElement.ProgressBarParam.builder()
                .nodes(AUTO_REVIEW_BAR_NODES)
                .currentNode(AUTO_REVIEW_BAR_NODES.get(0))
                .nextNode(AUTO_REVIEW_BAR_NODES.get(AUTO_REVIEW_BAR_NODES.size() - 1))
                .passTime(Clock.now() - homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO().timeCreated)
                .waitingSecond(Clock.SECOND_PER_MINUTE)
                .apiRequestPath(ApiRequestPath.REVIEW)
                .build())
            .build());
    // 线上既有态保持前缀「预计」不变（NFR-002 未命中实验组逐字段等价旧链路）
    addCountDownTimeElement(cardV3VO, creditsInfoVO, TT.gen("预计"));
  }

  /**
   * TAPD-369589 实验组审核中新样式：文案 / 节点 / 配图由 {@link ICreditGainDisplayService} 按场景给出，
   * 本方法只做元素组装。
   *
   * <p>新样式无主按钮，故移除父类 {@code addCommonElements} 下发的主按钮（不存在时 delete 亦幂等），
   * 并覆盖其 reviewIcon 为本需求额度审核图。
   */
  private void addCreditGainReviewElements(PageCardV3VO cardV3VO, HomePageContext homePageContext,
      LoanUserCreditsInfoVO creditsInfoVO, LoanUserRiskTraceVO originTrace) {
    CreditGainReviewCard reviewCard = creditGainDisplayService.getReviewCard(originTrace);
    LoanUserRiskTraceVO latestTrace = homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO();

    cardV3VO.deleteFromElementMap(MainCardElementId.MAIN_BUTTON);
    cardV3VO.forceAddElementForMainCard(ElementBuilder.image()
        .id(MainCardElementId.MAIN_ICON)
        .imageUrl(reviewCard.getIconUrl())
        .build());

    List<TT> barNodes = reviewCard.getProgressNodes();
    log.debug("[CreditGainReview] render new review card, userId={}, scene={}, originRiskTrace={}",
        homePageContext.getUserId(), reviewCard.getScene(), CreditGainRiskTraceDigest.of(originTrace));

    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.TITLE)
            .type(ElementType.TEXT)
            .text(reviewCard.getTitle())
            .build());
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.AUTH_REVIEW_INFO)
            .elementParam(ProgressBarElement.ProgressBarParam.builder()
                .nodes(barNodes)
                .currentNode(barNodes.get(0))
                .nextNode(barNodes.get(barNodes.size() - 1))
                // 已过时长取最新 trace 而非源头：被拒回捞时源头早于回捞开始，用源头会让进度条直接冲到头。
                .passTime(latestTrace == null ? 0L : Clock.now() - latestTrace.timeCreated)
                .waitingSecond(Clock.SECOND_PER_MINUTE)
                .apiRequestPath(ApiRequestPath.REVIEW)
                .progressBarStyle(ProgressBarElement.ProgressBarStyle.CREDIT_GAIN)
                .build())
            .build());
    addCountDownTimeElement(cardV3VO, creditsInfoVO, reviewCard.getCountDownPrefix());
  }

  /**
   * 审核中秒级倒计时 Element（通用/还款/线上样式共用同一机制：基准 {@code timeUpdated}，下发剩余秒 = 60 − 已过秒）。
   *
   * @param prefixText 倒计时前缀文案：实验组新样式传「预计还需」（PRD），线上既有态传「预计」保持不变
   */
  private void addCountDownTimeElement(PageCardV3VO cardV3VO, LoanUserCreditsInfoVO creditsInfoVO, TT prefixText) {
    long passTime = (Clock.now() - creditsInfoVO.getTimeUpdated()) / Clock.MILLS_PER_SECOND;
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.COUNT_DOWN_TIME)
            .type(ElementType.COUNT_DOWN_TIME)
            .text(prefixText)
            .elementParam(CountDownTimeElement.TimeElementParam.builder()
                .timeUnit(TimeUnit.SECONDS)
                .time(Clock.SECOND_PER_MINUTE - passTime)
                .build())
            .build());
  }

  /**
   * 是否命中「增强额度获得感」审核中相关实验组。
   *
   * <p>从 {@link HomePageContext} 提取判组入参（渠道/版本门槛 + 人群/场景），
   * 交 {@link CreditGainPerceptionExpService#resolve} 短路路由到唯一目标实验；任何异常 fail-open 返回 false。
   */
  private boolean hitCreditGainReviewExperiment(HomePageContext homePageContext, LoanUserRiskTraceVO originTrace) {
    try {
      CreditGainExpContext expContext = buildExpContext(homePageContext, originTrace);
      CreditGainExpResult result = creditGainPerceptionExpService.resolve(expContext);
      // 一条日志把「判组输入（场景/人群 + 场景所依据的完整 trace）+ 路由结果（expKey/是否实验组）」打全：
      // 只打结论就分不清是场景判错还是分流没中；只打 triggerSource 又分不清这条 trace 是不是本次那条。
      log.debug("[CreditGainReview] resolve experiment, userId={}, scene={}, reloan={}, hitExpKey={}, "
              + "experimentGroup={}, originRiskTrace={}",
          homePageContext.getUserId(), expContext.getScene(), expContext.isReloan(),
          result.getHitExpKey(), result.isExperimentGroup(), CreditGainRiskTraceDigest.of(originTrace));
      return result.isExperimentGroup();
    } catch (Exception e) {
      log.warn("[CreditGainReview] resolve experiment failed, fail-open to online style, userId={}",
          homePageContext.getUserId(), e);
      return false;
    }
  }

  /**
   * 组装判组入参：场景由<b>源头</b> trace 推导（{@link CreditGainScene#resolveByTrace}），
   * <b>不得</b>写死。审核中首页状态 {@code IN_REVIEW} 既可能来自完件首授（场景①）也可能来自失效后戳额
   * （场景③），写死成「首次授信」会让失效戳额的首贷用户恒判到实验一——线上曾因此在失效大卡刚命中实验三后
   * 4 秒，审核中就 {@code not experiment} 退回老样式。
   *
   * <p>人群（是否复贷）改用领域服务 {@link IUserInfoService#isReloanUserByAccountId}（打款成功次数&gt;0 为复贷），
   * 替换已 Deprecated 的 {@code HomepageUserParamsVO.firstLoan}；场景与人群解耦，入组由服务端「场景 + 人群」双条件裁决。
   */
  private CreditGainExpContext buildExpContext(HomePageContext homePageContext, LoanUserRiskTraceVO originTrace) {
    UserDeviceContextVO deviceContext = homePageContext.getUserDeviceContextVO();
    return CreditGainExpContext.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(deviceContext == null ? null : deviceContext.getDeviceToken())
        .sourceType(deviceContext == null ? null : deviceContext.getSourceType())
        .build(deviceContext == null ? null : deviceContext.getBuild())
        .reloan(userInfoService.isReloanUserByAccountId(homePageContext.getLoanAccountId()))
        .scene(CreditGainScene.resolveByTrace(originTrace))
        .build();
  }

  /**
   * 机审超 1 分钟态（US1-4）：命中审核中相关实验组时，主文案换成与「1 分钟内」态相同的场景文案（完件 /
   * 还款 / 其余失效重测），原文案「系统加急处理中，晚点再来查看吧」下沉为副文案；未命中 / fail-open
   * 保持线上既有值「额度审批正在加速处理，请耐心等待」逐字段不变、不下发副文案（NFR-002）。
   *
   * <p>超 1 分钟态仍不下发进度条 / 倒计时（US1-4 不新增这些 Element）。
   */
  private void addAutoReviewBeyondOneMinuteElements(PageCardV3VO cardV3VO, HomePageContext homePageContext) {
    LoanUserRiskTraceVO originTrace = creditGainOriginTraceService.resolveOriginTrace(
        homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO());
    if (!hitCreditGainReviewExperiment(homePageContext, originTrace)) {
      cardV3VO.addElementForMainCard(
          ElementBuilder.progressBar()
              .id(MainCardElementId.TITLE)
              .type(ElementType.TEXT)
              .text(TT.gen("额度审批正在加速处理，请耐心等待"))
              .build());
      return;
    }
    CreditGainReviewCard reviewCard = creditGainDisplayService.getReviewCard(originTrace);
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.TITLE)
            .type(ElementType.TEXT)
            .text(reviewCard.getTitle())
            .build());
    cardV3VO.addElementForMainCard(
        ElementBuilder.progressBar()
            .id(MainCardElementId.SUB_TITLE)
            .type(ElementType.TEXT)
            .text(TT.gen("系统加急处理中，晚点再来查看吧"))
            .build());
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.REVIEW_FOR_RISK;
  }
}
