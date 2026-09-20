package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.componnent.ComponentType;
import com.miyou.controllers.cashloan.newhomepage.componnent.IComponentCardParamCreateCommonService;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotLoginSelector implements IUserInfoProcessorSelector,
    IMiddleProcessorSelector, ICommonProcessorSelector, INoticeProcessorSelector,
    IComponentCardParamCreateCommonService {

  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.NOT_LOGIN;
  }


  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.NOT_LOGIN_USER_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.RECENTLY_ORDERS_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<ComponentType> getComponentTypeList(HomePageContext homePageContext) {
    // 根据首页类型获取需要的组件
    return Lists.newArrayList(ComponentType.BANNER, ComponentType.NOTICE);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.NOT_LOGIN, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }
}
