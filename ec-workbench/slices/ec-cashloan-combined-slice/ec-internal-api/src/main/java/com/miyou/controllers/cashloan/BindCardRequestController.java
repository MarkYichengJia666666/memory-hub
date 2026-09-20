package com.miyou.controllers.cashloan;

import com.yqg.core.aop.StatQueryRequest;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.core.model.generated.tables.records.BindCardRequestRecord;
import com.yqg.core.model.sql.bankaccount.BindCardRequestModel;
import com.yqg.ec.common.spring.response.EcBindCardRequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : haoranzhao
 * @date : 2023-03-28
 **/

@Slf4j
@RestController
@RequestMapping("/ecInternalApi/bindCardRequest")
public class BindCardRequestController {
  //TODO(LTB,T000000) 上下层隔离
  @Autowired
  BindCardRequestModel bindCardRequestModel;

  @StatQueryRequest
  @GetMapping(path = "/queryByUserId")
  public List<EcBindCardRequestInfo> queryByUserId(@RequestParam(value = "userId") Long userId,
                                                   @RequestParam(value = "sdkType") String sdkTypeCode,
                                                   @RequestParam(value = "endTime") Long endTime) {
    List<BindCardRequestRecord> records = bindCardRequestModel.findByUserId(userId, SDKType.fromCode(sdkTypeCode), endTime);
    return records.stream()
        .map(r -> new EcBindCardRequestInfo(r.getId(), r.getUserId(), r.getAccountNumber(), r.getBankCode(), r.getSdkType()))
        .collect(Collectors.toList());
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByAccountNumbers")
  public List<EcBindCardRequestInfo> queryByAccountNumbers(@RequestParam(value = "accountNumbers") Collection<String> accountNumbers,
                                                           @RequestParam(value = "endTime") Long endTime) {
    List<BindCardRequestRecord> records = bindCardRequestModel.findByAccountNumbers(accountNumbers, endTime);
    return records.stream()
        .map(r -> new EcBindCardRequestInfo(r.getId(), r.getUserId(), r.getAccountNumber(), r.getBankCode(), r.getSdkType()))
        .collect(Collectors.toList());
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByAccountNumberAndSdk")
  public List<EcBindCardRequestInfo> queryByAccountNumberAndSdk(@RequestParam(value = "accountNumbers") Collection<String> accountNumbers,
                                                                @RequestParam(value = "endTime") Long endTime,
                                                                @RequestParam(value = "sdkTypeList") List<String> sdkTypeCodes) {
    List<SDKType> sdkTypeList = sdkTypeCodes.stream().map(SDKType::fromCode).collect(Collectors.toList());
    List<BindCardRequestRecord> records = bindCardRequestModel.findByAccountNumbersIdn(accountNumbers, endTime, sdkTypeList);
    return records.stream()
        .map(r -> new EcBindCardRequestInfo(r.getId(), r.getUserId(), r.getAccountNumber(), r.getBankCode(), r.getSdkType()))
        .collect(Collectors.toList());
  }
}
