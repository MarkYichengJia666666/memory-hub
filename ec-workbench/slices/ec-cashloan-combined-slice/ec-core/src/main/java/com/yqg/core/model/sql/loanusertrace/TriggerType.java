package com.yqg.core.model.sql.loanusertrace;

import com.yqg.common.spring.util.enums.DescriptionBaseEnum;
import com.yqg.ec.common.exception.EcException;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/6/23 6:53 下午
 */
public enum TriggerType implements DescriptionBaseEnum {
  MANUAL("M", "用户自行手动触发", TriggerSubType.MANUAL),
  AUTOMATIC("A", "流程自动触发", TriggerSubType.AUTOMATIC),

  ;

  public String code;
  public String desc;
  public TriggerSubType subType;

  TriggerType(String code, String desc, TriggerSubType subType) {
    this.code = code;
    this.desc = desc;
    this.subType = subType;
  }

  public static TriggerType fromCode(String code) {
    for (TriggerType triggerType : TriggerType.values()) {
      if (triggerType.code.equals(code)) {
        return triggerType;
      }
    }
    throw EcException.error("can not find code for TriggerType,code is {}", code);
  }

  @Override
  public String getDescription() {
    return desc;
  }
}
