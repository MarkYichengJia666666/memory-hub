package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.response.v5.notice.SingleNoticeResponse;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chenxianrui
 * @date 2024/12/5
 */
@Service
public class CanCreateOrderNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    // 获取提示信息 JSON 字符串
    String promptJsonInfo = cashLoanOrderLimitService.getPromptJsonInfo(
        homePageContext.getUserOrderLimitVO().limitType,
        homePageContext.getUserOrderLimitVO().orderOverLimitLevel
    );

    if (StringUtils.isNotBlank(promptJsonInfo)) {
      SingleNoticeResponse noticeResponse = JsonUtils.fromOrNull(promptJsonInfo, SingleNoticeResponse.class);
      if (Objects.nonNull(noticeResponse) && StringUtils.isNotBlank(noticeResponse.noticeData)) {
        noticeInfo
            .setData(Lists.newArrayList(TT.gen(noticeResponse.noticeData)))
            .setRedirectUrl(noticeResponse.noticeRedirectUrl)
            .setColor(noticeResponse.noticeColor);
        return;
      }
    }

    String antiFraudCarouselText = homepageV5Config.getAntiFraudCarouselText();
    if (StringUtils.isNotBlank(antiFraudCarouselText)) {
      noticeInfo.data.add(TT.gen(antiFraudCarouselText));
    }
  }


  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.CAN_CREATE_ORDER_NOTICE_INFO_PROCESSOR;
  }

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }
}
