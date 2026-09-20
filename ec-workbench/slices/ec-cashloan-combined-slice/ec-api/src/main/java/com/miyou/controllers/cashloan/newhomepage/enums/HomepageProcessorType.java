package com.miyou.controllers.cashloan.newhomepage.enums;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.HomePageResponseFields;
import com.miyou.controllers.cashloan.newhomepage.content.vo.FactoryFilterVO;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.processormap.*;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.miyou.controllers.cashloan.response.v5.alert.AlertResponse;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageConfigResponse;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.product.ProductListResponse;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@AllArgsConstructor
@Slf4j
public enum HomepageProcessorType {
  PRODUCT_INFO(
      (item) -> {
        if (item instanceof IProductProcessorSelector) {
          return ((IProductProcessorSelector) item).getProductProcessorList();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomepageProductProcessorType.values()),
      ProductListResponse::new,
      "产品信息"),
  USER_INFO(
      (item) -> {
        if (item instanceof IUserInfoProcessorSelector) {
          return ((IUserInfoProcessorSelector) item).getUserInfoProcessorList();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomepageUserInfoProcessorType.values()),
      UserResponse::new,
      "用户信息"),

  BURIED_PINT_INTO((item) -> {
    if (item instanceof IPointProcessorSelector) {
      return ((IPointProcessorSelector) item).getPointProcessorList();
    }
    return Lists.newArrayList();
  }, () -> Arrays.asList(HomepagePointProcessorType.values()),
      HomePagePointResponse::new, "埋点信息"),
  MIDDLE_INFO(
      (item) -> {
        if (item instanceof IMiddleProcessorSelector) {
          return ((IMiddleProcessorSelector) item).getMiddleProcessorList();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomepageMiddleProcessorType.values()),
      MiddleListResponse::new,
      (vo) -> vo.isLevel1Page(),
      "中通图信息"),

  TOP_AREA((item) -> {
    if (item instanceof ITopAreaProcessorSelector) {
      return ((ITopAreaProcessorSelector) item).getTopAreaProcessorList();
    }
    return Lists.newArrayList();
  }, () -> Arrays.asList(HomepageTopAreaProcessorType.values()),
      TopAreaResponse::new, (vo) -> vo.isLevel1Page() && !vo.pageVersion3(),
      "top area"),
  ALERT_INFO(
      (item) -> {
        if (item instanceof IAlertProcessorSelector) {
          return ((IAlertProcessorSelector) item).getAlertProcessorList();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(AlertProcessorType.values()),
      AlertResponse::new, "提醒信息"),
  APP_RESOURCE((item) -> {
    if (item instanceof IAppResourceProcessorSelector) {
      return ((IAppResourceProcessorSelector) item).getAppResourceProcessorList();
    }
    return Lists.newArrayList();
  }, () -> Arrays.asList(AppResourceProcessorType.values()),
      HomeAppResourceResponse::new,
      FactoryThreadGroup.APP_RESOURCE,
      "资源位"),

  NOTICE_INFO(
      (item) -> {
        if (item instanceof INoticeProcessorSelector) {
          return ((INoticeProcessorSelector) item).getNoticeProcessorList();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomepageNoticeProcessorType.values()),
      NoticeListResponse::new,
      (vo) -> !vo.pageVersion3(),
      "通知信息"),

  PAGE_CARD_INFO_V3(
      (item) -> {
        if (item instanceof IPageCardV3ProcessorSelector) {
          return ((IPageCardV3ProcessorSelector) item).getPageCardSelector();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(PageCardV3ProcessorType.values()),
      PageCardV3VO::new,
      (vo) -> vo.isLevel1Page() && vo.pageVersion3(),
      "v3首页卡片元素"),

  PAGE_USER_INFO_V3(
      (item) -> {
        if (item instanceof IPageUserInfoV3Selector) {
          return ((IPageUserInfoV3Selector) item).getPageUserInfoV3Type();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomepageUserInfoV3ProcessorType.values()),
      PageUserInfoV3Response::new, "用户信息V3"),
  PAGE_CONFIG(
      (item) -> {
        if (item instanceof IPageConfigSelector) {
          return ((IPageConfigSelector) item).getPageConfigProcessorTypes();
        }
        return Lists.newArrayList();
      },
      () -> Arrays.asList(HomePageConfigProcessorType.values()),
      HomepageConfigResponse::new, "首页配置");


  @Getter
  private Function<IStatusProcessorSelector, List<? extends HomepageBaseInfoProcessorType>> processorListFunction;
  @Getter
  private Supplier<? extends List<HomepageBaseInfoProcessorType>> processorType;
  private Supplier<? extends HomePageResponseFields> resultInstanceSupplier;
  @Getter
  private Predicate<FactoryFilterVO> predicate = (p) -> true;
  @Getter
  private FactoryThreadGroup factoryThreadGroup;
  private String desc;

  HomepageProcessorType(Function<IStatusProcessorSelector,
      List<? extends HomepageBaseInfoProcessorType>> processorListFunction,
                        Supplier<? extends List<HomepageBaseInfoProcessorType>> processorType,
                        Supplier<? extends HomePageResponseFields> resultInstanceSupplier,
                        String desc) {
    this.processorListFunction = processorListFunction;
    this.processorType = processorType;
    this.resultInstanceSupplier = resultInstanceSupplier;
    this.factoryThreadGroup = FactoryThreadGroup.MAIN;
    this.desc = desc;
  }

  HomepageProcessorType(Function<IStatusProcessorSelector,
      List<? extends HomepageBaseInfoProcessorType>> processorListFunction,
                        Supplier<? extends List<HomepageBaseInfoProcessorType>> processorType,
                        Supplier<? extends HomePageResponseFields> resultInstanceSupplier,
                        Predicate<FactoryFilterVO> predicate,
                        String desc) {
    this.processorListFunction = processorListFunction;
    this.processorType = processorType;
    this.resultInstanceSupplier = resultInstanceSupplier;
    this.predicate = predicate;
    this.factoryThreadGroup = FactoryThreadGroup.MAIN;
    this.desc = desc;
  }

  HomepageProcessorType(Function<IStatusProcessorSelector,
      List<? extends HomepageBaseInfoProcessorType>> processorListFunction,
                        Supplier<? extends List<HomepageBaseInfoProcessorType>> processorType,
                        Supplier<? extends HomePageResponseFields> resultInstanceSupplier,
                        FactoryThreadGroup factoryThreadGroup,
                        String desc) {
    this.processorListFunction = processorListFunction;
    this.processorType = processorType;
    this.resultInstanceSupplier = resultInstanceSupplier;
    this.factoryThreadGroup = factoryThreadGroup;
    this.desc = desc;
  }

  public <T extends HomepageBaseInfoProcessorType> List<T> getProcessorList(IStatusProcessorSelector item) {
    return (List<T>) processorListFunction.apply(item);
  }

  public <V extends HomePageResponseFields> V newResultInstance() {
    return (V) resultInstanceSupplier.get();
  }
}
