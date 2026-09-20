package com.yqg.core.service.risk.facade.enums;

import lombok.Getter;

/**
 * @author chenxianrui
 * @date 2025/8/24
 */
@Getter
public enum CreditCheckResult {
  REJECTED,    // 发送拒绝消息
  SUCCESS,     // 发送成功消息
  NEEDS_HANDLING // 不需要发送消息
}
