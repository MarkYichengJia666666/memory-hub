package com.miyou.controllers.cashloan.newhomepage.elementmodel.reject;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageWholeProcessTool;
import com.yqg.core.service.marketing.channel.HomeMainCardQian1Service;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.*;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.FOREST_GREEN;

@Component
@Slf4j
public class ReloadKtpCardProcessor extends AbstractPageCardV3Processor {

  @Autowired
  protected HomepageV5Config homepageV5Config;

  @Autowired
  private HomepageV3ElementConfig elementConfig;
  @Autowired
  private RiskReuploadService riskReuploadService;
  @Autowired
  private HomepageWholeProcessTool homepageWholeProcessTool;
  @Autowired
  private HomeMainCardQian1Service homeMainCardQian1Service;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    Long reuploadDeadline = riskReuploadService.geTreuploadDeadline(homePageContext.getLoanAccountId());
    int calenderDaysBetween = Clock.getCalenderDaysBetween(Clock.now(), reuploadDeadline, homePageContext.getSdkType().getTimeZone());
    if (homepageWholeProcessTool.showDownloadButton()) {
      pageCardV3VO.addElementForMainCard(ElementBuilder.button()
          .id(MAIN_BUTTON)
          .text(TT.gen("上传KTP"))
          .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getRedirectPage(NativePathConstant.KTP_RE_UPLOAD_PAGE))))
          .build());
    }
    // 未过件场景：命中千1 承接时覆盖副标题为 0.1% 营销条（主按钮跳转不变，spec US4-1）
    boolean qian1SubTitleHit = homeMainCardQian1Service.shouldShowMarketingBar(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getDeviceToken(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getUserDeviceContextVO().getSourceType(),
        homePageContext.getSdkType(), false);
    TT subTitle = qian1SubTitleHit
        ? TT.gen(HomeMainCardQian1Service.SUB_TITLE_TEXT_NOT_AUTH)
        : TT.gen("为保证您顺利申请，请于{0}天内完成上传", Math.max(calenderDaysBetween, 1));
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(SUB_TITLE)
        .text(subTitle)
        .elementParam(qian1SubTitleHit
            ? TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardQian1Service.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardQian1Service.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
            .highlightColor(FOREST_GREEN)
                .build()
            : TextElement.TextParam.builder().build())
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen("KTP图片不清晰"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getRejectedConfigByElementId(MAIN_ICON.name(), "RELOAD_KTP"))
        .build());
  }




  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.RELOAD_KTP;
  }
}