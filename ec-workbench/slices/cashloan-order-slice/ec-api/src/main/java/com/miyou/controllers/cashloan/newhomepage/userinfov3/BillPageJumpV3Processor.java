package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.userpage.AutoJumpPageEnum;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.userpage.AutoJumpPageResponse;
import com.yqg.core.service.cashloan.homepage.JumpBillPageTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillPageJumpV3Processor extends AbstractHomepageUserInfoV3Processor {
  @Autowired
  private JumpBillPageTool jumpBillPageTool;

  @Override
  public void process(PageUserInfoV3Response fieldsInfo, HomePageContext homePageContext) {
    try {
      if (!jumpBillPageTool.canJumpBillPageForNotOverdue(homePageContext.getUserId())) {
        return;
      }
      fieldsInfo.setUserPage(AutoJumpPageResponse.builder()
              .autoJumpPage(AutoJumpPageEnum.BILL_PAGE)
          .build());
    } catch (Exception e) {
      log.warn("BillPageJumpV3Processor process error", e);
    }
  }



  @Override
  public HomepageUserInfoV3ProcessorType getProcessorType() {
    return HomepageUserInfoV3ProcessorType.AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE;
  }
}
