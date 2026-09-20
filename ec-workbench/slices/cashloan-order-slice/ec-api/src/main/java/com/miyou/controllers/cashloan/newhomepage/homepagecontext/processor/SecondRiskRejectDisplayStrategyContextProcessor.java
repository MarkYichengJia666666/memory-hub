package com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.IHomepageHomeContextProcessor;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SecondRiskRejectDisplayStrategyContextProcessor implements IHomepageHomeContextProcessor {
    @Autowired
    private HomepageV5Config homepageV5Config;
    @Override
    public void processHomeContext(HomePageContext homePageContext) {
        if (!canShowSecondRiskRejectDisplayStrategy(homePageContext)) {
            return;
        }
        // flush param
        homePageContext.getPrepareAbTestVO().secondRiskRejectDisplayStrategy = HomeDisplayStrategy.A;
    }

    private Boolean canShowSecondRiskRejectDisplayStrategy(HomePageContext homePageContext) {
        //逾期不进入实验
        if (Objects.nonNull(homePageContext.getUserCashLoanOrderContext()) && homePageContext.getUserCashLoanOrderContext().isOverdue()) {
          return false;
        }
        //≥新首页版本 & 首页状态为：临时拒绝/首贷已打款/复贷已打款
        if (Objects.isNull(homePageContext.getUserDeviceContextVO()) || Objects.isNull(homePageContext.getUserDeviceContextVO().getBuild())) {
            return false;
        }

        //READY、RELOAN_READY、CAN_REAPPLY_IN_FUTURE，都是当前不可借，都走本次实验
        return IDNHomepageLoanStatusV5.SECOND_RISK_REJECT_DISPLAY.contains(homePageContext.getStatus());
    }

    @Override
    public HomePageContextProcessorType getType() {
        return HomePageContextProcessorType.SECOND_RISK_REJECT_DISPLAY_STRATEGY;
    }
}
