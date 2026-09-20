package com.miyou.controllers.cashloan.repayment.calculate;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class RepaymentCalculateResponse {

  public final RepaymentCollectionReductionCalculateResponse collectionReduction;
}
