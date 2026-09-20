package com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.IHomepageHomeContextProcessor;
import com.yqg.core.service.mergeaccount.ChannelMergeDetectionService;
import com.yqg.core.service.mergeaccount.ChannelMergeInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChannelMergeDetectionContextProcessor implements IHomepageHomeContextProcessor {

    @Autowired
    private ChannelMergeDetectionService channelMergeDetectionService;

    @Override
    public void processHomeContext(HomePageContext homePageContext) {
        if (homePageContext.getUserId() == null) {
            return;
        }
        try {
            ChannelMergeInfoVO mergeInfo =
                channelMergeDetectionService.detect(homePageContext.getUserId(), homePageContext.getSdkType(), homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserDeviceContextVO().getSourceType());
            homePageContext.getHomePageContextHolder().setChannelMergeInfoVO(mergeInfo);
        } catch (Exception e) {
            log.error("[channel-merge-predetect] failed, userId:{}", homePageContext.getUserId(), e);
        }
    }

    @Override
    public HomePageContextProcessorType getType() {
        return HomePageContextProcessorType.CHANNEL_MERGE_DETECTION;
    }
}
