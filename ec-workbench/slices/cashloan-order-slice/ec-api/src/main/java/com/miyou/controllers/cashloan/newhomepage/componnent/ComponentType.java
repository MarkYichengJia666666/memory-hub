package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * app组件type
 */
@Slf4j
public enum ComponentType {
  BANNER(IBannerParamCreateService.BANNER_FUNCTION, IBannerParamCreateService.class, "旧首页banner"),
  BANNER_V2(IBannerParamCreateService.BANNER_FUNCTION, IBannerParamCreateService.class, "新首页banner"),
  NOTICE(INoticeParamCreateService.NOTICE_FUNCTION, INoticeParamCreateService.class, "notice");
  private Function<ComponentParamFunctionBaseVO, ? extends ComponentBaseParam> function;
  @Getter
  private Class<? extends IComponentParamCreateService> clazz;
  private String desc;

  ComponentType(Function<ComponentParamFunctionBaseVO, ? extends ComponentBaseParam> function,
                Class<? extends IComponentParamCreateService> clazz,
                String desc) {
    this.function = function;
    this.clazz = clazz;
    this.desc = desc;
  }


  public ComponentBaseParam createComponentParam(IComponentParamCreateService createService, HomePageContext homePageContext) {
    try {
      return this.function.apply(ComponentParamFunctionBaseVO.create(createService, homePageContext, this));
    } catch (Exception e) {
      log.error("createComponentParam error, type:{},serviceType:{}, status:{}", this.name(), createService.getClass().getSimpleName(), homePageContext.getStatus(), e);
      return null;
    }
  }
}
