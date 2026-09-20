package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.core.service.cashloan.observer.EasycashObserver;
import com.yqg.core.service.cashloan.observer.IRiskObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/8/13 3:43 下午
 */
@Component
public class EasycashRiskProcessor extends BaseRiskEventProcessor {
  @Autowired
  private EasycashObserver observer;

  @Override
  protected IRiskObserver getObserver() {
    return observer;
  }
}
