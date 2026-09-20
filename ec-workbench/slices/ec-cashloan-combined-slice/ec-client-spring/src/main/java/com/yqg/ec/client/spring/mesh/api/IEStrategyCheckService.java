package com.yqg.ec.client.spring.mesh.api;

import com.yqg.ec.common.spring.request.CheckStrategyRequest;
import com.yqg.ec.common.spring.response.CheckStrategyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@EcFeignServiceMeshClient
public interface IEStrategyCheckService {


  @GetMapping("/ecInternalApi/checkStrategy/userId")
  Boolean checkStrategyUserId(@RequestParam("strategyId") Long strategyId, @RequestParam("userId") Long userId);

  @GetMapping("/ecInternalApi/checkStrategy/deviceId")
  Boolean checkStrategyDeviceId(@RequestParam("strategyId") Long strategyId, @RequestParam("deviceId") String deviceId);

  @PostMapping("/ecInternalApi/checkStrategy")
  CheckStrategyResponse checkStrategy(@RequestBody CheckStrategyRequest checkStrategyRequest);
}
