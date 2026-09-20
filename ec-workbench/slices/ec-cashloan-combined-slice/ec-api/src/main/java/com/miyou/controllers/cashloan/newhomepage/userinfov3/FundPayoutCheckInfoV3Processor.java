package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import org.springframework.stereotype.Component;

@Component
public class FundPayoutCheckInfoV3Processor extends AbstractOrderCheckInfoV3Processor{
    @Override
    public BizCheckGroup getBizCheckGroup() {
        return BizCheckGroup.FUND_PAYOUT;
    }

    @Override
    public HomepageUserInfoV3ProcessorType getProcessorType() {
        return HomepageUserInfoV3ProcessorType.FUND_PAYOUT_BIZ_CHECK_V3;
    }
}
