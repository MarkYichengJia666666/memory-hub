package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageReviewRejectedProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private RiskReuploadService riskReuploadService;
  @Override
  public void process(UserResponse userInfo, HomePageContext homePageContext) {
    Long reuploadDeadline = riskReuploadService.geTreuploadDeadline(homePageContext.getLoanAccountId());
    String reuploadTime = Clock.dateTimeStringFromTimestamp(reuploadDeadline,
        DateFormatter.getDateFormatter(homePageContext.getSdkType().getLocale()),
        homePageContext.getSdkType().getTimeZone());
    TT content = TT.gen("您的信息不清晰，请在{0}前重新提交，提交成功后我们将再次审核并告知您结果", reuploadTime);
    userInfo.setButtonName(TT.gen("重拍KTP"))
        .setTitle(TT.gen("重新上传信息"))
        .setContent(content)
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getKtpReuploadMainCardInfo(content));
        ;

  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.IMAGE_REVIEW_REJECTED;
  }
}
