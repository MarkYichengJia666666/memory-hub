package com.yqg.core.service.user;

import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.mongo.MongoUserLoginInfoModel;
import com.yqg.core.model.mongo.MongoUserRegisterInfoModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.user.LoginStatusCacheModel;
import com.yqg.core.model.sql.user.UserLoginInfoModel;
import com.yqg.core.model.sql.user.UserModel;
import com.yqg.core.model.sql.user.UserRegisterInfoModel;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.general.pageconfig.filterstrategy.GeneralPageConfigFilterStrategyService;
import com.yqg.core.service.general.pageconfig.filterstrategy.threadlocal.GeneralPageConfigFilterThreadLocal;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.user.vo.UserLoginDetailVO;
import com.yqg.core.service.user.vo.UserRegisterDetailVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chaoye
 * @date 2023/10/13
 */
@Slf4j
@Service
public class UserGeneralPageConfigFilterService {
  @Autowired
  private GeneralPageConfigFilterStrategyService strategyService;
  @Autowired
  private UserLoginInfoModel userLoginInfoModel;
  @Autowired
  private UserRegisterInfoModel userRegisterInfoModel;
  @Autowired
  private MongoUserLoginInfoModel mongoUserLoginInfoModel;
  @Autowired
  private MongoUserRegisterInfoModel mongoUserRegisterInfoModel;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private UserModel userModel;
  @Autowired
  private LoginStatusCacheModel loginStatusCacheModel;

  /**
   * 此方法，用于判断用户是否满足对应strategyId的首页策略
   * 一般提供给除ec-api外的服务调用，也可用于ec-api传参不太方便的方法调用
   * <p>
   * 这里的GeneralPageConfigParam的参数取的是用户登录/注册时的快照数据
   * 使用此方法时，需要确认对应的首页策略是否能够使用快照数据！！！
   *
   * @param strategyId
   * @param userId
   * @return
   */
  public boolean hitStrategy(Long strategyId, Long userId) {
    try {
      GeneralPageConfigParam generalPageConfigParam = getUserGeneralPageConfigParam(userId);
      if (generalPageConfigParam == null) {
        return false;
      }
      boolean res = strategyService.hitStrategy(strategyId, generalPageConfigParam, null);
      log.info("userId:{}, strategyId:{}. result:{}, generalPageConfigParam:{}", userId, strategyId, res, JsonUtils.toString(generalPageConfigParam));
      return res;
    } catch (Exception e) {
      log.warn("hitStrategy warn,userId:{}, strategyId:{}", userId, strategyId, e);
      return false;
    } finally {
      GeneralPageConfigFilterThreadLocal.remove();
    }
  }

  public boolean hitStrategyDeviceToken(Long strategyId, String deviceToken) {
    try {
      GeneralPageConfigParam generalPageConfigParam = new GeneralPageConfigParam();
      generalPageConfigParam.deviceToken = deviceToken;
      boolean res = strategyService.hitStrategy(strategyId, generalPageConfigParam, null);
      log.info("deviceToken:{}, strategyId:{}. result:{}, generalPageConfigParam:{}", deviceToken, strategyId, res, JsonUtils.toString(generalPageConfigParam));
      return res;
    } catch (Exception e) {
      log.warn("hitStrategy warn,deviceToken:{}, strategyId:{}", deviceToken, strategyId, e);
      return false;
    } finally {
      GeneralPageConfigFilterThreadLocal.remove();
    }
  }

  private GeneralPageConfigParam getUserGeneralPageConfigParam(Long userId) {
    //优先取上次登录的信息
    GeneralPageConfigParam generalPageConfigParam = getUserGeneralPageConfigParamByLogin(userId);
    if (generalPageConfigParam != null) {
      return generalPageConfigParam;
    }

    //若没有登录信息，取注册信息
    //若都没有，返回null
    return getUserGeneralPageConfigParamByRegister(userId);

  }

  private GeneralPageConfigParam getUserGeneralPageConfigParamByLogin(Long userId) {
    UserLoginInfoRecord userLoginInfoRecord = userLoginInfoModel.fetchLatestByUserId(userId);
    if (userLoginInfoRecord == null) {
      return null;
    }
    UserLoginDetailVO userLoginDetailVO = mongoUserLoginInfoModel.findByObjectIdOrNull(userLoginInfoRecord.getObjectId());
    if (userLoginDetailVO == null) {
      return null;
    }

    LoginStatusCacheRecord loginStatusCacheRecord = loginStatusCacheModel.findLatestByUserId(userId);

    SDKType sdkType = SDKType.fromCode(userLoginInfoRecord.getSdkType());
    Long build = loginStatusCacheRecord.getBuild();
    PlatformType platformType = PlatformType.fromCode(loginStatusCacheRecord.getPlatformType());
    SourceType sourceType = userLoginDetailVO.environmentInfo.obtainSourceType();
    String deviceToken = userLoginDetailVO.environmentInfo.deviceToken;

    LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserId(userId);
    IDNHomepageLoanStatusV5 statusV5 =
        loanAccountRecord == null ?
            null :
            homepageStatusTool.getStatus(loanAccountRecord.getId(), build, SDKType.fromCode(loanAccountRecord.getSdkType()));

    return GeneralPageConfigParam.from(
        userId,
        sdkType,
        build,
        platformType,
        null,
        statusV5,
        sourceType,
        deviceToken
    );
  }

  private GeneralPageConfigParam getUserGeneralPageConfigParamByRegister(Long userId) {
    UserRegisterInfoRecord userRegisterInfoRecord = userRegisterInfoModel.fetchByUserId(userId);
    if (userRegisterInfoRecord == null) {
      return null;
    }
    UserRegisterDetailVO userRegisterDetailVO = mongoUserRegisterInfoModel.findByObjectIdOrNull(userRegisterInfoRecord.getObjectId());
    if (userRegisterDetailVO == null) {
      return null;
    }

    UserRecord userRecord = userModel.findById(userId);
    LoginStatusCacheRecord loginStatusCacheRecord = loginStatusCacheModel.findLatestByUserId(userId);
    // USER_REGISTER_INFO 没有存sdk，只能取注册时候的sdk了
    SDKType sdkType = SDKType.fromCode(userRecord.getInitSdkType());
    Long build = loginStatusCacheRecord.getBuild();
    PlatformType platformType = PlatformType.fromCode(loginStatusCacheRecord.getPlatformType());
    SourceType sourceType = userRegisterDetailVO.environmentInfo.obtainSourceType();
    String deviceToken = userRegisterDetailVO.environmentInfo.deviceToken;

    LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserId(userId);
    IDNHomepageLoanStatusV5 statusV5 =
        loanAccountRecord == null ?
            null :
            homepageStatusTool.getStatus(loanAccountRecord.getId(), build, SDKType.fromCode(loanAccountRecord.getSdkType()));

    return GeneralPageConfigParam.from(

        userId,
        sdkType,
        build,
        platformType,
        null,
        statusV5,
        sourceType,
        deviceToken
    );
  }
}
