package com.miyou.controllers.cashloan.newhomepage;


import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageV3ExperimentContext;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpPreDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.auth.AuthService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.ec.common.enums.loan.SourceType;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @deprecated 已由 {@link com.yqg.core.userflow.domain.home.service.IHomeSafeMoudelService} 替代
 */
@Deprecated
@Slf4j
@Service
public class SafetyModuleExpService {

  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private ExpPreDiversionClient expPreDiversionClient;
  @Autowired
  private AuthService authService;
  @Autowired
  private IUserInfoService userInfoServiceImpl;

  public ExperimentPlatformGroupType fetchExpRes(HomepageV3ExperimentContext homepageV3ExperimentContext,
      UserDeviceContextVO userDeviceContextVO, LoanAccountVO loanAccountVO, IDNHomepageLoanStatusV5 status, boolean isPrediversion) {
    if (homepageV3ExperimentContext == null || !homepageV3ExperimentContext.isV3()) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }

    SourceType sourceType = userDeviceContextVO.getSourceType();
    if (sourceType != null && sourceType.isApiChannelSourceType()) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }

    if (RequestClientType.isWholeProcess(userDeviceContextVO.getClientType())) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }

    Long build = userDeviceContextVO.getBuild();
    if (build == null || build < homepageV5Config.getSafetyModuleStartBuild()) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }
    //未登录
    if (Objects.isNull(loanAccountVO)) {
      return divert(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE_NOT_LOGIN,
          buildDeviceExpUser(userDeviceContextVO.getDeviceToken(), build, userDeviceContextVO.getSourceType()), isPrediversion);
    }

    //未完件
    if (!authService.isAuthFinished(loanAccountVO.id)) {
      return divert(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE_FIRST_LOAN,
          buildUserExpUser(loanAccountVO.userId, build, userDeviceContextVO.getSourceType()), isPrediversion);
    }

    if (!status.canCreateOrder()) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }

    //首贷
    if (loanAccountVO.firstLoan()) {
      return divert(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE_FIRST_LOAN,
          buildUserExpUser(loanAccountVO.userId, build, userDeviceContextVO.getSourceType()), isPrediversion);
    }

    // 复贷非回捞
    if (userInfoServiceImpl.isRetrievalUserByAccountId(loanAccountVO.id)) {
      return ExperimentPlatformGroupType.BLANK_GROUP;
    }
    return divert(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE,
        buildUserExpUser(loanAccountVO.userId, build, userDeviceContextVO.getSourceType()), isPrediversion);
  }

  private ExpUser buildDeviceExpUser(String deviceToken, Long build, SourceType sourceType) {
    return ExpUser.builder().deviceToken(deviceToken).versionBuild(build).sourceType(sourceType).build();
  }

  private ExpUser buildUserExpUser(Long userId, Long build, SourceType sourceType) {
    return ExpUser.builder().userId(userId).versionBuild(build).sourceType(sourceType).build();
  }

  private ExperimentPlatformGroupType divert(UserFlowExperimentEnum experiment, ExpUser expUser, boolean isPrediversion) {
    if (isPrediversion) {
      return ExperimentPlatformGroupType.valueOfOrNull(expPreDiversionClient.getResult(experiment.getKey(), expUser));
    }
    return ExperimentPlatformGroupType.valueOfOrNull(expDiversionClient.getResult(experiment.getKey(), expUser));
  }
}
