package com.yqg.core.service.general.pageconfig.filterstrategy.function;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorBoolean;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.yqg.core.service.general.pageconfig.filterstrategy.GeneralPageConfigFilterRuleService;
import com.yqg.core.service.general.pageconfig.filterstrategy.threadlocal.GeneralPageConfigFilterThreadLocal;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GeneralPageConfigFilterRuleFunction extends AbstractFunction {

  @Autowired
  private GeneralPageConfigFilterRuleService ruleService;

  @Override
  public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
    Long ruleId = (Long) FunctionUtils.getNumberValue(arg1, env);
    GeneralPageConfigParam param = GeneralPageConfigFilterThreadLocal.getParam();
    //优先从缓存中拿result
    Boolean result = GeneralPageConfigFilterThreadLocal.getRuleResult(ruleId);
    if (result == null) {
      Long startTime = Clock.now();
      result = ruleService.hitRule(ruleId, param);
      GeneralPageConfigFilterThreadLocal.setRuleResult(ruleId, result, Clock.now() - startTime);
    }
    return AviatorBoolean.valueOf(result);
  }

  @Override
  public String getName() {
    return "rule";
  }
}
