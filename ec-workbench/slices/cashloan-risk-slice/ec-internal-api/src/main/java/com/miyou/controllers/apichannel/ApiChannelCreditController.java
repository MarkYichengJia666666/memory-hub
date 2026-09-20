package com.miyou.controllers.apichannel;

import com.miyou.controllers.apichannel.utils.ApiChannelCommonConverter;
import com.miyou.controllers.apichannel.utils.ApiChannelProductConverter;
import com.yqg.core.model.generated.tables.records.CashLoanOrderRecord;
import com.yqg.core.model.sql.cashloan.order.CashLoanOrderModel;
import com.yqg.core.model.sql.loan.account.LoanAccountDetailsModel;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.service.apichannel.common.CommonApiChannelService;
import com.yqg.core.service.apichannel.user.ApiChannelCreditsService;
import com.yqg.core.service.apichannel.user.ApiChannelProductService;
import com.yqg.core.service.apichannel.vo.ApiChannelCommonCreditData;
import com.yqg.core.service.apichannel.vo.ApiChannelProductAndTrialData;
import com.yqg.core.service.data.CommonQueryDataConverter;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsPojo;
import com.yqg.core.service.user.UserService;
import com.yqg.core.util.fileopreator.CloudFileUtil;
import com.yqg.ec.common.commonvo.apichannel.ApiChannelBaseVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelCreditApplyRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelCreditStatusRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelLoanAccountDetailsNewRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelLatestRiskAcceptRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelProductRequest;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelAllProductAndTrialResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelCreditStatusResponse;
import com.yqg.ec.common.spring.response.data.EcLoanAccountDetailsSimpleVO;
import com.yqg.ec.common.spring.response.data.EcLoanUserRiskTraceVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(path = "/ecInternalApi/apichannel/credit")
public class ApiChannelCreditController {

  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanAccountDetailsModel loanAccountDetailsModel;
  @Autowired
  private CashLoanOrderModel cashLoanOrderModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private CommonApiChannelService commonApiChannelService;
  @Autowired
  private ApiChannelCreditsService apiChannelCreditsService;
  @Autowired
  private ApiChannelProductService apiChannelProductService;
  @Autowired
  private UserService userService;
  @Autowired
  private CloudFileUtil cloudFileUtil;

  @PostMapping("/apply")
  public void applyCredit(@Valid @RequestBody ApiChannelCreditApplyRequest request) {
    try {
      ApiChannelBaseVO creditApplyVO = request.getApiChannelBaseVO();
      ApiChannel channel = ApiChannel.from(request.getChannel());
      UserInfoVO userInfoVO = UserInfoVO.from(request.getApiChannelUserInfoVO());

      String idFrontImageKey = request.getApiChannelBaseVO().getIdFrontImageKey();
      creditApplyVO.setIdFrontImageKey(idFrontImageKey);
      creditApplyVO.setIdFrontImageUrl(cloudFileUtil.getFileDownloadUrl(idFrontImageKey, 3600));

      List<String> faceImageKeys = request.getApiChannelBaseVO().getFaceImageKeys();
      creditApplyVO.setFaceImageKeys(faceImageKeys);
      creditApplyVO.setFaceImageUrl(cloudFileUtil.getFileDownloadUrl(
          faceImageKeys.get(0), 3600
      ));

      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(userInfoVO.getLoanAccountId());
      LoanApiViewerContext viewerContext = commonApiChannelService.getViewContextByAccountVO(loanAccountVO, SourceType.getSourceType(request.getSourceType()));
      LoanAccountDetailsPojo detailsPojo = ApiChannelCommonConverter.convertToLoanAccountDetailsPojo(creditApplyVO);
      commonApiChannelService.submitBaseInfo(viewerContext, detailsPojo);
      commonApiChannelService.submitCreditsApplication(viewerContext);
    } catch (Exception e) {
      log.error("Error in applyCredit", e);
      throw e;
    }
  }

  @PostMapping("/status")
  public ApiChannelCreditStatusResponse getCreditStatus(@Valid @RequestBody ApiChannelCreditStatusRequest request) {
    try {

      ApiChannel channel = ApiChannel.from(request.getChannel());
      UserInfoVO userInfoVO = userService.fetchById(request.getApiChannelUserInfoVO().getUserId(), SDKType.IDN_YQD);
      ApiChannelCommonCreditData creditData = apiChannelCreditsService.getCreditData(userInfoVO.userId, channel);

      return ApiChannelCreditStatusResponse.from(creditData.usedCredits,
          creditData.totalRemainCredits,
          creditData.totalCredits,
          creditData.creditStatus,
          creditData.latestApplyTime,
          creditData.timeUpdated,
          creditData.reapplyTime,
          creditData.loanCreditsRejectedReason,
          userInfoVO.userType);
    } catch (Exception e) {
      log.error("Error in getCreditStatus", e);
      throw e;
    }
  }

  @PostMapping("/productAndTrial")
  public ApiChannelAllProductAndTrialResponse getProductAndTrial(@Valid @RequestBody ApiChannelProductRequest request) {
    try {

      ApiChannel channel = ApiChannel.from(request.getChannel());
      UserInfoVO userInfoVO = UserInfoVO.from(request.getApiChannelUserInfoVO());

      List<ApiChannelProductAndTrialData> productAndTrialDataList = apiChannelProductService.getProductAndTrialData(userInfoVO.userId, channel);

      return ApiChannelProductConverter.getAllProductAndTrialResponse(productAndTrialDataList);

    } catch (Exception e) {
      log.error("Error in getProductAndTrial", e);
      throw e;
    }
  }

  @PostMapping("/latestRiskAccept")
  public EcLoanUserRiskTraceVO getLatestRiskAcceptByUserId(@Valid @RequestBody ApiChannelLatestRiskAcceptRequest request) {
    List<LoanUserRiskType> types = request.getRiskTypeCodes().stream()
        .map(LoanUserRiskType::fromCode)
        .collect(Collectors.toList());
    LoanUserRiskTraceRecord record = loanUserRiskTraceModel.findLatestRiskAcceptByUserId(request.getUserId(), types);
    return convertToEcLoanUserRiskTraceVO(record);
  }

  @PostMapping("/loanAccountDetailsNewSimple")
  public EcLoanAccountDetailsSimpleVO getLoanAccountDetailsNewSimpleByUserId(@Valid @RequestBody ApiChannelLoanAccountDetailsNewRequest request) {
    EcLoanAccountDetailsSimpleVO vo = CommonQueryDataConverter.convert(loanAccountDetailsModel.newFindByUserId(request.getUserId()));
    if (vo != null && vo.loanAccountId != null) {
      CashLoanOrderRecord firstPayoutOrder = cashLoanOrderModel.findFirstOrder(vo.loanAccountId, CashLoanOrderStatus.PAYOUT_STATUSES);
      if (firstPayoutOrder != null) {
        vo.firstPayoutSourceType = firstPayoutOrder.getSourceType();
      }
    }
    return vo;
  }

  private EcLoanUserRiskTraceVO convertToEcLoanUserRiskTraceVO(LoanUserRiskTraceRecord record) {
    if (record == null) {
      return null;
    }
    EcLoanUserRiskTraceVO vo = new EcLoanUserRiskTraceVO();
    vo.setUserId(record.getUserId());
    vo.setAccountId(record.getLoanAccountId());
    vo.setTraceId(record.getTraceId());
    vo.setRiskFlowId(record.getRiskFlowId());
    vo.setOrderId(record.getOrderId());
    vo.setRiskType(record.getRiskType() == null ? null : LoanUserRiskType.fromCode(record.getRiskType()));
    vo.setTimeCreated(record.getTimeCreated());
    vo.setTimeUpdated(record.getTimeUpdated());
    vo.setCreditsStatus(record.getCreditsStatus());
    vo.setTriggerType(record.getTriggerType());
    vo.setEventId(record.getEventId());
    vo.setSourceType(record.getSourceType() == null ? null : SourceType.valueOf(record.getSourceType()));
    return vo;
  }
}
