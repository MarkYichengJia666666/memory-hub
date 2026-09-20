package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;

/**
 * 工厂接口
 * HomepageResponseV5中的每个字段理论上都由对应的该类型的工厂生成
 * 比如：
 * HomepageResponseV5.userInfo 由HomePageUserInfoResponseResponseFactory生成
 * HomepageResponseV5.product 由HomePageProductResponseFactory生成
 *
 * @param <V>
 */
public interface IHomePageResponseFactory<V extends HomePageResponseFields> {
  V getResult(HomePageContext homePageContext);

  void setValue(HomepageResponseV5 homepageResponseV5, V value);

  HomepageProcessorType getFactoryType();

}