package com.yqg.core.service.cashloan.homepage.enums;

import com.yqg.common.spring.util.enums.DescriptionBaseEnum;

public enum HomePageScene implements DescriptionBaseEnum {
  PULL_REFRESH("下拉刷新"),
  BACKGROUND("从后台返回前台"),
  DEFAULT("默认");

  private final String description;

  HomePageScene(String description) {
    this.description = description;
  }

  public static HomePageScene fromName(String name) {
    if (name == null) {
      return DEFAULT;
    }
    for (HomePageScene scene : HomePageScene.values()) {
      if (scene.name().equalsIgnoreCase(name)) {
        return scene;
      }
    }
    return DEFAULT;
  }

  @Override
  public String getDescription() {
    return description;
  }
}