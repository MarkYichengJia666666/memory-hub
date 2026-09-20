package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;

import java.util.List;

/**
 * 状态 -> 组件list
 */
public interface IStatusComponentTypeService {
  IDNHomepageLoanStatusV5 getStatus();

  /**
   * 获取当前状态下的组件列表
   *
   * @param homePageContext
   * @return
   */
  List<ComponentType> getComponentTypeList(HomePageContext homePageContext);
}
