package com.miyou.controllers.image;

import com.miyou.controllers.image.requests.ReuploadInfoRequest;
import com.miyou.controllers.loan.BaseLoanController;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.yqg.ec.common.enums.Label;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.core.service.reupload.enums.ReuploadInfoType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class RiskReuploadController extends BaseLoanController {
  @Autowired
  private RiskReuploadService reuploadService;

  @ECSecuredApi
  @GetMapping("/api/loan/reupload/getTypes")
  public Result getResubmitInfoTypes() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    List<ReuploadInfoType> typeList = reuploadService.getInfoNotUploaded(viewerContext.loanAccountId);
    return EcResponseUtil.generate(typeList
        .stream()
        .map(type -> Label.gen(type.desc(), type.name()))
        .collect(Collectors.toList()));
  }

  @ECSecuredApi
  @PostMapping("/api/loan/reupload")
  public Result reupload(@RequestBody ReuploadInfoRequest request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    reuploadService.upload(viewerContext.userSdkType, viewerContext.loanAccountId, viewerContext.userId, request.documents);
    return EcResponseUtil.generateSuccess();
  }
}
