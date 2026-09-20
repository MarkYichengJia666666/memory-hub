package com.yqg.core.service.cashloan.repay;

import com.google.common.collect.ImmutableMap;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class UnionRepaymentFactory {
  @Autowired
  private List<UnionRepaymentHandler> handlers;

  private static Map<UnionRepaymentType, UnionRepaymentHandler> TYPE_HANDLER_MAP;

  @PostConstruct
  public void init() {
    ImmutableMap.Builder<UnionRepaymentType, UnionRepaymentHandler> builder = new ImmutableMap.Builder<>();
    for (UnionRepaymentHandler handler : handlers) {
      builder.put(handler.getSupportedType(), handler);
    }
    TYPE_HANDLER_MAP = builder.build();
  }

  public static UnionRepaymentHandler getMethod(UnionRepaymentType type) {
    return TYPE_HANDLER_MAP.get(type);
  }
}
