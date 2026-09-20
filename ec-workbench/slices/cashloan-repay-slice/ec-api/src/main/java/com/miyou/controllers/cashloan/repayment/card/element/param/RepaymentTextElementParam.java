package com.miyou.controllers.cashloan.repayment.card.element.param;

import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElementParam;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Builder
@AllArgsConstructor
public class RepaymentTextElementParam implements IElementParam {
  public final String iconUrl;
  public final Map<String /* highlight placeholder */, String /* replacement */> highlightMap;
  public final String highlightColor;
  public final Integer fontSize;
  public final String textColor;
  public final String blockText;
  public final String itemBackgroundColor;
}