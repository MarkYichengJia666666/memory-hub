package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.banner.BannerListResponse;
import com.miyou.utilities.AppResourceResponseVO;
import com.miyou.utilities.McAppResourceConvertUtil;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.general.mcresource.AppResourceManagerService;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.mc.common.vo.appresource.AppResourceBaseInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.collections.CollectionUtils;

public interface IBannerParamCreateService extends IComponentParamCreateService {
  Function<ComponentParamFunctionBaseVO, BannerComponentBaseParam> BANNER_FUNCTION = (paramFunctionBaseVO) -> {
    return ((IBannerParamCreateService) paramFunctionBaseVO.getCreateComponentParamService()).createBannerComponentParam(paramFunctionBaseVO);
  };


  default BannerComponentBaseParam createBannerComponentParam(ComponentParamFunctionBaseVO componentParamFunctionBaseVO) {
    HomePageContext homePageContext = componentParamFunctionBaseVO.getHomePageContext();
    UserDeviceContextVO userDeviceContextVO = homePageContext.getUserDeviceContextVO();
    HashMap<String, Object> params = Maps.newHashMap();
    AppResourceExtraParam.APP_CURRENT_OPEN_TIME.setValue(params, userDeviceContextVO.getAppCurrentOpenTime());
    AppResourceManagerService appResourceManagerService = homePageContext.getHomePageServiceManager().getAppResourceManagerService();
    GeneralPageConfigParam param = GeneralPageConfigParam.fromWithTriggerSource(homePageContext.getUserId(), homePageContext.getSdkType(),
        userDeviceContextVO.getBuild(), homePageContext.getStatus(), userDeviceContextVO.getDeviceToken(),
        userDeviceContextVO.getSourceType(), homePageContext.getTriggerSource(), params);
    Map<String, List<AppResourceBaseInfo>> appResourceMap =
        appResourceManagerService.getResourceFromMc(param, Lists.newArrayList(GeneralPageConfigType.BANNER));
    AppResourceResponseVO bannerResource = McAppResourceConvertUtil.buildResponse(appResourceMap, GeneralPageConfigType.BANNER);
    Object banner = BannerListResponse.from(bannerResource.resource);
    if (CollectionUtils.isEmpty(bannerResource.sourceIds)) {
      return null;
    }
    return new BannerComponentBaseParam(componentParamFunctionBaseVO.getComponentType(), banner);
  }


  class BannerComponentBaseParam extends ComponentBaseParam {
    public Object banner;
    public ComponentType componentType;

    public BannerComponentBaseParam(ComponentType componentType, Object banner) {
      super(componentType);
      this.banner = banner;
      this.componentType = componentType;
    }

  }

}
