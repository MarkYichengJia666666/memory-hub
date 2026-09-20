package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.utilities.HomePageCacheService;
import com.miyou.controllers.cashloan.utilities.HomepageCommonTool;
import com.miyou.controllers.cashloan.utilities.HomepageNoticeTool;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.CashLoanOrderLimitService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractNoticeInfoProcessor implements IHomePageFieldProcessor<NoticeListResponse, HomepageNoticeProcessorType> {

  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  protected HomepageNoticeTool homepageNoticeTool;
  @Autowired
  protected CashLoanOrderLimitService cashLoanOrderLimitService;
  @Autowired
  protected HomePageCacheService homePageCacheService;
  @Autowired
  protected HomepageCommonTool homepageCommonTool;

  @Override
  public HomepageNoticeProcessorType getProcessorType() {
    return getNoticeProcessorType();
  }

  protected abstract HomepageNoticeProcessorType getNoticeProcessorType();

  @Override
  public void process(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (homePageContext.newHomePageUI()) {
      doProcessorForNewHomepage(noticeInfo, homePageContext);
    }else {
      doProcessorForOldHomepage(noticeInfo, homePageContext);
    }
  }

  protected abstract void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext);

  protected abstract void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext);
}
