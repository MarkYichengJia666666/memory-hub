package com.miyou.controllers.cashloan.newhomepage.alert;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.alert.processor.AbstractAlertInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.alert.AlertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomePageAlertResponseFactory extends AbstractHomePageResponseFactory<AlertResponse, AlertProcessorType, AbstractAlertInfoProcessor> {

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.ALERT_INFO;
  }

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, AlertResponse value) {
    homepageResponseV5.setAlert(value);
  }

}