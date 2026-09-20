package com.miyou.controllers.cashloan.newhomepage.elementmodel.auth;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 未登录processer，主button动作为跳转登录注册页面，对应以下状态
 * NOT_LOGIN("未登录", IDNHomepageDisplayStatusV5.NOT_LOGIN),
 */
@Component
@Slf4j
public class NotLoginProcessor extends AbstractAuthProcessor {
  @Override
  protected void processCustomElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("查看额度"))
        .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getAuthElementsConfigByElementId(MainCardElementId.MAIN_BUTTON.name(), IDNHomepageLoanStatusV5.NOT_LOGIN.name()))))
        .build());
  }


  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.NOT_LOGIN;
  }
}