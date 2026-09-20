package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.HomePageGuideBubbleInfoV3;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HomePageGuideBubbleInfoV3Processor extends AbstractHomepageUserInfoV3Processor{
    @Autowired
    private HomepageContentTool homepageContentTool;

    @Override
    public HomepageUserInfoV3ProcessorType getProcessorType() {
        return HomepageUserInfoV3ProcessorType.HOME_PAGE_GUIDE_BUBBLE_V3;
    }

    @Override
    public void process(PageUserInfoV3Response userInfoV3Response, HomePageContext homePageContext) {
        userInfoV3Response
                .setUserInfo(HomePageGuideBubbleInfoV3.builder()
                        .homePageGuideBubbleInfo(homepageContentTool.getHomePageGuideBubbleInfo(homePageContext.getUserId(),
                                        homePageContext.getUserDeviceContextVO().getBuild()))
                        .build());
    }
}
