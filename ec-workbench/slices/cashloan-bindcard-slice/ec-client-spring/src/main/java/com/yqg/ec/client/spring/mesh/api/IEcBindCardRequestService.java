package com.yqg.ec.client.spring.mesh.api;

import com.yqg.ec.common.spring.response.EcBindCardRequestInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * @author : haoranzhao
 * @date : 2023-03-28
 **/

@EcFeignServiceMeshClient
public interface IEcBindCardRequestService {
  @GetMapping(path = "/ecInternalApi/bindCardRequest/queryByUserId")
  List<EcBindCardRequestInfo> queryByUserId(@RequestParam(value = "userId") Long userId, @RequestParam(value = "sdkType") String sdkTypeCode, @RequestParam(value = "endTime") Long endTime);

  @GetMapping(path = "/ecInternalApi/bindCardRequest/queryByAccountNumbers")
  List<EcBindCardRequestInfo> queryByAccountNumbers(@RequestParam(value = "accountNumbers") Collection<String> accountNumbers, @RequestParam(value = "endTime") Long endTime);

  @GetMapping(path = "/ecInternalApi/bindCardRequest/queryByAccountNumberAndSdk")
  List<EcBindCardRequestInfo> queryByAccountNumberAndSdk(@RequestParam(value = "accountNumbers") Collection<String> accountNumbers, @RequestParam(value = "endTime") Long endTime, @RequestParam(value = "sdkTypeList") List<String> sdkTypeCodes);
}
