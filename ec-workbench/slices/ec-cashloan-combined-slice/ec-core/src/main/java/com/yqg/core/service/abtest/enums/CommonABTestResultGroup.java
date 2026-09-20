package com.yqg.core.service.abtest.enums;

import com.yqg.common.spring.util.enums.DescriptionBaseEnum;

/**
 * 通用的分流结果
 */
public enum CommonABTestResultGroup implements DescriptionBaseEnum {

  A("A"),
  B("B"),
  C("C"),
  D("D"),
  E("E"),
  ONE("1"),
  TWO("2"),
  THREE("3"),
  ;

  public String desc;

  CommonABTestResultGroup(String desc) {
    this.desc = desc;
  }

  @Override
  public String getDescription() {
    return desc;
  }
}
