package com.yqg.core.service.abtest.vo;

import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.ec.common.exception.EcException;
import com.yqg.experiment.common.enums.ResultGetType;

public abstract class ABTestBaseRequestVO {
  public ExperimentNameSpace experimentNameSpace;
  public Long userId;
  public String deviceToken;
  public String nik;
  public ResultGetType resultGetType;


  public abstract DiversionKeyType getDiversionKeyType();

  public abstract void checkDiversionKey();

  public void checkExperimentSpace() {
    if (this.experimentNameSpace == null) {
      throw EcException.error("experimentNameSpace can not be null");
    }
  }

}
