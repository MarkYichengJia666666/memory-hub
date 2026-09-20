package com.yqg.core.service.loan.bankaccount;

import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.bankaccount.BindCardNameSimilarityCheckLogModel;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.util.NameSimilarityUtil;
import com.yqg.core.util.NameSimilarityUtil.NameSimilarityScoreDetail;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.utils.MaskUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 绑卡环节"持卡人姓名 vs 借款人留底姓名"相似度校验（TAPD-1351382），本次（TAPD-360957）从
 * {@link LoanBankAccountService} 抽取为独立子服务，并新增结构化落表：
 * **沿用母需求既有的 AB 分组门禁位置**——只有命中 {@code EXPERIMENT_GROUP} 才会继续计算相似度并落一条
 * 记录到 {@code bind_card_name_similarity_check_log}；对照组/未入组与豁免条件命中时一样，直接放行、
 * 不计算、不落表，落库逻辑与母需求原有的校验逻辑保持一致。
 *
 * <p><b>为何</b>：4 处前端入口（完件 / 风控补救 / 下单页 / 个人中心）共用本 service 入口，
 * 在 {@code checkParam} 后做 single-source 拦截即可一次性覆盖全部入口；抽取为子服务避免继续在
 * 冻结 god class {@link LoanBankAccountService} 内堆叠新功能。落库失败不得影响绑卡主流程。
 */
@Service
@Slf4j
public class BindCardNameSimilarityCheckService {

  private static final long BIND_CARD_NAME_SIMILARITY_MIN_BUILD = 36900L;
  private static final String BIND_CARD_NAME_SIMILARITY_EXP_KEY = "technology-other-abroad-loan_all-name_check";
  private static final String LAST_PAGE_QUERY_PARAM = "lastPage";
  private static final String LAST_PAGE_CREDIT = "CREDIT";
  private static final String LAST_PAGE_HOME = "HOME";
  private static final String LAST_PAGE_SUB_HOME = "SUB_HOME";
  private static final String SIMILARITY_RESULT_MATCH = "MATCH";
  private static final String SIMILARITY_RESULT_NOT_MATCH = "NOT_MATCH";
  private static final int REFERER_LOG_MAX_LENGTH = 200;

  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanUserAdditionalInfoService loanUserAdditionalInfoService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private BindCardNameSimilarityCheckLogModel bindCardNameSimilarityCheckLogModel;

  /**
   * 绑卡入口来源标识；用于在拦截日志中区分四个前端入口（母需求既有物理接口维度，语义不变）。<br>
   * - {@link #H5_AUTH}：完件绑卡 H5 链路（`/api/idn/cashloan/addBankAccount`）<br>
   * - {@link #NATIVE_REBIND}：原生 `AddAnotherBankCardAct`（下单页换绑 + 个人中心换绑，走 `/api/v3/cashloan/addBankAccountWithIdNumber`）
   */
  public enum BindCardEntry {
    H5_AUTH,
    NATIVE_REBIND
  }

  /**
   * 落表用的业务场景维度（TAPD-360957 新增），比 {@link BindCardEntry} 更细：
   * 只要请求头 {@code Referer} 存在且路径匹配完件绑卡页，即可通过 {@code lastPage} 查询参数细分为
   * 完件绑卡 / 完件风控审核中绑卡 / 下单页换绑；其余情形（含个人中心换绑）统一归 {@link #NATIVE}。
   */
  public enum BindCardLogEntry {
    FULL_AUTH_BIND,
    FULL_AUTH_RISK_REVIEW_BIND,
    ORDER_PAGE_REBIND,
    NATIVE
  }

  /**
   * 绑卡环节姓名相似度校验入口。<br>
   * 仅对印尼借贷 SDK、非 API 渠道、非端外 H5 且命中 AB 实验组（{@code EXPERIMENT_GROUP}）的请求继续处理，
   * 与母需求原有校验逻辑一致：对照组 / 未入组直接放行，既不计算相似度也不落表。命中实验组后：
   * 不修改姓名（最终提交 name 与 baseline 大小写无关一致）的用户直接放行、不落表；否则计算相似度明细
   * 并落一条记录，不达标才抛异常拦截。
   */
  public void check(Long userId, SDKType sdkType, String accountNumber, String name, Long build, BindCardEntry entry) {
    if (!sdkType.isIdnLoanSDKType()) {
      return;
    }
    if (build == null || build < BIND_CARD_NAME_SIMILARITY_MIN_BUILD) {
      return;
    }
    SourceType sourceType = ImpliedContextUtils.sourceType();
    if (sourceType == null || sourceType.isApiChannelSourceType()
        || RequestClientType.isWholeProcess(ImpliedContextUtils.requestClientType())) {
      return;
    }
    String expGroup = getExpGroup(userId, build, sourceType);
    // 非 EXPERIMENT_GROUP 均直接放行：涵盖 CONTROL_GROUP 与实验平台未配置/灰度时返回的 BLANK_GROUP
    if (!UserFlowConstants.EXPERIMENT_GROUP.equals(expGroup)) {
      return;
    }

    Long loanAccountId = loanAccountService.getAccountIdByUserIdOrThrow(userId, sdkType);
    // getName 可能返回 null（完件链路异常未留底），isBlank 已覆盖 null 场景，无需单独判空
    String baseline = loanUserAdditionalInfoService.getName(loanAccountId);
    // 未取到留底姓名或与提交值一致 → 未改名，PRD 口径不触发，仍不落表
    if (StringUtils.isBlank(baseline) || baseline.equalsIgnoreCase(name)) {
      return;
    }

    NameSimilarityScoreDetail detail = NameSimilarityUtil.getSimilarScoreDetail(name, baseline);
    int minScore = cashLoanConfig.getNameMatchSimilarMinScore();
    boolean pass = detail.getFinalScore() >= minScore;
    BindCardLogEntry logEntry = resolveLogEntry();

    recordCheckLog(userId, loanAccountId, logEntry, expGroup, baseline, name, accountNumber, detail, pass);

    if (!pass) {
      log.info("bind card name similarity reject, entry: {}, userId: {}, score: {}, minScore: {}, name: {}, baseline: {}",
          entry, userId, detail.getFinalScore(), minScore, MaskUtils.maskedName(name), MaskUtils.maskedName(baseline));
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("姓名输入有误,请重新输入"));
    }
  }

  /**
   * 判断规则不依赖调用方传入的物理入口（{@link BindCardEntry}），也不校验 {@code Referer} 路径，
   * 只看 {@code lastPage} 查询参数细分场景；{@code Referer} 缺失/不合法、{@code lastPage} 未知取值
   * 均保守归为 {@link BindCardLogEntry#NATIVE}。
   */
  private BindCardLogEntry resolveLogEntry() {
    String referer = ImpliedContextUtils.referer();
    if (StringUtils.isBlank(referer)) {
      return BindCardLogEntry.NATIVE;
    }
    UriComponents refererUri;
    try {
      refererUri = UriComponentsBuilder.fromUriString(referer).build();
    } catch (Exception e) {
      log.warn("bind card name similarity: parse referer failed, referer: {}",
          StringUtils.abbreviate(referer, REFERER_LOG_MAX_LENGTH), e);
      return BindCardLogEntry.NATIVE;
    }
    String lastPage = refererUri.getQueryParams().getFirst(LAST_PAGE_QUERY_PARAM);
    if (StringUtils.isBlank(lastPage) || LAST_PAGE_CREDIT.equalsIgnoreCase(lastPage)) {
      return BindCardLogEntry.FULL_AUTH_BIND;
    }
    if (LAST_PAGE_HOME.equalsIgnoreCase(lastPage)) {
      return BindCardLogEntry.FULL_AUTH_RISK_REVIEW_BIND;
    }
    if (LAST_PAGE_SUB_HOME.equalsIgnoreCase(lastPage)) {
      return BindCardLogEntry.ORDER_PAGE_REBIND;
    }
    return BindCardLogEntry.NATIVE;
  }

  private void recordCheckLog(Long userId, Long loanAccountId, BindCardLogEntry logEntry, String expGroup,
      String baseline, String submittedName, String accountNumber,
      NameSimilarityScoreDetail detail, boolean pass) {
    try {
      // 调用方已保证 expGroup 恒为 EXPERIMENT_GROUP（见 check 方法门禁），此处原样落表不再做归一化
      bindCardNameSimilarityCheckLogModel.insert(userId, loanAccountId, logEntry.name(),
          expGroup, baseline, submittedName, accountNumber, detail,
          pass ? SIMILARITY_RESULT_MATCH : SIMILARITY_RESULT_NOT_MATCH);
    } catch (Exception e) {
      // 落库失败不得阻断绑卡主流程，仅记录异常日志（PII 已在上游脱敏/由 baseline、submittedName 传入前处理）
      log.error("bind card name similarity record failed, userId: {}, entry: {}", userId, logEntry, e);
    }
  }

  private String getExpGroup(Long userId, Long build, SourceType sourceType) {
    ExpUser expUser = ExpUser.builder()
        .userId(userId)
        .versionBuild(build)
        .sourceType(sourceType)
        .build();
    return expDiversionClient.getResult(BIND_CARD_NAME_SIMILARITY_EXP_KEY, expUser);
  }
}
