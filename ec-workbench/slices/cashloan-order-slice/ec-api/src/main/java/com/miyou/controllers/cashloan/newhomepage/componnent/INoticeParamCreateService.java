package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;

import java.util.function.Function;

public interface INoticeParamCreateService extends IComponentParamCreateService{
  Function<ComponentParamFunctionBaseVO, NoticeComponentBaseParam> NOTICE_FUNCTION = (paramFunctionBaseVO) -> {
      return ((INoticeParamCreateService)paramFunctionBaseVO.getCreateComponentParamService()).createNoticeComponentParam(paramFunctionBaseVO);
  };

  default NoticeComponentBaseParam createNoticeComponentParam(ComponentParamFunctionBaseVO componentParamFunctionBaseVO) {
    // todo 具体状态实现
    return new NoticeComponentBaseParam(componentParamFunctionBaseVO.getComponentType(), new NoticeListResponse());
  }

  class NoticeComponentBaseParam extends ComponentBaseParam {
    public NoticeListResponse notice;

    public NoticeComponentBaseParam(ComponentType componentType, NoticeListResponse notice) {
      super(componentType);
      this.notice = notice;
    }

  }
}
