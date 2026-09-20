package com.yqg.core.configure;

import com.yqg.ec.common.configuration.ISiteVars;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BindCardRequestConfig {

  @Autowired
  private ISiteVars siteVars;

  public Boolean checkNeedInsertBindCardRequestNormal() {
    return siteVars.getBoolean("risk.bind_card_request.create", false);
  }

}
