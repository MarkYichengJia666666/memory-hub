package com.miyou.controllers.app;

import com.miyou.controllers.app.request.AppStartupRequest;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.utilities.secureapi.RequestParser;
import com.miyou.utilities.secureapi.UserLoginService;
import com.yqg.core.service.app.AppStartupService;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for app-related endpoints
 *
 * @author lihancock
 */
@Slf4j
@RestController
public class AppController extends YqgBaseController {

  @Autowired
  private AppStartupService appStartupService;

  @Autowired
  private UserLoginService userLoginService;

  /**
   * Endpoint for app startup event
   * This endpoint is called when the app starts up, either as a cold start or hot start
   *
   * @param request Request containing isColdStart flag
   * @return Success response
   */
  @PostMapping("/api/app/startup")
  public Result startup(@RequestBody @Valid AppStartupRequest request) {
    if (userLoginService.isUserLogin(ecRequest())) {
      Long userId = userLoginService.getUserId(ecRequest());
      userLoginService.refreshBuildByUserToken(RequestParser.getUserToken(ecRequest()), RequestParser.getBuild(ecRequest()));
      // Send the app startup event
      appStartupService.processAppStartupEvent(userId, ImpliedContextUtils.requestClientType(), request.getOpenAppScene());
    }
    return EcResponseUtil.generateSuccess();
  }
}
