package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.RepaymentInfoV3;
import com.miyou.controllers.cashloan.utilities.HomePageMainCardInfoTool;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepaymentInfoV3Processor extends AbstractHomepageUserInfoV3Processor {
    @Autowired
    private HomepageContentTool homepageContentTool;
    @Autowired
    private HomePageMainCardInfoTool homePageMainCardInfoTool;

    @Override
    public HomepageUserInfoV3ProcessorType getProcessorType() {
        return HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3;
    }

    @Override
    public void process(PageUserInfoV3Response fieldsInfo, HomePageContext homePageContext) {
        fieldsInfo.setRepaymentInfo(RepaymentInfoV3.builder()
                .repayment(homepageContentTool.getRepaymentInfo(homePageContext))
                .build());
    }
}
