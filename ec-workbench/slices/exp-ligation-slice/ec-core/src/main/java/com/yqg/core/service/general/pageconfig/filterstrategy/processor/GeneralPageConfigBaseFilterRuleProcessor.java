package com.yqg.core.service.general.pageconfig.filterstrategy.processor;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.ObtainRulePayloadType;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.payload.BaseFilterRulePayload;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.ec.common.serialization.JsonUtils;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public abstract class GeneralPageConfigBaseFilterRuleProcessor<T extends BaseFilterRulePayload> {

  private static final BigDecimal ONE_HUNDRED_THOUSAND = new BigDecimal("100000");

  public abstract GeneralPageConfigFilterRuleType getRuleType();

  public abstract void checkPayload(T payload);

  public abstract boolean hitRule(T payload, GeneralPageConfigParam param);

  public Class<? extends BaseFilterRulePayload> getPayloadClass() {
    ParameterizedType superGenericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
    return (Class<? extends BaseFilterRulePayload>) superGenericSuperclass.getActualTypeArguments()[0];
  }

  public BaseFilterRulePayload getPayload(String payload, GeneralPageConfigFilterRuleType type, ObtainRulePayloadType obtainRUlePayloadType) {
    BaseFilterRulePayload baseFilterRulePayload = JsonUtils.fromOrException(payload, getPayloadClass());
    return obtainRUlePayloadType.decoratRule.apply(new ObtainRulePayloadType.BaseFilterRuleDecoratorVO(baseFilterRulePayload, type));
  }

  public boolean checkUserLoanParamLegal(Map<String, Object> extraParams) {
    if (extraParams == null) {
      return false;
    }
    BigDecimal userInputAmount = AppResourceExtraParam.USER_INPUT_AMOUNT.getOrNull(extraParams);
    String productId = AppResourceExtraParam.SELECTED_PRODUCT_ID_HASH.getOrNull(extraParams);

    if (BigDecimalHelper.isNullOrZero(userInputAmount) || BigDecimalHelper.lessThan(userInputAmount, ONE_HUNDRED_THOUSAND) || StringUtils.isBlank(productId)) {
      return false;
    }

    return true;
  }

}
