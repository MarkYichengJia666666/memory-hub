package com.yqg.core.service.abtest.vo;

import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.ec.common.exception.EcException;
import com.yqg.experiment.common.enums.ResultGetType;

public class ABTestUserIdRequestVO extends ABTestBaseRequestVO {

  public static ABTestUserIdRequestVO from(ExperimentNameSpace experimentNameSpace, Long userId) {
    ABTestUserIdRequestVO requestVO = new ABTestUserIdRequestVO();
    requestVO.experimentNameSpace = experimentNameSpace;
    requestVO.userId = userId;
    return requestVO;
  }

  public static ABTestUserIdRequestVO from(ExperimentNameSpace experimentNameSpace, Long userId, ResultGetType resultGetType) {
    ABTestUserIdRequestVO requestVO = new ABTestUserIdRequestVO();
    requestVO.experimentNameSpace = experimentNameSpace;
    requestVO.userId = userId;
    requestVO.resultGetType = resultGetType;
    return requestVO;
  }

  @Override
  public DiversionKeyType getDiversionKeyType() {
    return DiversionKeyType.USER_ID;
  }

  @Override
  public void checkDiversionKey() {
    if (userId == null) {
      throw EcException.error("filed diversion key is null!");
    }
  }
}
