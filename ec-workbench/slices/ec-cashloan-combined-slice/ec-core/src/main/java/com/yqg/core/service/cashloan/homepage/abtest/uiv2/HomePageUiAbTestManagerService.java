package com.yqg.core.service.cashloan.homepage.abtest.uiv2;

import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.vo.HomepagePrepareAbTestVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.ec.common.enums.loan.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 首页新ui的分流管理---所有分流service都只暴露在该ManagerService中
 */
@Service
@Slf4j
public class HomePageUiAbTestManagerService {
  @Autowired
  private HomePageUiAbTestService homePageUiAbTestService;
  @Autowired
  private HomePageUiV2PopupAbTestService homePageUiV2PopupAbTestService;
  @Autowired
  private HomePageUiFloatingIconAbTestService homePageUiFloatingIconAbTestService;
  @Autowired
  private HomePageUiTopAreaAbTestService homePageUiTopAreaAbTestService;
  @Autowired
  private HomePageUiRevolvingLoanAbTestService homePageUiRevolvingLoanAbTestService;
  @Autowired
  private HomepageV5Config homepageV5Config;


  public HomepagePrepareAbTestVO getUiHomePageAbTestResult(Long userId, Long accountId, Long build) {
    HomeDisplayStrategy uiV2PopupAbTestResult = homePageUiAbTestService.getHomePageUiV2PopupAbTest(userId, accountId);
    HomepagePrepareAbTestVO res = new HomepagePrepareAbTestVO();
    HomeDisplayStrategy revolvingReviewInnerDisplayStrategy = homePageUiRevolvingLoanAbTestService.getAbTestResult(userId, accountId, build);
    if (HomeDisplayStrategy.A == uiV2PopupAbTestResult) {
      res.revolvingReviewInnerDisplayStrategy = revolvingReviewInnerDisplayStrategy;
      return res;
    }
    HomepagePrepareAbTestVO resNew = new HomepagePrepareAbTestVO();
    resNew.revolvingReviewInnerDisplayStrategy = revolvingReviewInnerDisplayStrategy;
    resNew.homePageUiStrategy = uiV2PopupAbTestResult;
    resNew.homePageCutInterestPopupStrategy = homePageUiV2PopupAbTestService.getHomePageUiV2PopupAbTest(userId);
    resNew.floatingIconStrategy = homePageUiFloatingIconAbTestService.getAbTestResult(userId);
    resNew.topAreaStrategy = homePageUiTopAreaAbTestService.getAbTestResult(userId);
    //注注注！！！！！！只有新页面才需要做分流的实验加在这里
    return resNew;
  }

  public HomepagePrepareAbTestVO getUiHomePageAbTestResultForH5(Long userId, Long accountId, Long build) {
    HomepagePrepareAbTestVO resNew = new HomepagePrepareAbTestVO();
    resNew.homePageUiStrategy = homePageUiAbTestService.getHomePageUiV2PopupAbTest(userId, accountId);
    return resNew;
  }

  public void executeAbTestForSubHomePage(Long userId, Long accountId) {
    HomeDisplayStrategy homePageUiV2PopupAbTest = homePageUiAbTestService.getHomePageUiV2PopupAbTest(userId, accountId);
    if (homePageUiV2PopupAbTest == HomeDisplayStrategy.A) {
      return;
    }
    homePageUiTopAreaAbTestService.executeAndGetAbTestResult(userId);
  }

  public void executeHomePageUiV2AbTest(Long userId, Long build, SourceType sourceType, Long accountId) {
    if (!isSupportedSourceTypeWithValidBuild(sourceType, build)) {
      return;
    }
    HomeDisplayStrategy homeDisplayStrategy = homePageUiAbTestService.executeHomePageUiV2AbTest(userId, accountId);
    if (homeDisplayStrategy == HomeDisplayStrategy.A) {
      return;
    }
    homePageUiV2PopupAbTestService.executeAndGetHomePageUiV2PopupAbTest(userId, accountId);
  }
  private boolean isSupportedSourceTypeWithValidBuild(SourceType sourceType, Long build) {
    if (sourceType == null) {
      return false;
    }
    switch (sourceType) {
      case IOS:
      case ANDROID:
        return build >= homepageV5Config.getNewHomepageStartBuild();
      case WEB:
        return build >= homepageV5Config.getNewHomepageStartBuildWeb();
      default:
        return false;
    }
  }

  public HomeDisplayStrategy executeHomePageFloatingIconAbTest(Long userId, Long accountId) {
    HomeDisplayStrategy homePageUiV2PopupAbTest = homePageUiAbTestService.getHomePageUiV2PopupAbTest(userId, accountId);
    if (homePageUiV2PopupAbTest == HomeDisplayStrategy.A) {
      return HomeDisplayStrategy.A;
    }
    return homePageUiFloatingIconAbTestService.executeAndGetAbTest(userId);
  }

}
