package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.bizcheck.BizCheckResponseFactory;
import com.miyou.controllers.bizcheck.response.BizCheckResponse;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.OrderCheckInfoV3;
import com.yqg.core.model.sql.cashloan.enums.BusinessType;
import com.yqg.core.service.bizcheck.BizCheckListService;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import com.yqg.core.service.bizcheck.resultvo.BizCheckCommonResultVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

public abstract class AbstractOrderCheckInfoV3Processor extends AbstractHomepageUserInfoV3Processor {
    @Autowired
    private BizCheckListService bizCheckListService;

    public abstract BizCheckGroup getBizCheckGroup();

    @Override
    public void process(PageUserInfoV3Response userInfoV3Response, HomePageContext homePageContext) {
        BizCheckCommonResultVO checkResultVO = bizCheckListService.getPendingOrLatestGroupCheckResultVO(
                homePageContext.getUserCashLoanOrderContext().getLatestOrderVO().id,
                BusinessType.ORDER_CHECK,
                Collections.singletonList(getBizCheckGroup())
        );

        BizCheckResponse response = BizCheckResponseFactory.getBizCheckResponse(checkResultVO.checkType);
        BizCheckResponse bizCheckResponse = response.convert(
                checkResultVO,
                bizCheckListService.getGroupCheckStatus(checkResultVO.businessId, checkResultVO.businessType, checkResultVO.checkTypeGroup)
        );
        userInfoV3Response.setOrderInfo(OrderCheckInfoV3.builder()
                .bizCheckInfo(bizCheckResponse)
                .build());
    }
}
