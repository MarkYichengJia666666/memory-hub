package com.miyou.controllers.cashloan;

import static com.miyou.controllers.converter.EcRiskUserAccountMappingResponseConverter.convertRiskUserAccountMapping;
import static com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED;
import static com.yqg.ec.common.spring.response.risk.EcIncreaseCreditsReviewResultInfoResponse.ResponseCode.FAILED;
import static com.yqg.ec.common.spring.response.risk.EcIncreaseCreditsReviewResultInfoResponse.ResponseCode.SUCCESS;

import com.miyou.controllers.converter.ECImmediateContactWithIdentityConverter;
import com.miyou.controllers.converter.EcAccountMaxOrderSeqResponseConverter;
import com.miyou.controllers.converter.EcBatchTriggerRiskTaskResponseConverter;
import com.miyou.controllers.converter.EcIncreaseCreditsReviewForRiskRejectConverter;
import com.miyou.controllers.converter.EcLivingInfoResponseConverter;
import com.miyou.controllers.converter.EcLoanExtraInfoDetailResponseConverter;
import com.miyou.controllers.converter.EcLoanExtraInfoResponseConverter;
import com.miyou.controllers.converter.EcLoanThirdPartyStatApiResponseConverter;
import com.miyou.controllers.converter.EcRiskAbtestResResponseConverter;
import com.miyou.controllers.converter.EcRiskEventTypeInfoResponseConverter;
import com.miyou.controllers.converter.EcRiskLevelScoreConfigResponseConverter;
import com.miyou.controllers.converter.EcRiskMultiLoanLogResponseConverter;
import com.miyou.controllers.converter.EcRiskOutputResultResponseConverter;
import com.miyou.controllers.converter.EcRiskTongDunInfoNewParamConverter;
import com.miyou.controllers.converter.EcRiskUserLevelScoreByPhoneResponseConverter;
import com.miyou.controllers.converter.EcRiskUserLevelScoreResponseConverter;
import com.miyou.controllers.converter.EcRiskUserOrderDetailResponseConverter;
import com.miyou.controllers.converter.EcRiskUserResponseConverter;
import com.miyou.controllers.converter.EcTongDunTaskInfoResponseConverter;
import com.miyou.controllers.converter.EcUserDeviceInfoResponseConverter;
import com.miyou.controllers.converter.EcUserIncreaseCreditsInfoAllStatusResponseConverter;
import com.miyou.controllers.converter.EcUserIncreaseCreditsInfoResponseConverter;
import com.miyou.controllers.converter.EcUserOCRResponseConverter;
import com.miyou.controllers.converter.LoanAccountAppInfoResponseConverter;
import com.miyou.controllers.converter.QueryUserGroupLogResponseConverter;
import com.yqg.core.aop.StatQueryRequest;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.ImageReviewRecord;
import com.yqg.core.model.generated.tables.records.IvrAuthRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserCreateOrderWillingnessRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.generated.tables.records.LoanUserSupplementCreateOrderRecord;
import com.yqg.core.model.generated.tables.records.RiskLevelScoreConfigRecord;
import com.yqg.core.model.generated.tables.records.RiskUserLevelRecord;
import com.yqg.core.model.generated.tables.records.UserImmediateContactRecord;
import com.yqg.core.model.mongo.MongoSupplementBeforeCreateOrderModel;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.model.sql.cashloan.order.CashLoanUserSupplementCreateOrderModel;
import com.yqg.core.model.sql.contact.ImmediateContactType;
import com.yqg.core.model.sql.financing.additionalinfo.FinancingUserAdditionalInfoModel;
import com.yqg.core.model.sql.financing.additionalinfo.enums.FinancingUserAdditionalInfoType;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserEventTypeRelationModel;
import com.yqg.core.model.sql.loan.account.condition.LoanUserEventTypeCondition;
import com.yqg.core.model.sql.loan.feature.AdvanceRawDataModel;
import com.yqg.core.model.sql.loan.feature.FaceppRawDataModel;
import com.yqg.core.model.sql.loan.feature.IziDataRawDataModel;
import com.yqg.core.model.sql.loan.image.ImageReviewModel;
import com.yqg.core.model.sql.loan.image.enums.ImageReviewStatus;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.manualauth.enums.IvrAuthType;
import com.yqg.core.model.sql.risk.RiskOutputResultCondition;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.model.sql.secure.SecureLivingInfoModel;
import com.yqg.core.service.abtest.vo.ABTestResultNewVO;
import com.yqg.core.service.advertisement.AdAppsflyerService;
import com.yqg.core.service.advertisement.attribution.UserDeltaAttributionRiskResultService;
import com.yqg.core.service.advertisement.vo.AppsflyerRawDataVO;
import com.yqg.core.service.advertisement.vo.appsflyer.AdAppsflyerRecordVO;
import com.yqg.core.service.authorizedcollection.td.enums.TongDunTaskNewStatus;
import com.yqg.core.service.bizcheck.BizCheckTool;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.auth.UserImmediateContactService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.risk.AppDataSourceQueryService;
import com.yqg.core.service.cashloan.risk.AppListService;
import com.yqg.core.service.cashloan.risk.RiskDetailService;
import com.yqg.core.service.cashloan.risk.vo.AppInfoPojoWithTimeCreateVO;
import com.yqg.core.service.cashloan.risk.vo.RiskMultiLoanLogVO;
import com.yqg.core.service.cashloan.risk.vo.RiskOutputResultSimpleVO;
import com.yqg.core.service.cashloan.risk.vo.RiskUserDeviceInfoVo;
import com.yqg.core.service.cashloan.risk.vo.RiskUserOrderDetailVO;
import com.yqg.core.service.cashloan.risk.vo.UserEventTypeRelationVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.cashloan.vo.CashLoanMaxOrderSeqVo;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.image.ImageReviewService;
import com.yqg.core.service.loan.image.LoanUserExtraInfoReviewService;
import com.yqg.core.service.loan.image.vo.IncreaseCreditsReviewInfoVO;
import com.yqg.core.service.loan.image.vo.LoanUserExtraInfoVO;
import com.yqg.core.service.loan.intention.repository.LoanIntentionRepository;
import com.yqg.core.service.loan.vo.LoanAccountDetailsSimpleVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.SimpleLoanAccountVO;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsPojo;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.core.service.risk.LoanUserMaxLoanInfoService;
import com.yqg.core.service.risk.RiskApplicationSubmitService;
import com.yqg.core.service.risk.batchtrigger.MarketingRiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerTaskType;
import com.yqg.core.service.risk.batchtrigger.vo.MarketingBatchTriggerRiskRelationVO;
import com.yqg.core.service.risk.batchtrigger.vo.TaskDataVO;
import com.yqg.core.service.risk.datasource.DataSourceHelper;
import com.yqg.core.service.risk.etl.vo.RiskBaseInfo;
import com.yqg.core.service.risk.export.RequesterConfig;
import com.yqg.core.service.risk.facade.RiskFacadeService;
import com.yqg.core.service.risk.facade.RiskSwimLaneTool;
import com.yqg.core.service.risk.feature.FraudAggregationReviewRiskService;
import com.yqg.core.service.risk.feature.FraudOrderReviewRiskService;
import com.yqg.core.service.risk.feature.HighRiskPasswordReviewRiskService;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.longshortuser.UserMinimalistJudgeService;
import com.yqg.core.service.risk.provider.advanceai.AdvanceAiV2Service;
import com.yqg.core.service.risk.provider.advanceai.response.OcrResponse;
import com.yqg.core.service.risk.provider.ivr.AntiFraudIvrReviewService;
import com.yqg.core.service.risk.provider.ivr.FraudOrderIvrReviewService;
import com.yqg.core.service.risk.provider.ivr.MobileAuthenticityService;
import com.yqg.core.service.risk.provider.ivr.MobileWizInitService;
import com.yqg.core.service.risk.provider.ivr.RiskIvrAuthService;
import com.yqg.core.service.risk.provider.izi.IziDataBizService;
import com.yqg.core.service.risk.provider.manualauth.ManualAuthService;
import com.yqg.core.service.risk.provider.manualreview.LoanIdCardImageManualReviewService;
import com.yqg.core.service.risk.provider.manualreview.LoanModifyBankAccountNameReviewService;
import com.yqg.core.service.risk.provider.manualreview.LoanWorkCardImageManualReviewService;
import com.yqg.core.service.risk.provider.tencentai.TencentAiService;
import com.yqg.core.service.risk.provider.tongdun.living.TongDunLivingRawDataService;
import com.yqg.core.service.risk.provider.tongdun.living.vo.TongDunLivingRawDataVO;
import com.yqg.core.service.risk.risklevel.RiskLevelScoreConfigService;
import com.yqg.core.service.risk.risklevel.RiskUserLevelService;
import com.yqg.core.service.risk.risklevel.vo.PhoneRiskLevelScoreVO;
import com.yqg.core.service.risk.risklevel.vo.RiskLevelScoreConfigVO;
import com.yqg.core.service.risk.riskoutput.RiskOutputLatestSnapshot;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.riskuser.RiskUserService;
import com.yqg.core.service.risk.riskuser.vo.RiskUserAccountMappingVO;
import com.yqg.core.service.risk.riskuser.vo.RiskUserVO;
import com.yqg.core.service.risk.usergroup.LoanRiskUserGroupService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupLogVO;
import com.yqg.core.service.similarface.SimilarFaceReviewService;
import com.yqg.core.service.tongdun.tongdunprocessor.TongdunProcessParam;
import com.yqg.core.service.user.LivingImageDataDetailsService;
import com.yqg.core.service.user.vo.LivingImageDataDetailsVO;
import com.yqg.core.util.thirdparty.loan.LoanCreditService;
import com.yqg.core.util.thirdparty.loan.OcrService;
import com.yqg.core.util.thirdparty.loan.query.LoanThirdPartyQueryContext;
import com.yqg.core.util.thirdparty.loan.vo.AdvanceRawDataVO;
import com.yqg.core.util.thirdparty.loan.vo.IziDataRawDataVO;
import com.yqg.core.util.thirdparty.loan.vo.TongDunTaskInfoNewVO;
import com.yqg.core.util.thirdparty.megvii.FacePPDataV3Service;
import com.yqg.core.util.thirdparty.megvii.vo.FacePPRawDataVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.extrainfo.ExtraInfoReviewStatus;
import com.yqg.ec.common.enums.risk.EcRiskModifyBankAccountNameReviewResult;
import com.yqg.ec.common.enums.risk.RiskCrowdCategory;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.enums.risk.ThirdPartyApi;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.request.EcAntiFraudIvrReviewRequest;
import com.yqg.ec.common.spring.request.EcBatchQueryUserLastedLoanInfoRequest;
import com.yqg.ec.common.spring.request.EcBatchQueryUserMaxLoanInfoRequest;
import com.yqg.ec.common.spring.request.EcBatchTriggerRiskTaskRequest;
import com.yqg.ec.common.spring.request.EcCallbackRiskReviewRequest;
import com.yqg.ec.common.spring.request.EcCallbackTraceFinishRequest;
import com.yqg.ec.common.spring.request.EcFraudAggregationReviewRequest;
import com.yqg.ec.common.spring.request.EcFraudOrderIvrReviewRequest;
import com.yqg.ec.common.spring.request.EcFraudOrderReviewRequest;
import com.yqg.ec.common.spring.request.EcHighRiskPasswordReviewRequest;
import com.yqg.ec.common.spring.request.EcIdCardImageReviewRequest;
import com.yqg.ec.common.spring.request.EcImageReviewUpdateAfterTimeOutRequest;
import com.yqg.ec.common.spring.request.EcLoanAccountIdRequest;
import com.yqg.ec.common.spring.request.EcManualAuthRequest;
import com.yqg.ec.common.spring.request.EcMexicoMobileAuthenticityRequest;
import com.yqg.ec.common.spring.request.EcMobileAuthenticityRequest;
import com.yqg.ec.common.spring.request.EcMobileWizInitRequest;
import com.yqg.ec.common.spring.request.EcModifyBankAccountNameReviewRequest;
import com.yqg.ec.common.spring.request.EcRiskIvrAuthInitRequest;
import com.yqg.ec.common.spring.request.EcSimilarFaceReviewRequest;
import com.yqg.ec.common.spring.request.EcUpdateTraceStatusToCancelledRequest;
import com.yqg.ec.common.spring.request.EcWorkCardImageReviewRequest;
import com.yqg.ec.common.spring.request.QueryUserGroupLogRequest;
import com.yqg.ec.common.spring.request.TongDunTaskNewUpdateRequest;
import com.yqg.ec.common.spring.request.risk.EcBatchGetMaxOrderReqInEveryAccountIdRequest;
import com.yqg.ec.common.spring.request.risk.EcIncreaseCreditsReviewResultInfoRequest;
import com.yqg.ec.common.spring.request.risk.EcLastedApplistRequest;
import com.yqg.ec.common.spring.request.risk.EcOldestApplistRequest;
import com.yqg.ec.common.spring.request.risk.EcQueryLoanUserExtraInfoRequest;
import com.yqg.ec.common.spring.request.risk.EcRiskOutputResultRequest;
import com.yqg.ec.common.spring.response.AppDataSourceCandidateResponse;
import com.yqg.ec.common.spring.response.AppDataSourceCandidateResponse.AppDataSourceCandidate;
import com.yqg.ec.common.spring.response.EcBatchQueryUserLastedLoanInfoResponse;
import com.yqg.ec.common.spring.response.EcBatchQueryUserMaxLoanInfoResponse;
import com.yqg.ec.common.spring.response.EcLoanAccountAppInfoResponse;
import com.yqg.ec.common.spring.response.EcRiskCallBackTraceCreatedResponse;
import com.yqg.ec.common.spring.response.EcRiskLevelScoreConfigV2Response;
import com.yqg.ec.common.spring.response.EcRiskTongdunProcessParamResponse;
import com.yqg.ec.common.spring.response.EcRiskUserResponse;
import com.yqg.ec.common.spring.response.QueryUserGroupLogResponse;
import com.yqg.ec.common.spring.response.enums.EcSignatureType;
import com.yqg.ec.common.spring.response.risk.ECIdentityNumberWithImmediateContactResponse;
import com.yqg.ec.common.spring.response.risk.ECTongDunTaskInfoNewResponse;
import com.yqg.ec.common.spring.response.risk.ECUserOrderWillingInfoResponse;
import com.yqg.ec.common.spring.response.risk.ECUserSupplementInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcAccountMaxOrderSeqResponse;
import com.yqg.ec.common.spring.response.risk.EcAdAppsflyerRawDataResponse;
import com.yqg.ec.common.spring.response.risk.EcBatchTriggerRiskTaskResponse;
import com.yqg.ec.common.spring.response.risk.EcH5HalfProcessUserInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcIncreaseCreditsReviewForRiskRejectResponse;
import com.yqg.ec.common.spring.response.risk.EcIncreaseCreditsReviewResultInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcLivingInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcLoanAccountIdResponse;
import com.yqg.ec.common.spring.response.risk.EcLoanExtraInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcMinimalistProcessUserInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcReloanCreditInValidInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskAbtestResResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskCrowdCategorySupportedRiskTypeResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskEventTypeInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskIvrAuthResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskLevelConfigInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskLevelScoreConfigResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskModifyBankAccountNameReviewResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskMultiLoanLogResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskOutputLatestResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskOutputResultResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserAccountMappingResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserAppListInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserExtraInfoDetailResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserLevelScoreByPhoneResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserLevelScoreResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserOrderDetailResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserOrderDetailSimpleResponse;
import com.yqg.ec.common.spring.response.risk.EcRiskUserOrderDetailV2Response;
import com.yqg.ec.common.spring.response.risk.EcSignatureResponse;
import com.yqg.ec.common.spring.response.risk.EcThirdPartyDataResponse;
import com.yqg.ec.common.spring.response.risk.EcThirdPartyStatApiResponse;
import com.yqg.ec.common.spring.response.risk.EcUserDeviceInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcUserIncreaseCreditsInfoAllStatusResponse;
import com.yqg.ec.common.spring.response.risk.EcUserIncreaseCreditsInfoResponse;
import com.yqg.ec.common.spring.response.risk.EcUserOCRResponse;
import com.yqg.ec.common.spring.response.risk.EcUserRealTimeMediaSourceResponse;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.overseas.ads.client.common.vo.AdsUserDeltaAttributionRiskResultVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/ecInternalApi/risk")
public class RiskController {
  @Autowired
  private AppListService appListService;
  @Autowired
  private LoanCreditService loanCreditService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private ManualAuthService manualAuthService;
  @Autowired
  private LoanIdCardImageManualReviewService loanIdCardImageManualReviewService;
  @Autowired
  private LoanWorkCardImageManualReviewService loanWorkCardImageManualReviewService;
  @Autowired
  private LoanModifyBankAccountNameReviewService loanModifyBankAccountNameReviewService;
  @Autowired
  private LoanUserAdditionalInfoService loanUserAdditionalInfoService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private MobileAuthenticityService mobileAuthenticityService;
  @Autowired
  private FraudAggregationReviewRiskService fraudAggregationReviewRiskService;
  @Autowired
  private FraudOrderReviewRiskService fraudOrderReviewRiskService;
  @Autowired
  private HighRiskPasswordReviewRiskService highRiskPasswordReviewRiskService;
  @Autowired
  private FraudOrderIvrReviewService fraudOrderIvrReviewService;
  @Autowired
  private AntiFraudIvrReviewService antiFraudIvrReviewService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private ImageReviewModel imageReviewModel;
  @Autowired
  private RiskReuploadService riskReuploadService;
  @Autowired
  private RiskDetailService riskDetailService;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private MobileWizInitService mobileWizInitService;
  @Autowired
  private FacePPDataV3Service facePPDataV3Service;
  @Autowired
  private IziDataBizService iziDataBizService;
  @Autowired
  private TencentAiService tencentAiService;
  @Autowired
  private LoanUserExtraInfoReviewService loanUserExtraInfoReviewService;
  @Autowired
  private RiskLevelScoreConfigService riskLevelScoreConfigService;
  @Autowired
  private LoanUserEventTypeRelationModel loanUserEventTypeRelationModel;
  @Autowired
  private LivingImageDataDetailsService livingImageDataDetailsService;
  @Autowired
  private UserDeltaAttributionRiskResultService userDeltaAttributionRiskResultService;
  @Autowired
  private SimilarFaceReviewService similarFaceReviewService;
  @Autowired
  private BizCheckTool bizCheckTool;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private UserMinimalistJudgeService userMinimalistJudgeService;
  @Autowired
  private CashLoanCalcCreditsService cashLoanCalcCreditsService;
  @Autowired
  private EcOrderService loanOrderService;
  @Autowired
  private IziDataRawDataModel iziDataRawDataModel;
  @Autowired
  private SecureLivingInfoModel secureLivingInfoModel;
  @Autowired
  private AdvanceRawDataModel advanceRawDataModel;
  @Autowired
  private FaceppRawDataModel faceppRawDataModel;
  @Autowired
  private TongDunLivingRawDataService tongDunLivingRawDataService;
  @Autowired
  private FinancingUserAdditionalInfoModel financingUserAdditionalInfoModel;
  @Autowired
  private AdvanceAiV2Service advanceAiV2Service;
  @Autowired
  private AdAppsflyerService adAppsflyerService;
  @Autowired
  private OcrService ocrService;
  @Autowired
  private UserImmediateContactService userImmediateContactService;
  @Autowired
  private RiskUserLevelService riskUserLevelService;
  @Resource
  private LoanIntentionRepository loanIntentionRepository;
  @Autowired
  private CashLoanUserSupplementCreateOrderModel supplementCreateOrderModel;
  @Autowired
  private MongoSupplementBeforeCreateOrderModel mongoSupplementBeforeCreateOrderModel;
  @Autowired
  private RiskIvrAuthService riskIvrAuthService;
  @Autowired
  private RiskUserService riskUserService;
  @Autowired
  private RiskFacadeService riskFacadeService;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private RiskApplicationSubmitService riskApplicationSubmitService;
  @Autowired
  private LoanAccountModel accountModel;
  public static final Integer DAYS_AGO = -180;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private RiskBatchTriggerService batchTriggerService;
  @Autowired
  private MarketingRiskBatchTriggerService marketingRiskBatchTriggerService;
  @Autowired
  private ImageReviewService imageReviewService;
  @Autowired
  private RiskSwimLaneTool riskSwimLaneTool;
  @Autowired
  private LoanUserMaxLoanInfoService userMaxLoanInfoService;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;

  @Autowired
  private AppDataSourceQueryService appDataSourceQueryService;

  @PostMapping(path = "/callbackRiskReview")
  public void callbackRiskReview(@RequestBody @Valid EcCallbackRiskReviewRequest request,
                                 @RequestHeader(value = "fintopia-swim-lane-id", required = false) String swimLaneId) {

    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(request.traceId);
    if (!riskSwimLaneTool.canHandleByCurrentSwimLane(swimLaneId, loanUserRiskTraceVO)) {
      log.info("Risk control callback has been skipped because no hit tarGetSwimlane, with accountID = {}, traceId = {}", loanUserRiskTraceVO.accountId, loanUserRiskTraceVO.traceId);
      return;
    }
    riskFacadeService.executeAutoReviewResultForCallBack(request.traceId);
  }

  @PostMapping(path = "/manualAuth")
  public void manualAuth(@RequestBody @Valid EcManualAuthRequest request) {
    manualAuthService.insertManualAuth(request.accountId, request.verificationReasons, request.traceId);
  }

  @PostMapping(path = "/idCardImageManualReview")
  public void idCardImageManualReview(@RequestBody @Valid EcIdCardImageReviewRequest request) {
    loanIdCardImageManualReviewService.collectData(request.accountId, request.verificationReasons, request.traceId);
  }

  @PostMapping(path = "/workCardImageManualReview")
  public void workCardImageManualReview(@RequestBody @Valid EcWorkCardImageReviewRequest request) {
    loanWorkCardImageManualReviewService.collectData(request.accountId);
  }

  @PostMapping(path = "/modifyBankAccountNameManualReview")
  public EcRiskModifyBankAccountNameReviewResponse modifyBankAccountNameManualReview(@RequestBody @Valid EcModifyBankAccountNameReviewRequest request) {
    EcRiskModifyBankAccountNameReviewResult reviewResult = loanModifyBankAccountNameReviewService.initAndGetResult(request.loanOrderId);
    return EcRiskModifyBankAccountNameReviewResponse.from(reviewResult);
  }

  @PostMapping(path = "/queryBankAccountNameManualReview")
  public EcRiskModifyBankAccountNameReviewResponse queryBankAccountNameManualReview(@RequestBody @Valid EcModifyBankAccountNameReviewRequest request) {
    EcRiskModifyBankAccountNameReviewResult reviewResult = loanModifyBankAccountNameReviewService.getResult(request.loanOrderId);
    return EcRiskModifyBankAccountNameReviewResponse.from(reviewResult);
  }

  @PostMapping(path = "/mobileAuthenticity")
  public void mobileAuthenticity(@RequestBody @Valid EcMobileAuthenticityRequest request) {
    UserInfoVO userInfoVO = loanUserAdditionalInfoService.genLoanUserInfoVO(request.accountId);
    LoanAccountDetailsVO detailsVO = loanAccountDetailsService.getByAccountId(request.accountId);
    LoanAccountDetailsPojo pojo = detailsVO == null ? null : detailsVO.detailsPojo;
    mobileAuthenticityService.init(userInfoVO, pojo);
  }

  @PostMapping(path = "/fraudAggregationReview")
  public void fraudAggregationReview(@RequestBody @Valid EcFraudAggregationReviewRequest request) {
    RiskBaseInfo riskBaseInfo = DataSourceHelper.buildRiskBaseInfo(request.props);
    fraudAggregationReviewRiskService.initCollection(riskBaseInfo);
  }

  @PostMapping(path = "/fraudOrderReview")
  public void fraudOrderReview(@RequestBody @Valid EcFraudOrderReviewRequest request) {
    RiskBaseInfo riskBaseInfo = DataSourceHelper.buildRiskBaseInfo(request.props);
    fraudOrderReviewRiskService.initCollection(riskBaseInfo);
  }

  @PostMapping(path = "/highRiskPasswordReview")
  public void highRiskPasswordReview(@RequestBody @Valid EcHighRiskPasswordReviewRequest request) {
    RiskBaseInfo riskBaseInfo = DataSourceHelper.buildRiskBaseInfo(request.props);
    highRiskPasswordReviewRiskService.initCollection(riskBaseInfo);
  }

  @PostMapping(path = "/fraudOrderIvrReview")
  public void fraudOrderIvrReview(@RequestBody @Valid EcFraudOrderIvrReviewRequest request) {
    UserInfoVO userInfoVO = loanUserAdditionalInfoService.genLoanUserInfoVO(request.accountId);
    fraudOrderIvrReviewService.initFraudOrderIvr(userInfoVO, request.traceId, request.extraData);
  }

  @PostMapping(path = "/antiFraudIvrReview")
  public void antiFraudIvrReview(@RequestBody @Valid EcAntiFraudIvrReviewRequest request) {
    UserInfoVO userInfoVO = loanUserAdditionalInfoService.genLoanUserInfoVO(request.accountId);
    if (userInfoVO == null) {
      throw EcException.error(EcExceptionType.LOAN_ACCOUNT_NOT_FOUND, "accountId = " + request.accountId);
    }
    antiFraudIvrReviewService.initAntiFraudIvr(userInfoVO, request.traceId, request.extraData, request.accountId);
  }

  @PostMapping(path = "/mexicoMobileAuthenticity")
  public void mexicoMobileAuthenticity(@RequestBody @Valid EcMexicoMobileAuthenticityRequest request) {
    UserInfoVO userInfoVO = loanUserAdditionalInfoService.genLoanUserInfoVO(request.accountId);
    LoanAccountDetailsVO detailsVO = loanAccountDetailsService.getByAccountId(request.accountId);
    LoanAccountDetailsPojo pojo = detailsVO == null ? null : detailsVO.detailsPojo;
    mobileAuthenticityService.init(userInfoVO, pojo);
  }

  @PostMapping(path = "/imageReviewUpdateAfterTimeOut")
  public void imageReviewUpdateAfterTimeOut(@RequestBody @Valid EcImageReviewUpdateAfterTimeOutRequest request) {
    threadTransactionalModel.transaction(configuration -> {
      ImageReviewRecord record = imageReviewModel.findById(request.imageReviewRecordId);
      imageReviewService.updateRecordStatus(record, ImageReviewStatus.TIME_OUT);
      riskReuploadService.setReUploadTimeout(record.getLoanAccountId());
    });
  }

  @PostMapping(path = "/similarFaceReview")
  public void similarFaceReview(@RequestBody @Valid EcSimilarFaceReviewRequest request) {
    similarFaceReviewService.init(request.loanAccountId, request.firstFaceLoanAccountId, request.secondFaceLoanAccountId, request.creditSubmitTime);
  }

  @StatQueryRequest
  @GetMapping(path = "/riskUserOrderDetails")
  public EcRiskUserOrderDetailResponse getRiskUserOrderDetails(@RequestParam("loanAccountId") Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = riskDetailService.getRiskUserOrderDetailVO(loanAccountId);
    return EcRiskUserOrderDetailResponseConverter.convert(riskUserOrderDetailVO);
  }

  @StatQueryRequest
  @GetMapping(path = "/riskUserOrderDetailsV2")
  public EcRiskUserOrderDetailResponse getRiskUserOrderDetailsV2(@RequestParam("loanAccountId") Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = riskDetailService.getRiskUserOrderDetailVOV2(loanAccountId);
    return EcRiskUserOrderDetailResponseConverter.convertV2(riskUserOrderDetailVO);
  }

  /**
   * 仅返回订单信息，不查询分期和 additional info，性能优于 riskUserOrderDetailsV2
   *
   * @param loanAccountId 账户 ID
   * @return 仅含 cashLoanOrderInfo 的响应，cashLoanInstalmentInfoList 和 riskOrderAdditionalInfo 为 null
   */
  @StatQueryRequest
  @GetMapping(path = "/riskUserOrderInfoOnly")
  public EcRiskUserOrderDetailResponse getRiskUserOrderInfoOnly(@RequestParam("loanAccountId") Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = riskDetailService.getRiskUserOrderInfoOnlyVO(loanAccountId);
    return EcRiskUserOrderDetailResponseConverter.convertOrderInfoOnly(riskUserOrderDetailVO);
  }

  @StatQueryRequest
  @GetMapping(path = "/riskUserOrderDetailsSimple")
  public EcRiskUserOrderDetailSimpleResponse riskUserOrderDetailsSimple(@RequestParam("loanAccountId") Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = riskDetailService.getRiskUserOrderDetailVOSimple(loanAccountId);
    return EcRiskUserOrderDetailResponseConverter.convertSimple(riskUserOrderDetailVO);
  }

  @StatQueryRequest
  @GetMapping(path = "/riskMultiLoanLogList")
  public EcRiskMultiLoanLogResponse getEcRiskMultiLoanLogResponse(@RequestParam("loanAccountId") Long loanAccountId, @RequestParam("startTime") Long startTime, @RequestParam("endTime") Long endTime) {
    List<RiskMultiLoanLogVO> riskMultiLoanLogVOList = riskDetailService.getRiskMultiLoanLogVOList(loanAccountId, startTime, endTime);
    return EcRiskMultiLoanLogResponseConverter.convert(riskMultiLoanLogVOList);
  }

  @PostMapping(path = "/mobileWizInit")
  public void mobileWizInit(@RequestBody @Valid EcMobileWizInitRequest request) {
    mobileWizInitService.init(
        request.userId,
        SDKType.fromCode(request.sdkTypeCode),
        IvrAuthType.from(request.ivrAuthTypeCode)
    );
  }

  @StatQueryRequest
  @GetMapping(path = "/getThirdPartyData")
  public EcThirdPartyDataResponse getThirdPartyData(
      @RequestParam("loanAccountId") Long loanAccountId,
      @RequestParam(value = "thirdPartyApi") ThirdPartyApi thirdPartyApi) {

    return getEcThirdPartyDataResponse(loanAccountId, thirdPartyApi, Clock.now());
  }

  @StatQueryRequest
  @GetMapping(path = "/getThirdPartyDataWithTimestamp")
  public EcThirdPartyDataResponse getThirdPartyDataWithTimestamp(
      @RequestParam("loanAccountId") Long loanAccountId,
      @RequestParam(value = "thirdPartyApi") ThirdPartyApi thirdPartyApi,
      @RequestParam(value = "timestamp") Long timestamp
  ) {
    return getEcThirdPartyDataResponse(loanAccountId, thirdPartyApi, timestamp);
  }

  private EcThirdPartyDataResponse getEcThirdPartyDataResponse(Long loanAccountId, ThirdPartyApi thirdPartyApi, Long timestamp) {
    switch (thirdPartyApi) {
      case FACE_PP_VERIFY_V3_MEGLIVE:
      case FACE_PP_VERIFY_V3_FLASH:
      case FACE_PP_LIVING_DETECTION_V3:
      case FACE_PP_LIVING_DETECTION_V5_FLASH:
      case FACE_PP_LIVING_DETECTION_V5_MEGLIVE:
        FacePPRawDataVO facePPVerifyData = facePPDataV3Service.getFacePPVerifyData(loanAccountId, thirdPartyApi);
        if (facePPVerifyData == null) {
          return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null);
        }
        return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.SUCCESS, facePPVerifyData.timeCreated, facePPVerifyData.responseData);

      case IZI_DATA_OCR:
        IziDataRawDataVO iziDataRawDataVO = iziDataBizService.getIziDataLatestRecordByAccountIdAndApi(loanAccountId, thirdPartyApi, timestamp);
        if (Objects.isNull(iziDataRawDataVO)) {
          return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null);
        }
        return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.SUCCESS, iziDataRawDataVO.timeCreated, iziDataRawDataVO.responseData);

      case TENCENT_KTP_OCR:
        return Optional.of(loanAccountId)
            .map(accountModel::findById)
            .map(LoanAccountRecord::getUserId)
            .map(userId -> LoanThirdPartyQueryContext.builder()
                .userId(userId)
                .loanAccountId(loanAccountId)
                .thirdPartyApi(ThirdPartyApi.TENCENT_KTP_OCR)
                .timestamp(timestamp)
                .build())
            .map(tencentAiService::getLoanLatestRawDataOrNull)
            .map(rawData -> EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.SUCCESS, rawData.timeCreated, rawData.responseData))
            .orElseGet(() -> EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null));

      case ADVANCE_LIVENESS_H5_DETECTION:
        AdvanceRawDataVO advanceRawDataVO = advanceAiV2Service.getAdvanceRawDataVO(loanAccountId, thirdPartyApi);
        if (Objects.isNull(advanceRawDataVO)) {
          return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null);
        }
        return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.SUCCESS, advanceRawDataVO.timeCreated, advanceRawDataVO.responseData);

      case TONGDUN_LIVING_DETECTION:
        TongDunLivingRawDataVO tongDunLivingRawDataVO = tongDunLivingRawDataService.getRawDataVOByTraceTime(
            loanAccountId, thirdPartyApi, timestamp);
        if (tongDunLivingRawDataVO == null) {
          return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null);
        }
        return EcThirdPartyDataResponse.from(
            EcThirdPartyDataResponse.ResponseCode.SUCCESS,
            tongDunLivingRawDataVO.timeCreated,
            tongDunLivingRawDataVO.responseData);

      default:
        return EcThirdPartyDataResponse.from(EcThirdPartyDataResponse.ResponseCode.NO_RESULT, null, null);
    }
  }

  @GetMapping(path = "/getRiskOutputResultDetailVOList")
  public EcRiskUserOrderDetailV2Response getRiskOutputResultDetailVOList(@RequestParam("loanAccountId") Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOutputResultDetailVOList = riskDetailService.getRiskOutputResultDetailVOList(loanAccountId);
    return EcRiskUserOrderDetailResponseConverter.convertOnlyOutputResultDetail(riskUserOrderDetailVO);
  }

  @GetMapping(path = "/getRiskOutputResultDetailVOListByTraceIds")
  public EcRiskUserOrderDetailV2Response getRiskOutputResultDetailVOListByTraceIds(@RequestParam("traceIds") List<Long> traceIds) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOutputResultDetailVOList = riskDetailService.getRiskOutputResultDetailVOListByTraceIds(traceIds);
    return EcRiskUserOrderDetailResponseConverter.convertOnlyOutputResultDetail(riskUserOrderDetailVO);
  }

  @StatQueryRequest
  @GetMapping(path = "/extraInfo")
  public EcLoanExtraInfoResponse getExtraInfo(@RequestParam("loanAccountId") Long loanAccountId, @RequestParam("startTime") Long startTime, @RequestParam("endTime") Long endTime) {
    List<LoanUserExtraInfoVO> loanUserExtraInfoVOList = loanUserExtraInfoReviewService.getLoanUserExtraInfoVO(loanAccountId, startTime, endTime);
    return EcLoanExtraInfoResponseConverter.convert(loanUserExtraInfoVOList);
  }

  /**
   * 获取所有的风控等级配置数据
   *
   * @return
   */
  @GetMapping(path = "/getAllRiskLevelScoreConfig")
  public EcRiskLevelScoreConfigResponse getAllRiskLevelScoreConfig() {
    List<RiskLevelScoreConfigRecord> list = riskLevelScoreConfigService.getAllAvailableConfigs();
    list = list.stream()
        .filter(record -> !RequesterConfig.fromId(Integer.parseInt(record.getSceneType())).isDeprecated())
        .collect(Collectors.toList());
    return EcRiskLevelScoreConfigResponseConverter.convert(list);
  }

  /**
   * 查询用户风控等级
   *
   * @param userId
   * @param sceneType
   * @param sdkType
   * @return
   */
  @GetMapping(path = "/getRiskUserLevelScoreInfo")
  public EcRiskUserLevelScoreResponse getRiskUserLevelScoreInfo(@RequestParam("userId") Long userId,
                                                                @RequestParam("sceneType") String sceneType,
                                                                @RequestParam("sdkType") String sdkType) {
    RiskUserLevelRecord riskUserLevelInfo = riskUserLevelService.getRiskUserLevelInfo(userId, sceneType, sdkType);
    return EcRiskUserLevelScoreResponseConverter.convert(userId, sceneType, Collections.singletonList(riskUserLevelInfo));
  }

  @GetMapping("/getRiskUserLevelScoreByPhone")
  public EcRiskUserLevelScoreByPhoneResponse getRiskUserLevelScoreByPhone(@RequestParam("normalizedMobileNumbers") List<String> normalizedMobileNumbers,
                                                                          @RequestParam("startTimeInMillSecond") Long startTimeInMillSecond,
                                                                          @RequestParam("endTimeInMillSecond") Long endTimeInMillSecond) {
    try {
      assertBatchSize(normalizedMobileNumbers);
      List<PhoneRiskLevelScoreVO> riskLevelScoreVOList = riskUserLevelService.getRiskUserLevelScoreByPhone(normalizedMobileNumbers, RiskOutputType.WHATSAPP_READ_MODEL_V1, startTimeInMillSecond, endTimeInMillSecond);
      EcRiskUserLevelScoreByPhoneResponse response = EcRiskUserLevelScoreByPhoneResponseConverter.convertSuccess(riskLevelScoreVOList);
      log.info("current risk user level store success, normalizedMobileNumbers={}, startTimeInMillSecond={}, endTimeInMillSecond={}, responsee={}", normalizedMobileNumbers, startTimeInMillSecond, endTimeInMillSecond, response);
      return response;
    } catch (Exception e) {
      log.error("current risk user level store by normalizedMobileNumber, normalizedMobileNumbers={}, startTimeInMillSecond={}, endTimeInMillSecond={}", normalizedMobileNumbers, startTimeInMillSecond, endTimeInMillSecond, e);
      return EcRiskUserLevelScoreByPhoneResponse.from(EcRiskUserLevelScoreByPhoneResponse.ResponseCode.FAILED, null);
    }
  }

  private void assertBatchSize(List<String> normalizedMobileNumbers) {
    Integer sizeLimit = riskConfig.getInfoBatchSizeLimit();
    EcAsserts.assertTrue(normalizedMobileNumbers != null && normalizedMobileNumbers.size() <= sizeLimit,
        "Request exceeds limit {}", sizeLimit);
  }

  @GetMapping(path = "/getRiskLevelConfig")
  public List<EcRiskLevelConfigInfoResponse> getRiskLevelConfigBySdk(@RequestParam("sdkType") String sdk) {
    SDKType sdkType = getSdkTypeFromString(sdk);
    List<RiskLevelScoreConfigVO> configs = riskLevelScoreConfigService.getAvailableConfigsBySdk(sdkType);
    if (CollectionUtils.isEmpty(configs)) {
      return new ArrayList<>();
    }
    return configs.stream()
        .map(o -> {
          List<String> levelList = new ArrayList<>();
          if (CollectionUtils.isNotEmpty(o.configs)) {
            levelList = o.configs.stream().map(config -> config.riskLevel).collect(Collectors.toList());
          }
          return EcRiskLevelConfigInfoResponse.from(o.sceneType.name(), o.sceneType.description, levelList);
        })
        .collect(Collectors.toList());
  }

  private SDKType getSdkTypeFromString(String sdkType) {
    return StringUtils.isBlank(sdkType) ? null : SDKType.valueOf(sdkType);
  }

  @GetMapping(path = "/eventTypeInfo")
  public EcRiskEventTypeInfoResponse getRiskEventTypeInfo(@RequestParam("userTypeName") String userTypeName) {
    List<UserEventTypeRelationVO> eventTypeRelationVOList = loanUserEventTypeRelationModel.findEventTypeByCondition(LoanUserEventTypeCondition.from(userTypeName));
    return EcRiskEventTypeInfoResponseConverter.convert(eventTypeRelationVOList);
  }

  @GetMapping(path = "/getLivingInfo")
  public EcLivingInfoResponse getLivingInfo(@RequestParam("loanAccountId") Long loanAccountId) {
    List<LivingImageDataDetailsVO> livingImageDataDetailsVOList = livingImageDataDetailsService.getLivingInfo(loanAccountId);
    return EcLivingInfoResponseConverter.convert(livingImageDataDetailsVOList);
  }

  /**
   * 补件风控特征，给风控提供一个接口查询用户最近的FacePP活体照片信息
   */
  @GetMapping(path = "/getLastLivingInfo")
  public EcLivingInfoResponse getLastLivingInfo(@RequestParam("loanAccountId") Long loanAccountId, @RequestParam("traceTime") Long traceTime) {
    List<LivingImageDataDetailsVO> livingImageDataDetailsVOList = livingImageDataDetailsService.getLastLivingInfoByLoanAccountIdAndTime(loanAccountId, traceTime);
    return EcLivingInfoResponseConverter.convert(livingImageDataDetailsVOList);
  }

  @GetMapping(path = "/getIncreaseCreditsInfo")
  public EcIncreaseCreditsReviewResultInfoResponse getIncreaseCreditsInfo(
      @RequestParam("loanAccountId") Long loanAccountId,
      @RequestParam("traceId") Long traceId,
      @RequestParam("submitRiskTime") Long submitRiskTime) {
    try {
      log.info("increase credits get feature,loanAccountId is {},traceId is {},submitRiskTime is {}", loanAccountId, traceId, submitRiskTime);
      IncreaseCreditsReviewInfoVO increaseCreditsReviewInfoVO = loanUserExtraInfoReviewService.getIncreaseCreditsReviewInfoV1(loanAccountId, traceId, submitRiskTime);
      return EcIncreaseCreditsReviewResultInfoResponse.from(
          increaseCreditsReviewInfoVO.imageType,
          increaseCreditsReviewInfoVO.reviewResult,
          increaseCreditsReviewInfoVO.imageTypeToTempCreditsMap,
          increaseCreditsReviewInfoVO.reviewPassTime,
          increaseCreditsReviewInfoVO.increaseCreditsCompletedTime,
          increaseCreditsReviewInfoVO.ocrResult,
          EcIncreaseCreditsReviewResultInfoResponse.ResponseCode.SUCCESS);
    } catch (Exception e) {
      log.error("current user exists problem,traceId is {},loanAccountId is {}", traceId, loanAccountId, e);
      return EcIncreaseCreditsReviewResultInfoResponse.from(FAILED);
    }
  }

  @PostMapping(path = "/getIncreaseCreditsInfoV2")
  public List<EcIncreaseCreditsReviewResultInfoResponse> getIncreaseCreditsInfoV2(@RequestBody EcIncreaseCreditsReviewResultInfoRequest request) {
    List<EcIncreaseCreditsReviewResultInfoRequest.RequestItem> requestList = request.requestList;
    if (requestList.size() > 500) {
      log.info("Params of getIncreaseCreditsInfoV2 exceed maximum limit: {}", requestList.size());
      return Collections.emptyList();
    }
    return requestList.stream().map(this::doGetIncreaseCreditsInfoV2).collect(Collectors.toList());
  }

  /**
   * 根据用户的 loanAccountId 和提交风控的 traceId ，查询增信提额资料的审核结果。
   *
   * @return 如果入参为空、或者查询过程出现任何异常，则状态码返回 FAILED ；
   * 否则返回符合条件的增信资料审核结果，状态码为 SUCCESS
   * @see EcIncreaseCreditsReviewResultInfoResponse.ResponseCode
   */
  private EcIncreaseCreditsReviewResultInfoResponse doGetIncreaseCreditsInfoV2(EcIncreaseCreditsReviewResultInfoRequest.RequestItem reqItem) {
    if (reqItem.loanAccountId == null || reqItem.traceId == null || reqItem.submitRiskTime == null) {
      return EcIncreaseCreditsReviewResultInfoResponse.from(FAILED);
    }

    try {
      log.info("increase credits get feature, loanAccountId is {}, traceId is {}, submitRiskTime is {}",
          reqItem.loanAccountId, reqItem.traceId, reqItem.submitRiskTime);

      IncreaseCreditsReviewInfoVO reviewInfoVO = loanUserExtraInfoReviewService.getIncreaseCreditsReviewInfoV2(
          reqItem.loanAccountId, reqItem.traceId, reqItem.submitRiskTime);

      return EcIncreaseCreditsReviewResultInfoResponse.from(
          reviewInfoVO.imageType,
          reviewInfoVO.reviewResult,
          reviewInfoVO.imageTypeToTempCreditsMap,
          reviewInfoVO.reviewPassTime,
          reviewInfoVO.increaseCreditsCompletedTime,
          reviewInfoVO.ocrResult,
          SUCCESS);
    } catch (Exception e) {
      log.error("current user exists problem,traceId is {},loanAccountId is {}", reqItem.traceId, reqItem.loanAccountId, e);
      return EcIncreaseCreditsReviewResultInfoResponse.from(FAILED);
    }
  }

  @GetMapping(path = "/getUserRealTimeMediaSource")
  public EcUserRealTimeMediaSourceResponse getUserRealTimeMediaSource(@RequestParam("userId") Long userId, @RequestParam("riskTime") Long riskTime) {
    LoanAccountDetailsSimpleVO loanAccountDetailsSimpleVO = loanAccountDetailsService.getSimpleByUserIdOrNull(userId);
    if (loanAccountDetailsSimpleVO == null || loanAccountDetailsSimpleVO.timeFinished == null) {
      return new EcUserRealTimeMediaSourceResponse();
    }
    //TODO(boshu, T00000)极简流程实时归因这里需要处理下
    AdsUserDeltaAttributionRiskResultVO attributionVO = userDeltaAttributionRiskResultService.getAuthCompletedAttributionByUserId(userId);
    if (attributionVO == null) {
      return new EcUserRealTimeMediaSourceResponse();
    }
    return EcUserRealTimeMediaSourceResponse.from(attributionVO.getRealTimeMediaSource(), attributionVO.getCampaign(), attributionVO.getChannel());
  }

  @GetMapping(path = "/getWithdrawAttribution")
  public EcUserRealTimeMediaSourceResponse getWithdrawAttribution(@RequestParam("userId") Long userId, @RequestParam("traceId") Long traceId) {
    AdsUserDeltaAttributionRiskResultVO attributionVO = userDeltaAttributionRiskResultService.getWithdrawAttribution(userId, traceId);
    if (attributionVO == null) {
      return new EcUserRealTimeMediaSourceResponse();
    }
    return EcUserRealTimeMediaSourceResponse.from(attributionVO.getRealTimeMediaSource(), attributionVO.getCampaign());
  }

  @GetMapping(path = "/getCreditsRiskAttribution")
  public EcUserRealTimeMediaSourceResponse getCreditsRiskAttribution(@RequestParam("userId") Long userId, @RequestParam("traceId") Long traceId) {
    AdsUserDeltaAttributionRiskResultVO attributionVO = userDeltaAttributionRiskResultService.getCreditsRiskAttribution(userId, traceId);
    if (attributionVO == null) {
      return new EcUserRealTimeMediaSourceResponse();
    }
    return EcUserRealTimeMediaSourceResponse.from(attributionVO.getRealTimeMediaSource(), attributionVO.getCampaign());
  }

  @GetMapping(path = "/getIncreaseCreditsReviewForRiskReject")
  public EcIncreaseCreditsReviewForRiskRejectResponse getIncreaseCreditsReviewForRiskReject(@RequestParam("loanAccountId") Long loanAccountId, @RequestParam("submitRiskTime") Long submitRiskTime) {
    try {
      log.info("getIncreaseCreditsReviewForRiskReject get feature,loanAccountId is {},submitRiskTime is {}", loanAccountId, submitRiskTime);
      List<IncreaseCreditsReviewInfoVO> allAcceptIncreaseCreditsReviewInfoVOList = loanUserExtraInfoReviewService.getAllAcceptIncreaseCreditsReviewInfoVO(loanAccountId, submitRiskTime);
      return EcIncreaseCreditsReviewForRiskRejectConverter.convert(allAcceptIncreaseCreditsReviewInfoVOList);
    } catch (Exception e) {
      log.error("current user exists problem,loanAccountId is {}", loanAccountId, e);
      return EcIncreaseCreditsReviewForRiskRejectResponse.from(new ArrayList<>());
    }
  }


  @GetMapping(path = "/getLastAbtestRecord")
  public EcRiskAbtestResResponse getLastAbtestRecord(@RequestParam("sceneType") String sceneType, @RequestParam("loanAccountId") Long loanAccountId, @RequestParam("submitRiskTime") Long submitRiskTime) {
    try {
      SimpleLoanAccountVO loanAccountVO = loanAccountService.getSimpleLoanAccountVo(loanAccountId);
      ABTestResultNewVO abTestResult = ABTestResultNewVO.from(loanAccountVO.userId,ABTestSceneType.valueOf(sceneType), ABTestSceneType.valueOf(sceneType).defaultScene,
          DiversionKeyType.USER_ID);
      return EcRiskAbtestResResponseConverter.convert(abTestResult);
    } catch (Exception e) {
      log.error("current user exists problem", e);
      return EcRiskAbtestResResponse.from();
    }
  }

  /**
   * 获取用户的签名类型
   *
   * @param loanOrderId
   * @return
   */
  @GetMapping(path = "/getSignatureType")
  public EcSignatureResponse getSignatureType(@RequestParam("loanOrderId") Long loanOrderId) {
    EcSignatureType ecSignatureType = bizCheckTool.finishClickHandWrittenSignatureWhenCrateOrder(loanOrderId) ?
        EcSignatureType.SIGNATURE_WHEN_CREATE_ORDER :
        EcSignatureType.SIGNATURE_AFTER_RISK;
    return EcSignatureResponse.from(ecSignatureType);
  }

  @GetMapping(path = "/getMinimalistProcessUser")
  public EcMinimalistProcessUserInfoResponse getMinimalistProcessUser(@RequestParam("loanAccountId") Long loanAccountId) {
    try {
      boolean minimaListProcessByLoanAccountId = userMinimalistJudgeService.isMinimalistProcessUser(loanAccountId);
      return EcMinimalistProcessUserInfoResponse.from(minimaListProcessByLoanAccountId);
    } catch (Exception e) {
      log.error("current user get minimalist process error,loanAccountId is {}", loanAccountId, e);
      return EcMinimalistProcessUserInfoResponse.from(null);
    }
  }

  @GetMapping(path = "/getH5HalfProcessUser")
  public EcH5HalfProcessUserInfoResponse getH5HalfProcessUser(@RequestParam("loanAccountId") Long loanAccountId) {
    try {
      boolean hitH5HalfUser = loanAccountService.hitH5HalfUserForRisk(loanAccountId);
      return EcH5HalfProcessUserInfoResponse.from(hitH5HalfUser);
    } catch (Exception e) {
      log.error("current user get minimalist process error,loanAccountId is {}", loanAccountId, e);
      return EcH5HalfProcessUserInfoResponse.from(null);
    }
  }

  /**
   * 检查用户复贷额度是否有效
   *
   * @param loanAccountId
   * @return
   */
  @GetMapping(path = "/getReloanCreditInValid")
  public EcReloanCreditInValidInfoResponse getReloanCreditInValid(@RequestParam("loanAccountId") Long loanAccountId) {
    try {
      boolean isReloan = loanAccountService.isReloan(loanAccountId);
      boolean noOrder = loanOrderService.countUncompletedOrder(loanAccountId) == 0;
      boolean res = isReloan && noOrder && cashLoanCalcCreditsService.getCalcCreditsStatus(loanAccountId).calcCreditsStatus == CALC_CREDITS_EXPIRED;
      return EcReloanCreditInValidInfoResponse.from(res);
    } catch (Exception e) {
      log.error("current user get minimalist process error,loanAccountId is {}", loanAccountId, e);
      return EcReloanCreditInValidInfoResponse.from(null);
    }
  }

  @PostMapping(path = "/batchGetMaxOrderReqInEveryAccountId")
  public EcAccountMaxOrderSeqResponse batchGetMaxOrderReqInEveryAccountId(@RequestBody @Valid EcBatchGetMaxOrderReqInEveryAccountIdRequest request) {
    List<Long> loanAccountIds = request.loanAccountIds;
    EcAsserts.assertTrue(loanAccountIds != null && !loanAccountIds.isEmpty(),
        "batchGetMaxOrderReqInEveryAccountId Request accountIds is empty!!!", riskConfig.getBatchFindCount());
    EcAsserts.assertTrue(loanAccountIds.size() < riskConfig.getBatchFindCount(),
        "batchGetMaxOrderReqInEveryAccountId Request exceeds limit {}", riskConfig.getBatchFindCount());
    List<CashLoanMaxOrderSeqVo> voList = loanOrderService.getMaxSeqByAccountIds(loanAccountIds, request.beginTimePayout, request.endTimePayout);
    return EcAccountMaxOrderSeqResponseConverter.convert(voList);
  }

  /**
   * @param startTime
   * @param endTime
   * @return
   */
  @GetMapping(path = "/getThirdPartyStatApi")
  public EcThirdPartyStatApiResponse getThirdPartyStatApi(@RequestParam("startTime") Long startTime, @RequestParam("endTime") Long endTime) {
    Map<String, Integer> iziDataRawDataNumberMap = iziDataRawDataModel.getIziDataRawDataRecord(ThirdPartyApi.iziDataApiList, startTime, endTime);
    Map<String, Integer> advanceRawDataNumberMap = advanceRawDataModel.getAdvanceRawDataRecord(ThirdPartyApi.advanceApiList, startTime, endTime);
    Map<String, Integer> faceppRawDataNumberMap = faceppRawDataModel.getFaceppRawDataRecord(ThirdPartyApi.facePPApiList, startTime, endTime);
    Map<String, Integer> tongdunLivingRawDataNumberMap = tongDunLivingRawDataService.getTongDunLivingRawDataRecord(
        ThirdPartyApi.tongDunLivingApiList, startTime, endTime);
    Map<String, Integer> allThirdPartyApiStatNum = new HashMap<>();
    allThirdPartyApiStatNum.putAll(iziDataRawDataNumberMap);
    allThirdPartyApiStatNum.putAll(advanceRawDataNumberMap);
    allThirdPartyApiStatNum.putAll(faceppRawDataNumberMap);
    allThirdPartyApiStatNum.putAll(tongdunLivingRawDataNumberMap);
    Integer facev2VerifyUsageStatNum = secureLivingInfoModel.countByGap(startTime, endTime).intValue();
    Integer financingAdvanceOcrUsageStatNum = financingUserAdditionalInfoModel.countFinancingOcrStat(FinancingUserAdditionalInfoType.OCR, startTime, endTime);
    return EcLoanThirdPartyStatApiResponseConverter.convert(allThirdPartyApiStatNum, facev2VerifyUsageStatNum, financingAdvanceOcrUsageStatNum);
  }

  @GetMapping(path = "/getUserIncreaseCreditsInfo")
  public EcUserIncreaseCreditsInfoResponse getUserIncreaseCreditsInfo(@RequestParam("loanAccountId") Long loanAccountId) {
    try {
      List<IncreaseCreditsReviewInfoVO> userIncreaseCreditsInfo = loanUserExtraInfoReviewService.getUserIncreaseCreditsInfo(loanAccountId);
      return EcUserIncreaseCreditsInfoResponseConverter.convert(userIncreaseCreditsInfo);
    } catch (Exception e) {
      log.error("current user get increase credits have problem, loanAccountId is {}", loanAccountId, e);
      return null;
    }
  }

  @GetMapping(path = "/getUserIncreaseCreditsInfoAllStatus")
  public EcUserIncreaseCreditsInfoAllStatusResponse getUserIncreaseCreditsInfoAllStatus(@RequestParam(value = "loanAccountId", required = true) Long loanAccountId) {
    List<IncreaseCreditsReviewInfoVO> list = loanUserExtraInfoReviewService.getUserIncreaseCreditsInfoAllStatus(loanAccountId);
    return EcUserIncreaseCreditsInfoAllStatusResponseConverter.convert(list);
  }


  @GetMapping(path = "/getAdAppsflyerRawData")
  public EcAdAppsflyerRawDataResponse getAdAppsflyerRawData(@RequestParam(value = "userId") Long userId,
                                                            @RequestParam(value = "timestamp", required = false) Long timestamp) {
    if (timestamp == null) {
      timestamp = Clock.now();
    }
    AdAppsflyerRecordVO appsflyerRecord = adAppsflyerService.findByUserId(userId);
    if (appsflyerRecord == null || StringUtils.isBlank(appsflyerRecord.getAppsflyerId())) {
      return null;
    }
    AppsflyerRawDataVO appsflyerRawDataVO = adAppsflyerService.fetchLatestMediaSourceNotBlandRawDataByAppsflyerIdAndEndTime(appsflyerRecord.getAppsflyerId(), timestamp);
    if (appsflyerRawDataVO == null) {
      return null;
    }
    return EcAdAppsflyerRawDataResponse.from(appsflyerRawDataVO.getMediaSource(),
        appsflyerRawDataVO.getContributor1MediaSource(),
        appsflyerRawDataVO.getContributor2MediaSource(),
        appsflyerRawDataVO.getContributor3MediaSource(),
        appsflyerRawDataVO.getEvent().code,
        JsonUtils.toString(appsflyerRawDataVO.getPushBody()));
  }

  @GetMapping(path = "/getLatestUserDeviceFeature")
  public EcUserDeviceInfoResponse getUserDeviceFeature(@RequestParam("userId") Long userId, @RequestParam("maxQueryTime") Long maxQueryTime) {
    if (userId == null || maxQueryTime == null) {
      log.error("RiskController#getUserDeviceInfo, params is invalid, userId is {}, endTime is {}", userId, maxQueryTime);
      return new EcUserDeviceInfoResponse();
    }
    RiskUserDeviceInfoVo riskUserDeviceInfoVo = riskDetailService.getLatestRiskUserDeviceInfo(userId, maxQueryTime);
    return EcUserDeviceInfoResponseConverter.convert(riskUserDeviceInfoVo);
  }

  @GetMapping(path = "/getLatestOcrResponse")
  public EcUserOCRResponse getLatestOcrResponse(@RequestParam("loanAccountId") Long loanAccountId, @RequestParam("timestamp") Long timestamp) {
    if (loanAccountId == null || timestamp == null) {
      log.error("RiskController#getLatestOcrResponse, params is invalid, loanAccountId is {}, timestamp is {}", loanAccountId, timestamp);
      return new EcUserOCRResponse();
    }
    OcrResponse ocrResponse = ocrService.fetchLatestOcrResponseByAccountIdAndMaxTime(loanAccountId, timestamp);
    return EcUserOCRResponseConverter.convert(ocrResponse);
  }

  @GetMapping(path = "/getIdentityNumberWithImmediateContact")
  public ECIdentityNumberWithImmediateContactResponse getIdentityNumberWithImmediateContact(
      @RequestParam("loanAccountId") Long loanAccountId,
      @RequestParam("timeStamp") Long timeStamp
  ) {
    List<UserImmediateContactRecord> userImmediateContactRecords = userImmediateContactService
        .getByLoanAccountIdAndTypes(loanAccountId, ImmediateContactType.FIRST_SECOND_IMMEDIATE_CONTACT);

    Map<Long, Map<LoanAccountVO, Long>> immediateIdToAccountIdsMap = userImmediateContactRecords.stream().collect(Collectors.toMap(
        UserImmediateContactRecord::getId,
        userImmediateContactRecord -> getLoanAccountIdsFromImmediateContact(userImmediateContactRecord, timeStamp)
    ));

    return ECImmediateContactWithIdentityConverter.convert(immediateIdToAccountIdsMap, userImmediateContactRecords);
  }

  @GetMapping(path = "/updateRiskUserLevel")
  public Boolean updateRiskUserLevel(
      @RequestParam("accountId") Long loanAccountId,
      @RequestParam("modelTypeName") String modelTypeName,
      @RequestParam("modelName") String modelName,
      @RequestParam("requester") short requesterId,
      @RequestParam("score") BigDecimal score,
      @RequestParam("timeCreated") Long timeCreated
  ) {
    return riskUserLevelService.updateRiskUserLevel(loanAccountId, requesterId, modelTypeName, modelName, score, timeCreated);
  }

  public Map<LoanAccountVO, Long> getLoanAccountIdsFromImmediateContact(UserImmediateContactRecord userImmediateContactRecord, Long timeStamp) {
    Long daysAgoTimeStamp = Clock.plusOffsetDaysMills(timeStamp, SDKType.IDN_YQD.getTimeZone(), DAYS_AGO);
    List<UserImmediateContactRecord> userImmediateContactRecords = userImmediateContactService
        .getByMobileAndTimeCreatedAndContactType(userImmediateContactRecord.getNormalizedMobileNumber(), daysAgoTimeStamp, userImmediateContactRecord.getTimeCreated());
    Map<Long, Long> loanAccountIdToTimeCreatedMap = userImmediateContactRecords.stream().collect(Collectors.toMap(UserImmediateContactRecord::getLoanAccountId, UserImmediateContactRecord::getTimeCreated));
    Set<Long> loanAccountIds = loanAccountIdToTimeCreatedMap.keySet();
    Map<Long, LoanAccountVO> loanAccountVOMap = loanAccountService.getAccounts(loanAccountIds);

    return loanAccountVOMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, entry -> loanAccountIdToTimeCreatedMap.get(entry.getKey())));
  }

  @GetMapping(path = "/getTongDunTaskInfoNew")
  public ECTongDunTaskInfoNewResponse getTongDunTaskInfoNew(@RequestParam("accountId") Long loanAccountId,
                                                            @RequestParam("apiType") String apiType,
                                                            @RequestParam("status") String status) {
    List<TongDunTaskInfoNewVO> taskInfoNewVOList = loanCreditService.fetchTongDunTaskInfoNewByLoanAccountIdAndAndApiTypeAndStatus(loanAccountId,
        apiType,
        status);
    if (Objects.isNull(taskInfoNewVOList)) {
      return new ECTongDunTaskInfoNewResponse();
    }
    return EcTongDunTaskInfoResponseConverter.convertOriginTask(taskInfoNewVOList);
  }

  @GetMapping(path = "/getTongDunTaskInfoNewV2")
  public ECTongDunTaskInfoNewResponse getTongDunTaskInfoNewV2(
      @RequestParam("loanAccountId") Long loanAccountId,
      @RequestParam("apiType") String apiType,
      @RequestParam("statuses") List<String> statuses
  ) {
    List<TongDunTaskInfoNewVO> taskInfoNewVOList = loanCreditService
        .fetchTongDunTaskInfoNewByLoanAccountIdAndAndApiTypesAndStatus(loanAccountId, apiType, statuses);

    return EcTongDunTaskInfoResponseConverter.convertOriginTask(taskInfoNewVOList);
  }

  @GetMapping(path = "/getUserWillingInfo")
  public ECUserOrderWillingInfoResponse getUserWillingInfo(
      @RequestParam("accountId") Long loanAccountId
  ) {
    LoanUserCreateOrderWillingnessRecord willingnessRecord = loanIntentionRepository.fetchByLoanAccountId(loanAccountId);
    if (Objects.isNull(willingnessRecord)) {
      return new ECUserOrderWillingInfoResponse();
    }
    return ECUserOrderWillingInfoResponse.from(willingnessRecord.getUserId(), willingnessRecord.getLoanAccountId(), willingnessRecord.getWillingCredits(), willingnessRecord.getTerms(), willingnessRecord.getStatus(), willingnessRecord.getTimeCreated());
  }

  @GetMapping(path = "/getUserSupplementStatusInfo")
  public ECUserSupplementInfoResponse getUserSupplementStatusInfo(@RequestParam("accountId") Long loanAccountId, @RequestParam("traceTime") Long traceTime) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = supplementCreateOrderModel.fetchLastedByLoanAccountIdAndTimeCreate(loanAccountId, traceTime);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return new ECUserSupplementInfoResponse();
    }
    CashLoanCreateOrderRequestVO orderRequestVO = mongoSupplementBeforeCreateOrderModel.findByObjectIdOrNull(supplementCreateOrderRecord.getObjectId());
    if (Objects.isNull(orderRequestVO)) {
      orderRequestVO = new CashLoanCreateOrderRequestVO();
    }
    BigDecimal remainCredits = loanUserCreditsService.getTotalRemainCredits(loanAccountId);
    return ECUserSupplementInfoResponse.from(supplementCreateOrderRecord.getUserId(),
        supplementCreateOrderRecord.getLoanAccountId(),
        supplementCreateOrderRecord.getFirstTraceId(),
        supplementCreateOrderRecord.getConfirmTraceId(),
        orderRequestVO.principal,
        remainCredits,
        supplementCreateOrderRecord.getStatus(),
        supplementCreateOrderRecord.getTimeCreated(),
        supplementCreateOrderRecord.getTimeUpdated(),
        supplementCreateOrderRecord.getTimeExpired());
  }

  @PostMapping(path = "/initRiskIvrAuth")
  public EcRiskIvrAuthResponse initRiskIvrAuth(@RequestBody @Valid EcRiskIvrAuthInitRequest request) {
    IvrAuthRecord ivrAuthRecord = riskIvrAuthService.init(
        request.userId,
        SDKType.fromCode(request.sdkTypeCode));

    return EcRiskIvrAuthResponse.from(
        ivrAuthRecord.getId(),
        ivrAuthRecord.getUserId(),
        SDKType.fromCode(ivrAuthRecord.getSdkType()),
        ivrAuthRecord.getType(),
        ivrAuthRecord.getStatus(),
        ivrAuthRecord.getResult(),
        ivrAuthRecord.getTimeCreated(),
        ivrAuthRecord.getTimeUpdated());
  }

  @GetMapping(path = "/getRiskIvrAuth")
  public EcRiskIvrAuthResponse getRiskIvrAuth(@RequestParam("userId") Long userId) {
    IvrAuthRecord ivrAuthRecord = riskIvrAuthService.getByUserId(userId);
    if (Objects.isNull(ivrAuthRecord)) {
      return new EcRiskIvrAuthResponse();
    }
    return EcRiskIvrAuthResponse.from(
        ivrAuthRecord.getId(),
        ivrAuthRecord.getUserId(),
        SDKType.fromCode(ivrAuthRecord.getSdkType()),
        ivrAuthRecord.getType(),
        ivrAuthRecord.getStatus(),
        ivrAuthRecord.getResult(),
        ivrAuthRecord.getTimeCreated(),
        ivrAuthRecord.getTimeUpdated());
  }

  @GetMapping(path = "/getUserSupplementInfoByTraceId")
  public ECUserSupplementInfoResponse getUserSupplementStatusInfo(@RequestParam("traceId") Long traceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = supplementCreateOrderModel.fetchByFirstTraceId(traceId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      supplementCreateOrderRecord = supplementCreateOrderModel.fetchByConfirmTraceId(traceId);
    }
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return new ECUserSupplementInfoResponse();
    }
    CashLoanCreateOrderRequestVO orderRequestVO = mongoSupplementBeforeCreateOrderModel.findByObjectIdOrNull(supplementCreateOrderRecord.getObjectId());
    if (Objects.isNull(orderRequestVO)) {
      orderRequestVO = new CashLoanCreateOrderRequestVO();
    }
    BigDecimal remainCredits = loanUserCreditsService.getTotalRemainCredits(supplementCreateOrderRecord.getLoanAccountId());
    return ECUserSupplementInfoResponse.from(supplementCreateOrderRecord.getUserId(),
        supplementCreateOrderRecord.getLoanAccountId(),
        supplementCreateOrderRecord.getFirstTraceId(),
        supplementCreateOrderRecord.getConfirmTraceId(),
        orderRequestVO.principal,
        remainCredits,
        supplementCreateOrderRecord.getStatus(),
        supplementCreateOrderRecord.getTimeCreated(),
        supplementCreateOrderRecord.getTimeUpdated(),
        supplementCreateOrderRecord.getTimeExpired());
  }

  /**
   * 2.risk_user_account_mapping 提供接口 可选字段（ userIdList｜loanAccountIdList）参数二选一，业务根据参数来决定如何查询。
   */
  @GetMapping(path = "/queryRiskUserAccountMapping")
  public EcRiskUserAccountMappingResponse queryRiskUserAccountMapping(@RequestParam(value = "userIdList", required = false) @Size(max = 500, message = "userIdList list cannot exceed 500 elements") List<Long> userIdList,
                                                                      @RequestParam(value = "loanAccountIdList", required = false) @Size(max = 500, message = "loanAccountIdList list cannot exceed 500 elements") List<Long> loanAccountIdList) {
    if (CollectionUtils.isEmpty(userIdList) && CollectionUtils.isEmpty(loanAccountIdList)) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "required at least 1 non-null parameter");
    }
    List<RiskUserAccountMappingVO> cashLoanInstalmentVOList = riskUserService.getUserIdOrAccountIdByCondition(userIdList, loanAccountIdList);
    if (Objects.isNull(cashLoanInstalmentVOList)) {
      return new EcRiskUserAccountMappingResponse();
    }
    EcRiskUserAccountMappingResponse response = new EcRiskUserAccountMappingResponse();
    response.riskUserAccountMappingList = convertRiskUserAccountMapping(cashLoanInstalmentVOList);
    return response;
  }

  /**
   * 3.LoanUserExtraInfo 提供接口查询用户额外信息 （可选字段｜accountId | statusList | source | startTime | endTime）
   */
  // TODO(mario): will delete queryLoanUserExtraInfo after queryLoanUserExtraInfoV2 test is completed
  @GetMapping(path = "/queryLoanUserExtraInfo")
  public EcRiskUserExtraInfoDetailResponse queryLoanUserExtraInfo(
      @RequestParam(value = "accountId") Long accountId,
      @RequestParam("reviewStatuses") List<ExtraInfoReviewStatus> reviewStatuses,
      @RequestParam(value = "typeList", required = false) List<String> typeList,
      @RequestParam(value = "source", required = false) String source,
      @RequestParam(value = "startTime") Long startTime,
      @RequestParam(value = "endTime", required = false) Long endTime
  ) {
    List<LoanUserExtraInfoVO> extraInfoVOList = loanUserExtraInfoReviewService.fetchByCondition(
        accountId,
        reviewStatuses,
        typeList,
        source,
        startTime,
        endTime);
    if (Objects.isNull(extraInfoVOList)) {
      return new EcRiskUserExtraInfoDetailResponse();
    }
    EcRiskUserExtraInfoDetailResponse response = new EcRiskUserExtraInfoDetailResponse();
    response.userExtraInfoList = EcLoanExtraInfoDetailResponseConverter.convert(extraInfoVOList);
    return response;
  }

  @PostMapping(path = "/queryLoanUserExtraInfoV2")
  public EcRiskUserExtraInfoDetailResponse queryLoanUserExtraInfoV2(@RequestBody @Valid EcQueryLoanUserExtraInfoRequest request) {
    List<LoanUserExtraInfoVO> extraInfoVOList = loanUserExtraInfoReviewService.fetchByCondition(
        request.accountId,
        request.reviewStatuses,
        request.typeList,
        request.source,
        request.startTime,
        request.endTime);
    if (Objects.isNull(extraInfoVOList)) {
      return new EcRiskUserExtraInfoDetailResponse();
    }
    EcRiskUserExtraInfoDetailResponse response = new EcRiskUserExtraInfoDetailResponse();
    response.userExtraInfoList = EcLoanExtraInfoDetailResponseConverter.convert(extraInfoVOList);
    return response;
  }

  /**
   * todo (LTB,T000000) 4.user_mobile_history 提供查询接口 （可选字段 ｜ userId ｜ timeCreate | limit） 其中 limit 可以不要，接口可扩展性差一些。
   */
  // TODO(Hafiz) : this method will be deprecated after v2 finish tested
  @GetMapping(path = "/queryRiskOutputResult")
  public EcRiskOutputResultResponse queryRiskOutputResult(
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "type", required = false) String type,
      @RequestParam(value = "traceIds", required = false) List<Long> traceIds,
      @RequestParam(value = "startTimeCreated", required = false) Long startTimeCreated,
      @RequestParam(value = "endTimeCreated", required = false) Long endTimeCreated
  ) {
    if (loanAccountId == null && type == null && traceIds == null && startTimeCreated == null && endTimeCreated == null) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "required at least 1 non-null parameter");
    }
    RiskOutputResultCondition condition = RiskOutputResultCondition.builder()
        .loanAccountId(loanAccountId)
        .type(type)
        .traceIds(traceIds)
        .startTimeCreated(startTimeCreated)
        .endTimeCreated(endTimeCreated)
        .build();
    List<RiskOutputResultSimpleVO> riskOutputResultSimpleVOList = riskDetailService.fetchRiskOutputResultByCondition(condition);
    EcRiskOutputResultResponse response = new EcRiskOutputResultResponse();
    response.riskOutputResultInfoList = EcRiskOutputResultResponseConverter.convert(riskOutputResultSimpleVOList);
    return response;
  }

  @PostMapping(path = "/queryRiskOutputResultV2")
  public EcRiskOutputResultResponse queryRiskOutputResult(@RequestBody @Valid EcRiskOutputResultRequest request) {
    if (Objects.isNull(request.loanAccountId) && CollectionUtils.isEmpty(request.traceIds)) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "required at least 1 non-null parameter");
    }
    RiskOutputResultCondition condition = RiskOutputResultCondition.builder()
        .loanAccountId(request.loanAccountId)
        .type(request.type)
        .traceIds(request.traceIds)
        .startTimeCreated(request.startTimeCreated)
        .endTimeCreated(request.endTimeCreated)
        .build();
    List<RiskOutputResultSimpleVO> riskOutputResultSimpleVOList = riskDetailService.fetchRiskOutputResultByCondition(condition);
    EcRiskOutputResultResponse response = new EcRiskOutputResultResponse();
    response.riskOutputResultInfoList = EcRiskOutputResultResponseConverter.convert(riskOutputResultSimpleVOList);
    return response;
  }

  /**
   * 查询最新风险输出快照（risk_output_latest），不回落历史表 risk_output_result。
   */
  @GetMapping(path = "/queryRiskOutputLatest")
  public EcRiskOutputLatestResponse queryRiskOutputLatest(
      @RequestParam(value = "loanAccountId") Long loanAccountId,
      @RequestParam(value = "jsonKey") String jsonKey
  ) {
    if (loanAccountId == null || StringUtils.isBlank(jsonKey)) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "loanAccountId and jsonKey are required");
    }
    RiskOutputLatestSnapshot snapshot =
        riskOutputService.findLatestTableSnapshotByLoanAccountIdAndJsonKey(loanAccountId, jsonKey);
    EcRiskOutputLatestResponse response = new EcRiskOutputLatestResponse();
    response.loanAccountId = loanAccountId;
    response.jsonKey = jsonKey;
    if (snapshot == null) {
      response.found = false;
      response.newValue = null;
      response.traceId = null;
      response.timeUpdated = null;
    } else {
      response.found = true;
      response.newValue = snapshot.newValue;
      response.traceId = snapshot.traceId;
      response.timeUpdated = snapshot.timeUpdated;
    }
    return response;
  }

  @GetMapping(path = "/queryRiskUser")
  public EcRiskUserResponse queryRiskUser(@RequestParam(value = "idNumbers") @Size(max = 500, message = "idNumbers size cannot be more than 500") List<String> idNumbers) {
    List<RiskUserVO> riskUserVOList = riskUserService.fetchByIdNumbers(idNumbers);
    EcRiskUserResponse response = new EcRiskUserResponse();
    response.riskUserInfoList = EcRiskUserResponseConverter.convert(riskUserVOList);
    return response;
  }


  @GetMapping(path = "/queryTongDunTaskInfoNew")
  public EcRiskTongdunProcessParamResponse queryTongDunTaskInfoNew(@RequestParam(value = "taskId") Long taskId) {
    TongdunProcessParam param = loanCreditService.fetchTongdunTriggerTaskParam(taskId);
    EcRiskTongdunProcessParamResponse response = new EcRiskTongdunProcessParamResponse();
    response.param = EcRiskTongDunInfoNewParamConverter.convert(param);
    return response;
  }

  @PostMapping(path = "/updateTongDunTaskInfoNew")
  public void updateTongDunTaskInfoNew(@RequestBody @Valid TongDunTaskNewUpdateRequest request) {
    loanCreditService.updateTongdunTriggerTask(request.taskId, TongDunTaskNewStatus.valueOf(request.newStatus));
  }

  @PostMapping(path = "/callbackTraceCreated")
  public EcRiskCallBackTraceCreatedResponse callbackTraceCreated(@RequestBody @Valid EcCallbackTraceFinishRequest request) {
    LoanUserRiskTraceRecord riskTraceResultRecord = threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(request.loanAccountId);
      LoanUserRiskTraceRecord riskTraceRecord = loanUserRiskTraceModel.findById(request.loanUserRiskTraceId);
      if (Objects.nonNull(riskTraceRecord) && RiskFlowTraceStatusV2.PRE_INIT != RiskFlowTraceStatusV2.fromCode(riskTraceRecord.getStatus())) {
        log.info("trace status is not PRE_INIT,userId is {},loanAccountId is {},traceId is {}", riskTraceRecord.getUserId(), riskTraceRecord.getLoanAccountId(), riskTraceRecord.getTraceId());
        return riskTraceRecord;
      }
      checkRiskTrace(request, riskTraceRecord);
      loanUserRiskTraceModel.updateStatusAndTraceId(riskTraceRecord, RiskFlowTraceStatusV2.INIT, request.traceId);
      riskApplicationSubmitService.dealAfterGetTraceVO(riskTraceRecord, request.callBackTraceVO);
      return riskTraceRecord;
    });
    EcRiskCallBackTraceCreatedResponse response = new EcRiskCallBackTraceCreatedResponse();
    response.loanAccountId = riskTraceResultRecord.getLoanAccountId();
    response.loanUserRiskTraceDataId = riskTraceResultRecord.getId();
    response.traceId = riskTraceResultRecord.getTraceId();

    return response;
  }

  private static void checkRiskTrace(EcCallbackTraceFinishRequest request, LoanUserRiskTraceRecord riskTraceRecord) {
    if (Objects.isNull(riskTraceRecord)) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "no data error,loanUserRiskTraceId is {}", request.loanUserRiskTraceId);
    }
    if (RiskFlowTraceStatusV2.PRE_INIT != RiskFlowTraceStatusV2.fromCode(riskTraceRecord.getStatus())) {
      throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "status error risk trace status is {}", riskTraceRecord.getStatus());
    }
  }

  @PostMapping(path = "/queryRiskUserAccountMappingLoanAccountId")
  public EcLoanAccountIdResponse queryRiskUserAccountMappingLoanAccountId(
      @RequestBody @Validated EcLoanAccountIdRequest request) {
    EcLoanAccountIdResponse response = new EcLoanAccountIdResponse();
    response.loanAccountIds = riskUserService.getByLoanAccountIdJoin(request.loanAccountIds);
    return response;
  }

  @PostMapping(path = "/updateTraceStatusToCancelledByTraceId")
  public Boolean updateTraceStatusToCancelledByTraceId(@RequestBody @Valid EcUpdateTraceStatusToCancelledRequest request) {
    return riskUserService.updateTraceStatusToCancelledByTraceId(request.traceId);
  }

  @PostMapping(path = "/queryBatchTriggerRiskTask")
  public EcBatchTriggerRiskTaskResponse queryBatchTriggerRiskTask(@RequestBody @Validated EcBatchTriggerRiskTaskRequest request) {
    EcBatchTriggerRiskTaskResponse response = new EcBatchTriggerRiskTaskResponse();
    List<TaskDataVO> taskDataVOList = batchTriggerService.listTasks(request.startTime, request.endTime, "");
    response.batchTriggerRiskTaskInfoList = EcBatchTriggerRiskTaskResponseConverter.convert(taskDataVOList, BatchTriggerTaskType.MANUAL);
    response.autoBatchTriggerRiskTaskInfoList = EcBatchTriggerRiskTaskResponseConverter.convert(taskDataVOList, BatchTriggerTaskType.AUTO);
    List<MarketingBatchTriggerRiskRelationVO> marketingBatchTriggerRiskRelationVOList = marketingRiskBatchTriggerService.getMarketingBatchTriggerRiskRelationList(request.startTime, request.endTime);
    response.marketingBatchTriggerRiskTaskInfoList = EcBatchTriggerRiskTaskResponseConverter.convertMarketingTask(marketingBatchTriggerRiskRelationVOList);
    return response;
  }


  @GetMapping(path = "/checkModelUse")
  public EcRiskLevelScoreConfigV2Response checkModelUse(@RequestParam(value = "modelName") String modelName) {
    boolean enable = riskLevelScoreConfigService.checkModelUse(modelName);
    EcRiskLevelScoreConfigV2Response response = new EcRiskLevelScoreConfigV2Response();
    response.modelName = modelName;
    response.enable = enable;
    return response;
  }

  @PostMapping(path = "/queryLastedApplist")
  public EcLoanAccountAppInfoResponse queryLastedApplist(@RequestBody @Validated EcLastedApplistRequest request) {
    AppInfoPojoWithTimeCreateVO appInfoPojoWithTimeCreateVO = appListService.getLastedApplistByAccountIdAndStartTime(request.loanAccountId, request.timeStamp);
    if (Objects.isNull(appInfoPojoWithTimeCreateVO)) {
      return new EcLoanAccountAppInfoResponse();
    }
    return EcLoanAccountAppInfoResponse.from(appInfoPojoWithTimeCreateVO.objectId,
        LoanAccountAppInfoResponseConverter.convert(appInfoPojoWithTimeCreateVO.appInfoPojo),
        appInfoPojoWithTimeCreateVO.timeCreate);
  }

  @GetMapping(path = "/fetchUserAppListInfo")
  public EcRiskUserAppListInfoResponse fetchUserAppListInfo(@RequestParam(value = "accountId") Long accountId) {
    Long oldAppListTimeCreated = appListService.fetchOldestAppListTimeCreated(accountId);
    if (Objects.isNull(oldAppListTimeCreated)) {
      return new EcRiskUserAppListInfoResponse();
    }
    return EcRiskUserAppListInfoResponse.from(oldAppListTimeCreated);
  }

  @PostMapping(path = "/queryOldestApplist")
  public EcLoanAccountAppInfoResponse queryOldestApplist(@RequestBody @Validated EcOldestApplistRequest request) {
    AppInfoPojoWithTimeCreateVO appInfoPojoWithTimeCreateVO = appListService.fetchOldestAppListInfo(request.loanAccountId, request.timeStamp);
    if (Objects.isNull(appInfoPojoWithTimeCreateVO)) {
      return new EcLoanAccountAppInfoResponse();
    }
    return EcLoanAccountAppInfoResponse.from(
        LoanAccountAppInfoResponseConverter.convert(appInfoPojoWithTimeCreateVO.appInfoPojo),
        appInfoPojoWithTimeCreateVO.timeCreate);
  }

  /**
   * @return
   * @title 风控人群支持的风险类型查询
   * @desc 返回每个人群类别支持的风险类型列表
   * @responseClass com.yqg.ec.common.spring.response.risk.EcRiskCrowdCategorySupportedRiskTypeResponse
   */
  @GetMapping(path = "/getRiskCrowdCategorySupportedRiskType")
  public EcRiskCrowdCategorySupportedRiskTypeResponse getRiskCrowdCategorySupportedRiskType() {
    Map<String, List<String>> result = Arrays.stream(RiskCrowdCategory.values())
        .collect(Collectors.toMap(
            RiskCrowdCategory::name,
            e -> e.getRiskTypeList().stream().map(Enum::name).collect(Collectors.toList())
        ));
    EcRiskCrowdCategorySupportedRiskTypeResponse response = new EcRiskCrowdCategorySupportedRiskTypeResponse();
    response.setCrowdCategoryToSupportedRiskTypeMap(result);
    return response;
  }

  @PostMapping(path = "/batchQueryUserMaxLoanInfo")
  public EcBatchQueryUserMaxLoanInfoResponse batchQueryUserMaxLoanInfo(@RequestBody @Validated EcBatchQueryUserMaxLoanInfoRequest request) {
    return userMaxLoanInfoService.batchQueryUserMaxLoanInfo(request.loanAccountIds, request.checkTime);
  }

  @PostMapping(path = "/batchQueryUserLastedLoanInfo")
  public EcBatchQueryUserLastedLoanInfoResponse batchQueryUserLastedLoanInfo(@RequestBody @Validated EcBatchQueryUserLastedLoanInfoRequest request) {
    return userMaxLoanInfoService.batchQueryUserLastedLoanInfo(request.loanAccountIds, request.checkTime);
  }

  @PostMapping(path = "/queryUserGroupLog")
  public QueryUserGroupLogResponse queryUserGroupLog(@RequestBody @Validated QueryUserGroupLogRequest request) {
    LoanRiskUserGroupLogVO vo = loanRiskUserGroupService.getGroupLogVOByAccountIdAndTimeCreatedBefore(request.loanAccountId, request.timestamp);
    return QueryUserGroupLogResponseConverter.from(vo);
  }

  @GetMapping(path = "/queryAppDataSourceCandidates")
  public AppDataSourceCandidateResponse queryAppDataSourceCandidates(
      @RequestParam("accountId") Long accountId,
      @RequestParam("timestamp") Long timestamp) {
    List<AppDataSourceCandidate> candidates =
        appDataSourceQueryService.queryAllAppDataSources(accountId, timestamp);
    return new AppDataSourceCandidateResponse(candidates);
  }

}
