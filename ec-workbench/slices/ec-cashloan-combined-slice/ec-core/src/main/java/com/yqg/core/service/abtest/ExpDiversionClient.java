package com.yqg.core.service.abtest;

import com.yqg.experiment.client.service.IExperimentRpcService;
import com.yqg.experiment.common.domain.ExperimentParams;
import com.yqg.experiment.common.domain.vo.ExperimentDataVO;
import org.springframework.stereotype.Component;

/**
 * @Description 进行实验分流，返回相应结果
 * 使用前考虑三点 1.api渠道是否要屏蔽；2.版本号是否满足；3.线程内是否自动注入了用户信息
 * @Author: lihancock
 * @Email: wenyaoli@fintopia.tech
 * @Date: 2025/9/16 19:57
 */
@Component
public class ExpDiversionClient extends AbstractExpClient {

  @Override
  protected ExperimentDataVO callExperimentPlatform(IExperimentRpcService experimentRpcService, ExperimentParams experimentParams) {
    return experimentRpcService.abTest(experimentParams);
  }

}
