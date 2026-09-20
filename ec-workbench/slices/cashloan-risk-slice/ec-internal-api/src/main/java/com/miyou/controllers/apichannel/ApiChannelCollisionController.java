package com.miyou.controllers.apichannel;

import com.yqg.core.service.apichannel.ApiChannelCollisionResultService;
import com.yqg.ec.common.spring.request.apichannel.EcCheckCollisionResultRequest;
import com.yqg.ec.common.spring.response.apichannel.EcCheckCollisionResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API渠道撞库结果查询接口
 *
 */
@Slf4j
@RestController
@RequestMapping(path = "/ecInternalApi/apichannel/collision")
public class ApiChannelCollisionController {
  @Autowired
  private ApiChannelCollisionResultService apiChannelCollisionResultService;

  /**
   * 查询手机号是否撞库通过
   *
   * @param request 请求参数，包含+62格式的手机号列表
   * @return 撞库结果响应，包含每个手机号对应的撞库通过表项信息
   */
  @PostMapping("/checkResult")
  public EcCheckCollisionResultResponse checkCollisionResult(@Valid @RequestBody EcCheckCollisionResultRequest request) {
    Map<String, List<com.yqg.core.service.apichannel.vo.CollisionResultVO>> voMap =
        apiChannelCollisionResultService.checkCollisionResult(request.normalizedMobileNumbers);
    
    // 将VO转换为响应对象
    Map<String, List<EcCheckCollisionResultResponse.CollisionResultInfo>> resultMap = voMap.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(vo -> EcCheckCollisionResultResponse.CollisionResultInfo.builder()
                    .id(vo.getId())
                    .mobileNumberMd5(vo.getMobileNumberMd5())
                    .apiChannel(vo.getApiChannel())
                    .timeCreated(vo.getTimeCreated())
                    .reason(vo.getReason())
                    .lossDays(vo.getLossDays())
                    .build())
                .collect(Collectors.toList())));
    
    return EcCheckCollisionResultResponse.from(resultMap);
  }
}
