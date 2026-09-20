package com.miyou.controllers.cashloan.newhomepage.componnent;

/**
 * 组件参数基类
 */
public abstract class ComponentBaseParam {

  private ComponentType componentType;

  public ComponentBaseParam(ComponentType componentType) {
    this.componentType = componentType;
  }

  public ComponentType getComponentType() {
    return this.componentType;
  }

  /**
   * 是否可以展示--具体应该有自己的判断逻辑
   *
   * @return
   */
  public boolean canDisplay() {
    return true;
  }
}
