package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import lombok.Getter;

@Getter
public class ComponentParamFunctionBaseVO {
  private HomePageContext homePageContext;
  private ComponentType componentType;
  private IComponentParamCreateService createComponentParamService;

  private ComponentParamFunctionBaseVO() {
  }

  public static ComponentParamFunctionBaseVO create(IComponentParamCreateService createComponentParamService, HomePageContext homePageContext, ComponentType componentType) {
    ComponentParamFunctionBaseVO componentParamFunctionBaseVO = new ComponentParamFunctionBaseVO();
    componentParamFunctionBaseVO.homePageContext = homePageContext;
    componentParamFunctionBaseVO.componentType = componentType;
    componentParamFunctionBaseVO.createComponentParamService = createComponentParamService;
    return componentParamFunctionBaseVO;
  }
}
