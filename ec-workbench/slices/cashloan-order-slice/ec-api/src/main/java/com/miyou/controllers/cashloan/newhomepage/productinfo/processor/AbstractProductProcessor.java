package com.miyou.controllers.cashloan.newhomepage.productinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProductProcessorType;
import com.miyou.controllers.cashloan.response.v5.product.ProductListResponse;

public abstract class AbstractProductProcessor implements IHomePageFieldProcessor<ProductListResponse, HomepageProductProcessorType> {

  @Override
  public HomepageProductProcessorType getProcessorType() {
    return getProductProcessorType();
  }

  protected abstract HomepageProductProcessorType getProductProcessorType();
}
