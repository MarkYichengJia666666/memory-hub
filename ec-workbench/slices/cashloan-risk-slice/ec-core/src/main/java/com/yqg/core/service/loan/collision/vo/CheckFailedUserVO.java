package com.yqg.core.service.loan.collision.vo;

/**
 * @author chenxianrui
 * @date 2025/2/7
 */
public class CheckFailedUserVO {
  public String phoneMd5;
  public String apiChannel;
  public String failureReason;

  public static CheckFailedUserVO from(String phoneMd5, String apiChannel, String failureReason) {
    CheckFailedUserVO checkExistUserVO = new CheckFailedUserVO();
    checkExistUserVO.phoneMd5 = phoneMd5;
    checkExistUserVO.apiChannel = apiChannel;
    checkExistUserVO.failureReason = failureReason;
    return checkExistUserVO;
  }
}
