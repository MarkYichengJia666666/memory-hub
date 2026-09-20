package com.miyou.controllers.cashloan.repayment.billpage.strategy;

import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Builder
@AllArgsConstructor
public class BillPageDisplayableInstalment {
  public final List<CashLoanInstalmentVO> unpaidInstalmentList;
  public final List<CashLoanInstalmentVO> paidInstalmentList;

  public BillPageDisplayableInstalment.BillPageDisplayableInstalmentBuilder mutate() {
    return BillPageDisplayableInstalment.builder()
        .unpaidInstalmentList(this.unpaidInstalmentList)
        .paidInstalmentList(this.paidInstalmentList);
  }
}
