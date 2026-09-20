package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.response.v5.pagev3.H5WholeProcessOrderUIConfig;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ExpFacade.ClientType;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.ec.common.enums.SDKType;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * H5全流程下单跳转UI A/B测试服务
 * 负责H5全流程下单跳转UI相关的实验逻辑处理
 */
@Slf4j
@Service
public class H5WholeProcessOrderUIAbTestService {

  @Autowired
  private ExpFacade expFacade;

  /**
   * 获取H5全流程下单跳转UI实验结果
   * @param userId 用户ID
   * @param build 版本号
   * @param sdkType SDK类型
   * @return H5全流程下单跳转UI配置
   */
  public H5WholeProcessOrderUIConfig fetchH5WholeProcessOrderUIConfigResponse(Long userId, Long build, SDKType sdkType) {
    if (Objects.isNull(userId)) {
      return H5WholeProcessOrderUIConfig.defaultConfig();
    }
    
    boolean enableNewAnimation = false;
    
    try {
      ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ExperimentNameSpace.H5_WHOLE_PROCESS_ORDER_UI_JUMP, userId);
      String result = expFacade.fetchResultFallBackWithDefaultScene("technology-lending-abroad-loan_all-h5_ui_new_order_1215",
          ClientType.DIVERSION, requestVO, build);
      
      if (CommonABTestResultGroup.B.name().equals(result)) {
        enableNewAnimation = true;
      }
    } catch (Exception e) {
      log.error("fetchH5OrderFlowJumpUIConfigResponse error, userId:{}", userId, e);
    }
    
    return H5WholeProcessOrderUIConfig.create(enableNewAnimation);
  }
}