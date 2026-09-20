package com.miyou.controllers.cashloan.newhomepage.elementmodel.order;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.HomepageWebChannelConfig;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageWholeProcessTool;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class MultiLoanProductProcessor extends LoanProductProcessor {
  @Autowired
  private HomepageWholeProcessTool homepageWholeProcessTool;
  @Autowired
  private HomepageWebChannelConfig homepageWebChannelConfig;

  @Override
  protected void doCreateButton(PageCardV3VO pageCardV3VO, HomeDisplayStrategy couponStyle, HomePageContext homePageContext) {
    if (!homepageWholeProcessTool.showDownloadButton() && !isWebRevolvingChannel(homePageContext)) {
      return;
    }
    appendMainApplyButton(pageCardV3VO, couponStyle, homePageContext);
  }

  /**
   * 判断是否为 WEB 续借白名单渠道。
   * WEB 全流程默认屏蔽主按钮，白名单内的渠道例外开放续借入口。
   */
  private boolean isWebRevolvingChannel(HomePageContext homePageContext) {
    UserDeviceContextVO deviceCtx = homePageContext.getUserDeviceContextVO();
    if (deviceCtx == null) {
      return false;
    }
    return homepageWebChannelConfig.isWebChannelRevolvingEnabled(deviceCtx.getChannel());
  }


  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.MULTI_LOAN_PRODUCT_INFO;
  }

}
