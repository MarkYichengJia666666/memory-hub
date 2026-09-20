package com.miyou.controllers.cashloan.newhomepage.productinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProductProcessorType;
import org.springframework.stereotype.Service;

@Service
public class EmptyProductProcessor extends AbstractProductProcessor {

  @Override
  protected HomepageProductProcessorType getProductProcessorType() {
    return HomepageProductProcessorType.DEFAULT_PRODUCT_INFO_PROCESSOR;
  }
}
