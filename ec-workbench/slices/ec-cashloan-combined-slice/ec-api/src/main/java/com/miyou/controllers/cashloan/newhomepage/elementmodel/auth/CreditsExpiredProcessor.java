package com.miyou.controllers.cashloan.newhomepage.elementmodel.auth;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.ApiRequestAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpContext;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpResult;
import com.yqg.core.service.cashloan.creditgain.CreditGainPerceptionExpService;
import com.yqg.core.service.cashloan.creditgain.CreditGainScene;
import com.yqg.core.service.cashloan.homepage.config.ApiRequestPath;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 额度失效processer，主button动作为调用测额API，对应以下状态
 * LOAN_CREDITS_EXPIRED("首贷额度失效", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
 * CALC_CREDITS_EXPIRED("复贷额度失效", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
 * MULTI_LOAN_INIT("可以续借额度测算", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
 */
@Component
@Slf4j
public class CreditsExpiredProcessor extends AbstractAuthProcessor {
  @Autowired
  protected HomepageContentTool homepageContentTool;
  @Autowired
  protected SensorsService sensorsService;
  @Autowired
  protected LoanMarketConfig loanMarketConfig;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;
  @Autowired
  private CreditGainPerceptionExpService creditGainPerceptionExpService;
  @Autowired
  private IUserInfoService userInfoService;

  /** US5 失效大卡主文案 */
  private static final String CREDIT_GAIN_EXPIRED_TITLE_TEXT = "欢迎回来！Easycash为你保留了额度更新资格";

  /** US5 失效大卡主按钮文案，动作复用既有测额 API */
  private static final String CREDIT_GAIN_EXPIRED_BUTTON_TEXT = "立即查看";

  /** US5 失效大卡额度审核图在 auth 元素配置中的 uniqueId，图 url 由运营配置下发 */
  private static final String CREDIT_GAIN_EXPIRED_ICON_CONFIG_KEY = "CREDIT_GAIN_EXPIRED";

  @Override
  protected void processCustomElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    if (hitCreditGainExpiredExperiment(homePageContext)) {
      // 未命中 / fail-open 保持既有假额度展示（NFR-002）；降额通知走独立 NoticeListResponse，不受此影响
      renderCreditGainExpiredCard(pageCardV3VO, homePageContext);
    } else {
      pageCardV3VO.addElementForMainCard(ElementBuilder.button()
          .id(MainCardElementId.MAIN_BUTTON)
          .text(TT.gen("查看额度"))
          .action(new ApiRequestAction(ApiRequestPath.CALC_CREDITS))
          .build());
    }

    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, true);
    if (homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk)) {
      loanMarketGotoLink(pageCardV3VO, homePageContext);
    }
  }

  /**
   * TAPD-369589 US5 失效大卡新样式，一处覆盖首贷失效 / 复贷失效 / 续借失效三态。
   *
   * <p>父类 {@code process} 为 final 且先无条件下发通用大卡元素，子类只能按同一 elementId 覆盖 / 移除，
   * 无法改为「不生成再组装」；PRD 图1 该态仅「额度审核图 + 主文案 + 立即查看」，故删掉通用的假额度与副标题。
   */
  private void renderCreditGainExpiredCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.forceAddElementForMainCard(ElementBuilder.image()
        .id(MainCardElementId.MAIN_ICON)
        .imageUrl(elementConfig.getAuthElementsConfigByElementId(
            MainCardElementId.MAIN_ICON.name(), CREDIT_GAIN_EXPIRED_ICON_CONFIG_KEY))
        .build());
    pageCardV3VO.forceAddElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen(CREDIT_GAIN_EXPIRED_TITLE_TEXT))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen(CREDIT_GAIN_EXPIRED_BUTTON_TEXT))
        .action(new ApiRequestAction(ApiRequestPath.CALC_CREDITS))
        .build());
    pageCardV3VO.deleteFromElementMap(MainCardElementId.AMOUNT_TEXT);
    pageCardV3VO.deleteFromElementMap(MainCardElementId.SUB_TITLE);
    log.debug("[CreditGainExpired] render new expired card, removed AMOUNT_TEXT & SUB_TITLE, userId={}",
        homePageContext.getUserId());
  }

  /**
   * 是否命中「增强额度获得感」失效戳额实验组。
   *
   * <p>本 Processor 只服务失效待戳额三态，故判组入参场景恒为 {@link CreditGainScene#CREDIT_EXPIRED_RE_CALC}；
   * 复贷（含续借子类）由 {@code reloan} 优先短路到复贷实验、首贷落失效戳额实验（见 CreditGainPerceptionExpService）。
   * 任何异常 fail-open 返回 false（NFR-002 不阻断既有链路）。
   */
  private boolean hitCreditGainExpiredExperiment(HomePageContext homePageContext) {
    try {
      CreditGainExpContext expContext = buildExpContext(homePageContext);
      CreditGainExpResult result = creditGainPerceptionExpService.resolve(expContext);
      // 与审核中 / 结果态同格式：失效大卡命中实验三、紧接着的审核中却退回老样式时，
      // 靠这两条同格式日志一比就能看出是哪一环把场景判丢了。
      log.debug("[CreditGainExpired] resolve experiment, userId={}, scene={}, reloan={}, hitExpKey={}, "
              + "experimentGroup={}",
          homePageContext.getUserId(), expContext.getScene(), expContext.isReloan(),
          result.getHitExpKey(), result.isExperimentGroup());
      return result.isExperimentGroup();
    } catch (Exception e) {
      log.warn("[CreditGainExpired] resolve experiment failed, fail-open to online style, userId={}",
          homePageContext.getUserId(), e);
      return false;
    }
  }

  /**
   * 组装失效态判组入参：本态恒为失效戳额场景（{@link CreditGainScene#CREDIT_EXPIRED_RE_CALC}）；
   * 复贷（含续借子类）走复贷实验（该场景满足其场景条件），首贷走失效戳额实验。
   *
   * <p>这里<b>不</b>按风控 triggerSource 推导场景（区别于审核中 / 结果态）：失效待戳额态的场景由首页状态本身
   * 即可确定，且此刻用户尚未戳额，最新风控 trace 属于上一轮流程、推不出本次场景。
   *
   * <p>人群（是否复贷）改用领域服务 {@link IUserInfoService#isReloanUserByAccountId}（打款成功次数&gt;0 为复贷），
   * 替换已 Deprecated 的 {@code HomepageUserParamsVO.firstLoan}；场景与人群解耦。
   */
  private CreditGainExpContext buildExpContext(HomePageContext homePageContext) {
    UserDeviceContextVO deviceContext = homePageContext.getUserDeviceContextVO();
    return CreditGainExpContext.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(deviceContext == null ? null : deviceContext.getDeviceToken())
        .sourceType(deviceContext == null ? null : deviceContext.getSourceType())
        .build(deviceContext == null ? null : deviceContext.getBuild())
        .reloan(userInfoService.isReloanUserByAccountId(homePageContext.getLoanAccountId()))
        .scene(CreditGainScene.CREDIT_EXPIRED_RE_CALC)
        .build();
  }

  private void loanMarketGotoLink(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    RedirectAction.LoanMarketLinkActionParam loanMarketParam = new RedirectAction.LoanMarketLinkActionParam();
    loanMarketParam.redirectUrl = homepageV5Config.getRejectLoanMarketInfoUrl();
    loanMarketParam.loanMarket = true;
    loanMarketParam.loanMarketRule = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType;

    pageCardV3VO.addElementForMainCard(ElementBuilder.link()
        .id(MainCardElementId.GOTO_LINK_INFO)
        .text(TT.gen("查看其他贷款平台"))
        .action(new RedirectAction(loanMarketParam))
        .build());
    if (homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
    }
  }


  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.CREDITS_EXPIRED;
  }
}
