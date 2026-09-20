package com.yqg.core.service.loan.collision.vo;

import lombok.Data;

/**
 * @author chenxianrui
 * @date 2025/2/7
 */
@Data
public class CheckSuccessUserVO {
  public String phoneMd5;
  public String apiChannel;
  public String reason;
  public Long lossDays;

  public static CheckSuccessUserVO from(String phoneMd5, String apiChannel, String reason, Long lossDays) {
    CheckSuccessUserVO checkExistUserVO = new CheckSuccessUserVO();
    checkExistUserVO.phoneMd5 = phoneMd5;
    checkExistUserVO.apiChannel = apiChannel;
    checkExistUserVO.reason = reason;
    checkExistUserVO.lossDays = lossDays;
    return checkExistUserVO;
  }

  public static CheckSuccessUserVO from(String phoneMd5, String apiChannel, String reason) {
    return from(phoneMd5, apiChannel, reason, -1L);
  }
}
