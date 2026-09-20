package com.miyou.controllers.cashloan.newhomepage.productinfo;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProductProcessorType;
import com.miyou.controllers.cashloan.newhomepage.productinfo.processor.AbstractProductProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.product.ProductListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomePageProductResponseFactory extends AbstractHomePageResponseFactory<ProductListResponse, HomepageProductProcessorType, AbstractProductProcessor> {

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.PRODUCT_INFO;
  }


  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, ProductListResponse value) {
    homepageResponseV5.setProduct(value);
  }
}