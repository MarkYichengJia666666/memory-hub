package com.miyou.controllers.cashloan.newhomepage.elementmodel.reject;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.GOTO_LINK_INFO;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.MAIN_BUTTON;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.MAIN_ICON;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.SUB_TITLE;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.TITLE;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.BLACK_000;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextStyle.BOLD;
import static com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus.REVIEWING_STATUS_LIST;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayDecision;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElementAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.translation.client.utils.TT;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 处理被拒绝用户的卡片显示逻辑
 * 主要处理以下几种情况：
 * 1. 用户没有资格重新申请（短期拒绝或永久拒绝）
 * 2. 用户有资格重新申请但尚未提交
 * 3. 用户已提交申请且正在审核中
 * 4. 用户申请已审核完成但被拒绝
 */
@Component
@Slf4j
public class RejectCardProcessor extends AbstractPageCardV3Processor {

  @Autowired
  protected HomepageV5Config homepageV5Config;

  @Autowired
  private HomepageContentTool homepageContentTool;

  @Autowired
  private SensorsService sensorsService;

  @Autowired
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;

  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    Long userId = homePageContext.getUserId();
    LoanMarketEntranceDisplayDecision loanMarketDecision =
        loanMarketEntranceDisplayStrategyService.evaluate(homePageContext, LoanMarketEntranceDisplayStrategyType.REJECT_STATUS, false);

    // 判断用户是否有资格重申
    if (loanMarketDecision.isNotQualifiedToReapply()) {
      handleNotQualifiedToReapply(pageCardV3VO, homePageContext, loanMarketDecision);
      return;
    }

    // 用户有资格重申，获取最近一次的申请记录，检查申请状态（在策略中完成）

    if (loanMarketDecision.isRejectWaitingForFirstSubmission()) {
      // 用户有资格但尚未提交申请
      handleWaitingForFirstSubmission(pageCardV3VO, homePageContext, loanMarketDecision);
      return;
    }

    if (REVIEWING_STATUS_LIST.contains(loanMarketDecision.getRiskIncreaseCreditsReviewStatus())) {
      // 申请正在审核中
      handleApplicationUnderReview(pageCardV3VO, homePageContext);
      return;
    }

    if (loanMarketDecision.isRejectPermanently()) {
      // 审核完成但被永久拒绝
      handlePermanentlyRejected(pageCardV3VO, homePageContext, loanMarketDecision);
      return;
    }

    // 未知状态
    log.info("Rejected with unknown status, userId:{}", userId);
  }

  /**
   * 处理没有资格重新申请的用户
   *
   * @param pageCardV3VO      页面卡片视图对象
   * @param homePageContext   主页上下文
   * @param loanMarketDecision 贷超入口展示策略结果（与本 processor 对应）
   */
  private void handleNotQualifiedToReapply(PageCardV3VO pageCardV3VO, HomePageContext homePageContext,
      LoanMarketEntranceDisplayDecision loanMarketDecision) {

    UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();

    Long userId = homePageContext.getUserId();
    log.info("User not qualified to reapply, userId:{}", userId);

    // 构建基础信息元素
    homepageV3CardTool.buildCommonLoanInfoElementForMainCard(pageCardV3VO, homePageContext);

    Long timeReapply = userCreditsContext.getCreditsInfoVO().timeReapply;

    // 用户授信临时被拒
    if (Objects.nonNull(timeReapply) && timeReapply > Clock.now()) {
      handleTemporaryRejection(pageCardV3VO, homePageContext, timeReapply, loanMarketDecision);
      buildSubTitleElementForMainCard(pageCardV3VO, homePageContext);
      buildSubTitleTipElementForMainCard(pageCardV3VO, homePageContext);
      return;
    }
    // 用户授信永久被拒入口，由贷超入口展示策略决定
    if (loanMarketDecision.isShowLoanMarketEntrance()) {
      addLoanMarketElementButton(pageCardV3VO, homePageContext);
    } else {
      addDisabledElementButton(pageCardV3VO);
    }
  }

  public void buildSubTitleElementForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    if (!homePageContext.getPrepareAbTestVO().secondRiskRejectDisplayStrategy.isStrategyB()) {
      return;
    }

    UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();
    if (Objects.isNull(userCreditsContext) || Objects.isNull(userCreditsContext.getCreditsInfoVO())) {
      return;
    }

    Long timeReapply = userCreditsContext.getCreditsInfoVO().timeReapply;
    String canReapplyDate = Clock.dateTimeStringFromTimestampWithLocale(timeReapply, DateFormatter.dd___MMM___yyyy, homePageContext.getSdkType().getTimeZone(), homePageContext.getSdkType().getLocale());
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
    if (hasReadyOrder) {
      pageCardV3VO.addElementForMainCard(ElementBuilder.text()
          .id(MainCardElementId.SUB_TITLE)
          .text(TT.gen("额度正在评估中。良好的还款记录将加快评估进程。"))
          .build());
    } else {
      pageCardV3VO.addElementForMainCard(ElementBuilder.text()
          .id(MainCardElementId.SUB_TITLE)
          .text(TT.gen("您的额度预计将于TIME可用。"))
          .elementParam(TextElement.TextParam.builder()
              .highlightMap(ImmutableMap.of("TIME", canReapplyDate))
              .highlightColor(BLACK_000)
              .highlightStyle(BOLD)
              .build())
          .build());
    }
  }

  private void buildSubTitleTipElementForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    if (!homePageContext.getPrepareAbTestVO().secondRiskRejectDisplayStrategy.isStrategyB()) {
      return;
    }

    UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();
    if (Objects.isNull(userCreditsContext) || Objects.isNull(userCreditsContext.getCreditsInfoVO())) {
      return;
    }

    Long timeReapply = userCreditsContext.getCreditsInfoVO().timeReapply;
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);

    Long time = hasReadyOrder ? getRecentlyBillingDate(homePageContext) : timeReapply;
    String localizedDateStr = Clock.dateTimeStringFromTimestampWithLocale(time, DateFormatter.dd___MMM___yyyy, homePageContext.getSdkType().getTimeZone(), homePageContext.getSdkType().getLocale());
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.SUB_TITLE_TIP)
        .action(new PopUpAction(PopUpAction.PopUpParam.builder()
            .title(TT.gen("温馨提示"))
            .content(hasReadyOrder ? TT.gen("贷款资格因风险评估因素暂时尚未通过。请保持良好还款记录，您可在{0}再次借款", localizedDateStr) : TT.gen("您的贷款资格由于风险评估因素暂时尚未通过。您可在{0}再次借款", localizedDateStr))
            .boldContent(TT.gen("{0}", localizedDateStr))
            .buttonContent(TT.gen("开启额度通知"))
            .build()))
        .build());
  }

  private Long getRecentlyBillingDate(HomePageContext homePageContext) {
    List<CashLoanInstalmentVO> instalmentList = homePageContext.getHomepageUserParamsVO().getSortedInstalmentVOs();
    if (CollectionUtils.isEmpty(instalmentList)) {
      return null;
    }
    return instalmentList.get(0).billingDate;
  }

  /**
   * 处理短期拒绝的用户
   *
   * @param pageCardV3VO      页面卡片视图对象
   * @param homePageContext   主页上下文
   * @param timeReapply       可以重新申请的时间
   * @param loanMarketDecision 贷超入口展示策略结果（与本 processor 对应）
   */
  private void handleTemporaryRejection(PageCardV3VO pageCardV3VO, HomePageContext homePageContext, Long timeReapply,
      LoanMarketEntranceDisplayDecision loanMarketDecision) {
    // 格式化可重新申请的时间
    String dateStr = Clock.dateTimeStringFromTimestampWithLocale(
        timeReapply,
        "dd MMM yyyy",
        EcTimeZone.JAKARTA.tz,
        YqgLocale.INDONESIAN
    );

    if (homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, loanMarketDecision.isShowLoanMarketEntrance())) {
      addLoanMarketMainCardElement(pageCardV3VO, homePageContext, GOTO_LINK_INFO);
    }

    // 添加获取额度按钮，点击后显示弹窗
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("获取额度"))
        .action(new PopUpAction(PopUpAction.PopUpParam.builder()
            .buttonContent(TT.gen("知道了"))
            .content(TT.gen("由于信用评分不足，未获得借款额度。您可在{0} 再次申请。", dateStr))
            .build()))
        .build());

    if (homePageContext.getPrepareAbTestVO().secondRiskRejectDisplayStrategy.isStrategyB()) {
      pageCardV3VO.addElementForMainCard(ElementBuilder.button()
          .id(MAIN_BUTTON)
          .text(TT.gen("开启额度通知"))
          .build());
    }
  }

  /**
   * 处理有资格但尚未提交申请的用户
   *
   * @param pageCardV3VO      页面卡片视图对象
   * @param homePageContext   主页上下文
   * @param loanMarketDecision 贷超入口展示策略结果（与本 processor 对应）
   */
  private void handleWaitingForFirstSubmission(PageCardV3VO pageCardV3VO, HomePageContext homePageContext,
      LoanMarketEntranceDisplayDecision loanMarketDecision) {
    Long userId = homePageContext.getUserId();
    log.info("User waiting to submit first application, userId:{}", userId);

    // 构建基础信息元素
    homepageV3CardTool.buildCommonLoanInfoElementForMainCard(pageCardV3VO, homePageContext);

    if (homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, loanMarketDecision.isShowLoanMarketEntrance())) {
      addLoanMarketMainCardElement(pageCardV3VO, homePageContext, GOTO_LINK_INFO);
    }

    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(SUB_TITLE)
        .text(TT.gen("额度计算中，需要补充信息"))
        .build());
    // 添加立即补充按钮，点击后跳转到增信页面
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("立即补充"))
        .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getRejectedConfigByElementId(MAIN_BUTTON.name(), "IncreaseCreditsReview"))))
        .build());
  }

  /**
   * 处理申请正在审核中的用户
   *
   * @param pageCardV3VO    页面卡片视图对象
   * @param homePageContext 主页上下文
   */
  private void handleApplicationUnderReview(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    Long userId = homePageContext.getUserId();
    log.info("User application under review, userId:{}", userId);

    // 添加标题和副标题
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen("工作人员审核中"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(SUB_TITLE)
        .text(TT.gen("我们将在24小时内联系您，请注意来电并及时接听"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getRejectedConfigByElementId(MAIN_ICON.name(), "reviewIcon"))
        .build());

    // 添加查看进度按钮
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("查看审核进度"))
        .action(new RedirectAction(new RedirectAction.LinkActionParam(
            elementConfig.getRejectedConfigByElementId(MAIN_BUTTON.name(), "REJECTED"))))
        .build());
  }

  /**
   * 处理永久被拒的用户
   *
   * @param pageCardV3VO      页面卡片视图对象
   * @param homePageContext   主页上下文
   * @param loanMarketDecision 贷超入口展示策略结果（与本 processor 对应）
   */
  private void handlePermanentlyRejected(PageCardV3VO pageCardV3VO, HomePageContext homePageContext,
      LoanMarketEntranceDisplayDecision loanMarketDecision) {
    Long userId = homePageContext.getUserId();
    log.info("User permanently rejected, userId:{}", userId);

    // 构建基础信息元素
    homepageV3CardTool.buildCommonLoanInfoElementForMainCard(pageCardV3VO, homePageContext);

    // 添加查看其他贷款平台按钮，由贷超入口展示策略决定
    if (loanMarketDecision.isShowLoanMarketEntrance()) {
      addLoanMarketElementButton(pageCardV3VO, homePageContext);
    } else {
      addDisabledElementButton(pageCardV3VO);
    }
  }

  /**
   * 添加贷超主按钮入口，目前只有永拒才调用这个地方,且已经做过风控展示贷超按钮的校验了
   *
   * @param pageCardV3VO 页面卡片视图对象
   */
  private void addLoanMarketElementButton(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    if (homepageContentTool.checkUserNotInLoanMarketCardExpr(homePageContext)) {
      addLoanMarketMainCardElement(pageCardV3VO, homePageContext, MAIN_BUTTON);
      return;
    }
    pageCardV3VO.forceAddElementForMainCard(
        ElementBuilder.text()
            .id(MainCardElementId.TITLE)
            .text(TT.gen("贷款暂不可用"))
            .build()
    );
    pageCardV3VO.forceAddElementForMainCard(
        ElementBuilder.text()
            .id(MainCardElementId.SUB_TITLE)
            .text(TT.gen("拥有良好的信用记录，未来仍有机会。"))
            .build()
    );
    pageCardV3VO.deleteFromElementMap(MainCardElementId.AMOUNT_TEXT);
  }

  private void addLoanMarketMainCardElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext, MainCardElementId mainCardElementId) {
    RedirectAction.LoanMarketLinkActionParam param = new RedirectAction.LoanMarketLinkActionParam();
    param.redirectUrl = homepageV5Config.getRejectLoanMarketInfoUrl();
    param.loanMarket = true;
    param.loanMarketRule = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType;

    pageCardV3VO.addElementForMainCard(ElementBuilder.link()
        .id(mainCardElementId)
        .text(TT.gen("查看其他贷款平台"))
        .action(new RedirectAction(param))
        .build());
    if (MAIN_BUTTON == mainCardElementId && homePageContext.loanMarketMainButtonReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.mainButtonParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.MAIN_BUTTION, getClass());
    }
    if (GOTO_LINK_INFO == mainCardElementId && homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
    }
  }

  private void addDisabledElementButton(PageCardV3VO pageCardV3VO) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("立即申请"))
        .action(IElementAction.NONE_ACTION)
        .build());
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.REJECT_STATUS;
  }
}
