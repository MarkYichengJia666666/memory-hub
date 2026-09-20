package com.miyou.controllers.cashloan.newhomepage.elementmodel.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageWholeProcessTool;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.*;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.MAIN_BUTTON;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.MAIN_ICON;

@Service
public class ManualReviewProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private HomepageWholeProcessTool homepageWholeProcessTool;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    // 添加标题和副标题
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen("工作人员审核中"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(SUB_TITLE)
        .text(TT.gen("我们将在24小时内联系您，请注意来电并及时接听"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getRejectedConfigByElementId(MAIN_ICON.name(), "reviewIcon"))
        .build());

    if (homepageWholeProcessTool.showDownloadButton()) {
      // 添加查看进度按钮
      pageCardV3VO.addElementForMainCard(ElementBuilder.button()
          .id(MAIN_BUTTON)
          .text(TT.gen("查看审核进度"))
          .action(new RedirectAction(RedirectAction.LinkActionParam.builder().redirectUrl(elementConfig.getRedirectPage(NativePathConstant.SUB_HOME_REVIEW_PAGE)).build()))
          .build());
    }
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.MANUAL_REVIEW;
  }
}
