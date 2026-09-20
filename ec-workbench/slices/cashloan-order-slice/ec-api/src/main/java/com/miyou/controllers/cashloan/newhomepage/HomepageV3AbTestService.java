package com.miyou.controllers.cashloan.newhomepage;

import static com.yqg.core.service.abtest.AbstractExpClient.BLANK_GROUP;
import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageV3ExperimentContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersionExpRes;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpLastResultRunningClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.auth.AuthService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.util.scope.ImpliedContextUtils;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HomepageV3AbTestService {

  @Autowired
  AuthService authService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private ExpLastResultRunningClient expLastResultRunningClient;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private HomepageMonitorService homepageMonitorService;

  public HomepageV3ExperimentContext fetchHomepageV3ExperimentContext(Long build, LoanAccountVO loanAccountVO, IDNHomepageLoanStatusV5 status) {
    return fetchHomepageV3ExperimentContextCommon(build, loanAccountVO, status, false);
  }

  public HomepageV3ExperimentContext fetchHomepageV3ExperimentContextPrediversion(Long build, LoanAccountVO loanAccountVO, IDNHomepageLoanStatusV5 status) {
    return fetchHomepageV3ExperimentContextCommon(build, loanAccountVO, status, true);
  }

  private HomepageV3ExperimentContext fetchHomepageV3ExperimentContextCommon(Long build, LoanAccountVO loanAccountVO, IDNHomepageLoanStatusV5 status, boolean isPrediversion) {
    //只有NOT_LOGIN状态下loanAccountVO才为空
    if (Objects.isNull(loanAccountVO)) {
      return HomepageV3ExperimentContext.create(getNotLoginUserHomepageVersion().name());
    }
    if (Objects.equals(status, IDNHomepageLoanStatusV5.MINIMALIST_IN_REVIEW) || Objects.equals(status, IDNHomepageLoanStatusV5.MINIMALIST_ACCEPT)) {
      //极简状态用户直接返回V1
      return HomepageV3ExperimentContext.create(HomepageVersionExpRes.V1.getCode());
    }

    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    // 全流程直接返回V3样式
    if (isWholeProcess) {
      return HomepageV3ExperimentContext.emptyV3Context();
    }

    if (build < homepageV5Config.getNewHomepageVersion()) {
      return HomepageV3ExperimentContext.emptyV2Context();
    }
    if (authService.isAuthFinished(loanAccountVO.id)) {
      return HomepageV3ExperimentContext.create(HomepageVersion.V3.name());
    } else {
      //未完件
      String expResStr = expLastResultRunningClient.getString("product_operation_auth-auth-abroad-loan-homepage_popup_1120", HomepageVersion.V2.name());
      return HomepageV3ExperimentContext.create(expResStr);
    }
  }

  /**
   * 获取已经分流的用户首页版本，如果用户未分流，则返回默认版本
   *
   * @param build
   * @param userId
   * @param status
   * @return
   */
  public String fetchExistOrDefaultHomepageVersion(Long build, Long userId, IDNHomepageLoanStatusV5 status) {
    String homepageVersion;
    //只有NOT_LOGIN状态下userId才为空
    if (Objects.isNull(userId)) {
      homepageVersion = getNotLoginUserHomepageVersion().name();
    } else if (Objects.equals(status, IDNHomepageLoanStatusV5.MINIMALIST_IN_REVIEW)
        || Objects.equals(status, IDNHomepageLoanStatusV5.MINIMALIST_ACCEPT)) {
      //极简状态用户直接返回V1
      homepageVersion = HomepageVersion.V1.name();
    } else if (build < homepageV5Config.getNewHomepageVersion()) {
      homepageVersion = HomepageVersion.V2.name();
    } else if (authService.isAuthFinishedByUserId(userId)) {
      String result = expLastResultRunningClient.getResult("product_operation_order-not_withdraw-abroad-loan_all-HomePageV3_loan_0723",
          ExpUser.builder().userId(userId).versionBuild(build).build());
      homepageVersion = BLANK_GROUP.equals(result) ? "V2" : result;
    } else {
      homepageVersion = expLastResultRunningClient.getString("product_operation_auth-auth-abroad-loan-homepage_popup_1120",
          HomepageVersion.V2.name());
    }
    homepageMonitorService.logHomepageVersionResolve(userId, build, status, homepageVersion);
    return homepageVersion;
  }

  private HomepageVersion getNotLoginUserHomepageVersion() {
    try {
      Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
      if (isWholeProcess) {
        return HomepageVersion.V2;
      }
      String abRes = expDiversionClient.getResult("register_26h1-register-abroad-loan_all-before_login_reg_home_page_0106");
      if (StringUtils.equalsAny(abRes, "V2", "V3")) {
        return HomepageVersion.valueOf(abRes);
      }
      return HomepageVersion.V2;
    } catch (Exception e) {
      log.error("deviceToken:{}, build:{}", ImpliedContextUtils.userId(),ImpliedContextUtils.build(), e);
      return HomepageVersion.V2;
    }
  }
}
