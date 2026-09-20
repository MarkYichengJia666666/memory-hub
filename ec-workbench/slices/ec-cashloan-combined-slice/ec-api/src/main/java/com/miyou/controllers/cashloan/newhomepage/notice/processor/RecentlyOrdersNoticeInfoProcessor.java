package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class RecentlyOrdersNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    BigDecimal principalThreshold = homepageV5Config.getPrincipalThreshold();
    NoticeListResponse recentlyOrdersNotice = homePageCacheService.getRecentlyOrdersNotice(homePageContext.getSdkType(), principalThreshold);
    BeanUtils.copyProperties(recentlyOrdersNotice, noticeInfo);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.RECENTLY_ORDERS_NOTICE_INFO_PROCESSOR;
  }
}