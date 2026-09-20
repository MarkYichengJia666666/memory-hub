package com.yqg.core.service.antifraud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Lists;
import com.yqg.collection.common.utils.AESCrypt;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.sql.antifraud.AntiFraudOverdueCaseLogModel;
import com.yqg.core.model.sql.antifraud.AntiFraudOverdueCaseModel;
import com.yqg.core.model.sql.antifraud.AntiFraudOverdueReviewLogModel;
import com.yqg.core.model.sql.cashloan.CashLoanInstalmentModel;
import com.yqg.core.model.sql.contact.UserImmediateContactModel;
import com.yqg.core.model.sql.loan.additionalinfo.LoanUserAdditionalInfoModel;
import com.yqg.core.model.sql.loan.additionalinfo.enums.LoanUserAdditionalInfoType;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.user.UserModel;
import com.yqg.core.service.antifraud.enums.AntiFraudOverdueTagEnum;
import com.yqg.core.service.antifraud.monitor.AntiFraudOverdueMonitorService;
import com.yqg.core.service.antifraud.vo.*;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanOrderAdditionalInfoService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.additioanalinfo.vo.LoanUserAdditionalInfoVO;
import com.yqg.core.service.loan.infos.LivingInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.SimpleLoanAccountVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.tool.EmailService;
import com.yqg.core.util.datawrapper.ContactInfoWrapper;
import com.yqg.core.util.file.EasyExcelUtils;
import com.yqg.core.util.file.MultiQueryCondition;
import com.yqg.core.util.fileopreator.CloudFileUtil;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.antifraud.AntiFraudOverdueCaseStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.yqg.core.configure.EcExecutorConfig.ANTI_FRAUD_EXPORT;
import static com.yqg.ec.common.exception.EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST;

/**
 * @author chenxianrui
 * @date 2025/6/16
 */
@Service
@Slf4j
public class AntiFraudOverdueService {
  @Autowired
  private AntiFraudOverdueCaseModel antiFraudOverdueCaseModel;
  @Autowired
  private AntiFraudOverdueCaseLogModel antiFraudOverdueCaseLogModel;
  @Autowired
  private AntiFraudOverdueReviewLogModel antiFraudOverdueReviewLogModel;
  @Autowired
  private UserModel userModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanAccountDetailsService accountDetailsService;
  @Autowired
  private UserImmediateContactModel userImmediateContactModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private CashLoanInstalmentModel cashLoanInstalmentModel;
  @Autowired
  private CashLoanOrderAdditionalInfoService cashLoanOrderAdditionalInfoService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private EmailService emailService;
  @Autowired
  private LoanUserAdditionalInfoModel loanUserAdditionalInfoModel;
  @Autowired
  private AntiFraudConfigService antiFraudConfigService;
  @Autowired
  private ContactInfoWrapper contactInfoWrapper;
  @Autowired
  private CloudFileUtil cloudFileUtil;
  @Resource(name = ANTI_FRAUD_EXPORT)
  private Executor antiFarudExportExecutor;
  @Autowired
  private AntiFraudOverdueMonitorService antiFraudOverdueMonitorService;

  public static final String EXCEL_NAME = "ANTI_FRAUD_OVERDUE_RESULT_{0}.xlsx";


  public AntiFraudOverdueDetailVO antiFraudOverdueDetail(Long id) {
    AntiFraudOverdueCaseRecord antiFraudOverdueCaseRecord = antiFraudOverdueCaseModel.findByIdOrThrow(id);
    AntiFraudUserBasicInfoVO userBasicInfoVO = fetchAntiFraudUserBasicInfoByUserId(antiFraudOverdueCaseRecord.getLoanAccountId());

    LoanAccountDetailsVO detailsVO = accountDetailsService.getAuthFinishedDetailsVoByAccountId(antiFraudOverdueCaseRecord.getLoanAccountId());
    AntiFraudUserImageInfoVO userImageInfoVO = fetchAntiFraudUserImageInfoByUserId(detailsVO);
    List<AntiFraudQualityInspectionVO> qualityInspectionVOList = fetchAntiFraudQualityInspectionByUserId(antiFraudOverdueCaseRecord.getUserId());
    AntiFraudOverdueCaseFillOutVO overdueCaseFillOutVO = fetchAntiFraudOverdueCaseFillOutByUserId(antiFraudOverdueCaseRecord.getId());

    return AntiFraudOverdueDetailVO.builder()
        .userBasicInfo(userBasicInfoVO)
        .userImageInfo(userImageInfoVO)
        .overdueCaseFillOut(overdueCaseFillOutVO)
        .qualityInspectionList(qualityInspectionVOList)
        .build();
  }

  private AntiFraudUserBasicInfoVO fetchAntiFraudUserBasicInfoByUserId(Long accountId) {
    LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(accountId);
    Long orderId = cashLoanInstalmentModel.findLatestOverdueOrderIdByAccountId(accountId);
    CashLoanOrderVO cashLoanOrderVO = ecOrderService.getOrderVO(orderId);
    return AntiFraudUserBasicInfoVO.builder()
        .userId(loanAccountVO.userId)
        .loanAccountId(accountId)
        .name(loanAccountVO.name)
        .ktpAccount(loanAccountVO.identityNumber)
        .lastApplyTime(cashLoanOrderVO.timeCreated)
        .lastLoanTime(cashLoanOrderVO.timePayout)
        .maxOverdueDays(loanAccountVO.maxOverdueDays)
        .riskControlScene(cashLoanOrderVO.orderSeq > 1 ? "复贷" : "首贷")
        .loanAmount(cashLoanOrderVO.principal)
        .remainingRepayment(cashLoanOrderVO.calculateOutstandingAmount())
        .build();
  }

  private AntiFraudUserImageInfoVO fetchAntiFraudUserImageInfoByUserId(LoanAccountDetailsVO detailsVO) {
    LivingInfo livingInfo = detailsVO.getLivingInfo();
    return AntiFraudUserImageInfoVO.builder()
        .ktpPhoto(getImageUrl(detailsVO.getIBaseIdInfo().getFrontImage()))
        .livingPhotoList(getLivingPhotoUrlList(livingInfo.livingImageList))
        .build();
  }

  private List<String> getLivingPhotoUrlList(List<String> livingImageList) {
    if (CollectionUtils.isEmpty(livingImageList)) {
      return Collections.emptyList();
    }
    List<String> result = Lists.newArrayList();
    for (String livingImage : livingImageList) {
      result.add(getImageUrl(livingImage));
    }
    return result;
  }

  private String getImageUrl(String imageKey) {
    return StringUtils.isEmpty(imageKey) ? null : cloudFileUtil.getFileDownloadUrl(imageKey);
  }

  private List<AntiFraudQualityInspectionVO> fetchAntiFraudQualityInspectionByUserId(Long userId) {
    List<AntiFraudQualityInspectionVO> result = Lists.newArrayList();

    // 本人信息
    UserRecord userRecord = userModel.findByIdOrThrow(userId);
    AntiFraudQualityInspectionVO ownQualityInspection = fetchUserQualityInspectionByUserId(userRecord, userRecord.getNormalizedMobileNumber(),"本人");
    if (Objects.nonNull(ownQualityInspection)) {
      result.add(ownQualityInspection);
    }

    List<UserImmediateContactRecord> userImmediateContactRecordList = userImmediateContactModel.findByUserId(userId);
    if (CollectionUtils.isEmpty(userImmediateContactRecordList)) {
      return null;
    }
    for (UserImmediateContactRecord userImmediateContactRecord : userImmediateContactRecordList) {
      AntiFraudQualityInspectionVO immediateQualityInspection = fetchUserQualityInspectionByUserId(null, userImmediateContactRecord.getNormalizedMobileNumber(), userImmediateContactRecord.getRelationship());
      if (Objects.nonNull(immediateQualityInspection)) {
        result.add(immediateQualityInspection);
      }
    }

    return result;
  }

  private AntiFraudOverdueCaseFillOutVO fetchAntiFraudOverdueCaseFillOutByUserId(Long caseId) {
    AntiFraudOverdueReviewLogRecord antiFraudOverdueReviewLogRecord = antiFraudOverdueReviewLogModel.findLastedByCaseId(caseId);
    if (Objects.isNull(antiFraudOverdueReviewLogRecord)) {
      return null;
    }

    List<AntiFraudOverdueTagEnum> tagEnums = Lists.newArrayList();
    if (!StringUtils.isEmpty(antiFraudOverdueReviewLogRecord.getTagCodeDetail())) {
      tagEnums = JsonUtils.from(antiFraudOverdueReviewLogRecord.getTagCodeDetail(), new TypeReference<List<AntiFraudOverdueTagEnum>>() {});
    }
    return AntiFraudOverdueCaseFillOutVO.builder()
        .overdueTagCodeList(tagEnums)
        .remark(antiFraudOverdueReviewLogRecord.getRemark())
        .build();
  }

  private AntiFraudQualityInspectionVO fetchUserQualityInspectionByUserId(UserRecord userRecord, String mobileNumber, String relationDesc) {
    return AntiFraudQualityInspectionVO.builder()
        .callNumber(mobileNumber)
        .relationship(relationDesc)
        .jumpQualitySystemUrl(Objects.nonNull(userRecord) ? getUrlByCollectionCreateTime(userRecord.getId()) : buildOldQualityInspectionUrl(mobileNumber))
        .build();
  }

  private String getUrlByCollectionCreateTime(Long userId) {
    AntiFraudOverdueCaseLogRecord antiFraudOverdueCaseLogRecord = antiFraudOverdueCaseLogModel.findEarliestCollectionCreateTimeByUserId(userId);
    if (Objects.isNull(antiFraudOverdueCaseLogRecord)) {
      return "";
    }
    UserRecord userRecord = userModel.findByIdOrThrow(userId);
    long cutoffTime = Clock.dateStringToLong(antiFraudConfigService.getNewOldQualityInspectionSystemCutoffTime(), DateFormatter.yyyy_MM_dd___HH_mm_ss, EcTimeZone.JAKARTA.tz);
    // 判断并返回对应 URL
    return antiFraudOverdueCaseLogRecord.getCollectionCaseTimeCreated() < cutoffTime ? buildOldQualityInspectionUrl(userRecord.getNormalizedMobileNumber()) : buildNewQualityInspectionUrl(userId);
  }

  private String buildNewQualityInspectionUrl(Long userId) {
    return antiFraudConfigService.getNewQualityInspectionSystemUrl() + userId;
  }

  private String buildOldQualityInspectionUrl(String normalizedMobileNumber) {
    String mobileNumber = contactInfoWrapper.clean(normalizedMobileNumber, SDKType.IDN_YQD);
    String encryptAesKey = AESCrypt.AES_Encrypt(antiFraudConfigService.getNewQualityInspectionSystemPhoneAesKey(), mobileNumber);
    return antiFraudConfigService.getOldQualityInspectionSystemUrl() + encryptAesKey;
  }

  public List<AntiFraudOverdueTagVO> antiFraudOverdueTags() {
    // 1. 构建 code -> VO 映射
    Map<AntiFraudOverdueTagEnum, AntiFraudOverdueTagVO> codeToVO = new HashMap<>();
    for (AntiFraudOverdueTagEnum tagEnum : AntiFraudOverdueTagEnum.values()) {
      AntiFraudOverdueTagVO vo = new AntiFraudOverdueTagVO();
      vo.setTagCode(tagEnum.getTagCode());
      vo.setTagName(tagEnum.getTagName());
      vo.setChildrenTagList(Lists.newArrayList());
      codeToVO.put(tagEnum, vo);
    }

    // 2. 构建树结构
    List<AntiFraudOverdueTagVO> rootList = Lists.newArrayList();
    for (AntiFraudOverdueTagEnum tagEnum : AntiFraudOverdueTagEnum.values()) {
      AntiFraudOverdueTagEnum parentCode = tagEnum.getParentCode();
      if (parentCode == null) {
        // 根节点
        rootList.add(codeToVO.get(tagEnum));
      } else {
        // 子节点加入父节点的 children
        AntiFraudOverdueTagVO parentVO = codeToVO.get(parentCode);
        if (parentVO != null) {
          parentVO.getChildrenTagList().add(codeToVO.get(tagEnum));
        }
      }
    }

    return rootList;
  }

  public Boolean antiFraudOverdueSubmit(Long caseId, List<AntiFraudOverdueTagEnum> overdueTagCodeList, String remark, String reviewerEmail) {
    return threadTransactionalModel.transactionResult(configuration -> {
      AntiFraudOverdueCaseRecord caseRecord = antiFraudOverdueCaseModel.findByIdOrThrow(caseId);
      AntiFraudOverdueReviewLogRecord antiFraudOverdueReviewLogRecord = antiFraudOverdueReviewLogModel.insert(caseId, reviewerEmail, overdueTagCodeList, remark);
      antiFraudOverdueCaseModel.updateStatus(caseRecord, AntiFraudOverdueCaseStatus.REVIEWED);
      if (Objects.nonNull(antiFraudOverdueReviewLogRecord)) {
        antiFraudOverdueMonitorService.logTagSubmitted();
      }
      return Objects.nonNull(antiFraudOverdueReviewLogRecord);
    });
  }

  public Integer getAntiFraudOverdueListCount(List<Long> loanAccountIds,
                                              String ktpAccount,
                                              AntiFraudOverdueCaseStatus reviewStatus,
                                              Long caseUpdateTimeStart,
                                              Long caseUpdateTimeEnd) {
    List<Long> loanAccountIdList = getAccountIdListByKtpAccount(loanAccountIds, ktpAccount);
    return antiFraudOverdueCaseModel.findCountByPageCondition(caseUpdateTimeStart, caseUpdateTimeEnd, reviewStatus, loanAccountIdList);
  }

  private List<Long> getAccountIdListByKtpAccount(List<Long> loanAccountIds, String ktpAccount) {
    if (CollectionUtils.isEmpty(loanAccountIds) && StringUtils.isEmpty(ktpAccount)) {
      return Lists.newArrayList();
    }

    // 从 ktpAccount 查询得到的 accountId 列表
    List<Long> ktpAccountIds = Lists.newArrayList();
    if (!StringUtils.isEmpty(ktpAccount)) {
      List<LoanUserAdditionalInfoRecord> loanUserAdditionalInfoRecords = loanUserAdditionalInfoModel.findByTypeAndValue(LoanUserAdditionalInfoType.IDN_ID_NO, ktpAccount);
      if (CollectionUtils.isEmpty(loanUserAdditionalInfoRecords)) {
        // 没查到记录直接返回 [0L]
        return Collections.singletonList(0L);
      }
      ktpAccountIds = loanUserAdditionalInfoRecords.stream().map(LoanUserAdditionalInfoRecord::getLoanAccountId).collect(Collectors.toList());
    }

    List<Long> resultList = Lists.newArrayList();
    // 两个都非空，取交集
    if (!CollectionUtils.isEmpty(loanAccountIds) && !CollectionUtils.isEmpty(ktpAccountIds)) {
      resultList = ktpAccountIds.stream()
          .filter(loanAccountIds::contains)
          .collect(Collectors.toList());
    }else if (!CollectionUtils.isEmpty(loanAccountIds)) {
      resultList = loanAccountIds;
    }else {
      resultList = ktpAccountIds;
    }

    return resultList.isEmpty() ? Collections.singletonList(0L) : resultList;
  }

  public List<AntiFraudOverdueRecordVO> antiFraudOverdueListPage(Integer pageNo,
                                                                 Integer pageSize,
                                                                 List<Long> loanAccountIds,
                                                                 String ktpAccount,
                                                                 AntiFraudOverdueCaseStatus reviewStatus,
                                                                 Long caseUpdateTimeStart,
                                                                 Long caseUpdateTimeEnd) {
    checkOptions(pageNo, pageSize, caseUpdateTimeStart, caseUpdateTimeEnd);
    return batchFetchOverdueListByPage(pageNo, pageSize, loanAccountIds, ktpAccount, reviewStatus, caseUpdateTimeStart, caseUpdateTimeEnd);
  }

  public List<AntiFraudOverdueRecordVO> batchFetchOverdueListByPage(Integer pageNo,
                                                                 Integer pageSize,
                                                                 List<Long> loanAccountIds,
                                                                 String ktpAccount,
                                                                 AntiFraudOverdueCaseStatus reviewStatus,
                                                                 Long caseUpdateTimeStart,
                                                                 Long caseUpdateTimeEnd) {
    Integer offset = null;
    Integer limit = null;
    if (pageNo != null && pageSize != null) {
      offset = pageSize * (pageNo - 1);
      limit = pageSize;
    }

    List<Long> loanAccountIdList = getAccountIdListByKtpAccount(loanAccountIds, ktpAccount);
    List<AntiFraudOverdueCaseRecord> overdueCaseRecords = antiFraudOverdueCaseModel.findByPageCondition(caseUpdateTimeStart, caseUpdateTimeEnd, reviewStatus, loanAccountIdList, offset, limit);
    if (CollectionUtils.isEmpty(overdueCaseRecords)) {
      return Lists.newArrayList();
    }

    return batchQueryUserOverdueRecord(overdueCaseRecords);
  }

  private List<AntiFraudOverdueRecordVO> batchQueryUserOverdueRecord(List<AntiFraudOverdueCaseRecord> overdueCaseRecords) {
    List<Long> accountIdList = overdueCaseRecords.stream().map(AntiFraudOverdueCaseRecord::getLoanAccountId).collect(Collectors.toList());
    Map<Long, AntiFraudOverdueCaseRecord> overdueCaseRecordMap = overdueCaseRecords.stream()
        .collect(Collectors.toMap(AntiFraudOverdueCaseRecord::getLoanAccountId, Function.identity()));

    // 查询ktp数据
    List<LoanUserAdditionalInfoVO> ktpInfos = loanUserAdditionalInfoModel.getInfosByAccountsAndTypes(accountIdList, Collections.singleton(LoanUserAdditionalInfoType.IDN_ID_NO));
    Map<Long, LoanUserAdditionalInfoVO> ktpsMap = ktpInfos.stream()
        .collect(Collectors.toMap(LoanUserAdditionalInfoVO::getLoanAccountId, Function.identity()));

    // 批量查询account表获取最大逾期天数
    List<SimpleLoanAccountVO> simpleLoanAccountVOS = loanAccountService.getSimpleAccounts(accountIdList);
    Map<Long, SimpleLoanAccountVO> accountMap = simpleLoanAccountVOS.stream()
        .collect(Collectors.toMap(account -> account.id, Function.identity()));

    // 批量查询instalment表，再通过order表查到最近打款金额和放款金额
    Map<Long, Long> orderIdMapByAccountIds = cashLoanInstalmentModel.findLatestOverdueOrderIdMapByAccountIds(accountIdList);
    List<Long> orderIds = new ArrayList<>(orderIdMapByAccountIds.values());
    List<CashLoanOrderVO> orderVOList = ecOrderService.getOrderVOList(orderIds);
    Map<Long, CashLoanOrderVO> orderMap = orderVOList.stream()
        .collect(Collectors.toMap(order -> order.accountId, Function.identity()));

    List<AntiFraudOverdueRecordVO> result = Lists.newArrayList();
    for (Long accountId : accountIdList) {
      try {
        AntiFraudOverdueCaseRecord antiFraudOverdueCaseRecord = overdueCaseRecordMap.get(accountId);
        SimpleLoanAccountVO simpleLoanAccountVO = accountMap.get(accountId);
        CashLoanOrderVO cashLoanOrderVO = orderMap.get(accountId);
        LoanUserAdditionalInfoVO ktpInfo = ktpsMap.get(accountId);

        AntiFraudOverdueRecordVO antiFraudOverdueRecordVO = new AntiFraudOverdueRecordVO();
        antiFraudOverdueRecordVO.setCaseId(antiFraudOverdueCaseRecord.getId());
        antiFraudOverdueRecordVO.setUserId(simpleLoanAccountVO.userId);
        antiFraudOverdueRecordVO.setLoanAccountId(simpleLoanAccountVO.id);
        // ktp信息
        antiFraudOverdueRecordVO.setKtpAccount(ktpInfo.value);
        // 最近一次打款时间
        antiFraudOverdueRecordVO.setLastLoanTime(cashLoanOrderVO.timePayout);
        // 最近一次申请风控时间
        antiFraudOverdueRecordVO.setLastApplyTime(cashLoanOrderVO.timeCreated);
        // 最大逾期天数
        antiFraudOverdueRecordVO.setMaxOverdueDays(simpleLoanAccountVO.maxOverdueDays);
        // 风控类型
        antiFraudOverdueRecordVO.setRiskControlScene(cashLoanOrderVO.orderSeq > 1 ? "复贷" : "首贷");
        // 放款金额
        antiFraudOverdueRecordVO.setLoanAmount(cashLoanOrderVO.principal);
        // 剩余还款金额
        antiFraudOverdueRecordVO.setRemainingRepayment(cashLoanOrderVO.calculateOutstandingAmount());
        // 审核更新时间
        antiFraudOverdueRecordVO.setReviewCaseUpdateTime(antiFraudOverdueCaseRecord.getTimeUpdated());
        // 审核状态
        antiFraudOverdueRecordVO.setAuditStatus(AntiFraudOverdueCaseStatus.valueOf(antiFraudOverdueCaseRecord.getReviewStatus()));
        result.add(antiFraudOverdueRecordVO);
      }catch (Exception e){
        log.error("batch query anti fraud overdue record error, loanAccountId = {}, e=", accountId, e);
      }
    }

    return result;
  }

  public void asyncAntiFraudOverdueExport(List<Long> loanAccountIds,
                                          String ktpAccount,
                                          AntiFraudOverdueCaseStatus reviewStatus,
                                          Long caseUpdateTimeStart,
                                          Long caseUpdateTimeEnd,
                                          String adminUserEmail) {
    antiFarudExportExecutor.execute(() -> {
      int pageNo = 1;
      int pageSize = antiFraudConfigService.getExportPageSize();
      genResultFileAndSave(pageSize, pageNo, adminUserEmail, loanAccountIds, ktpAccount, reviewStatus, caseUpdateTimeStart, caseUpdateTimeEnd);
    });
  }

  private void genResultFileAndSave(Integer pageSize,
                                    Integer pageNo,
                                    String email,
                                    List<Long> loanAccountIds,
                                    String ktpAccount,
                                    AntiFraudOverdueCaseStatus reviewStatus,
                                    Long caseUpdateTimeStart,
                                    Long caseUpdateTimeEnd) {
    try {
      AntiFraudOverdueExportCondition condition = AntiFraudOverdueExportCondition.builder()
          .pageNo(pageNo)
          .pageSize(pageSize)
          .reviewStatus(reviewStatus)
          .ktpAccount(ktpAccount)
          .loanAccountIds(loanAccountIds)
          .caseUpdateTimeEnd(caseUpdateTimeEnd)
          .caseUpdateTimeStart(caseUpdateTimeStart)
          .build();
      String excelFileName = MessageFormat.format(EXCEL_NAME, email + Clock.now());
      File targetFile = EasyExcelUtils.exportSingleSheet(this::queryData, condition, AntiFraudOverdueRecordExcelVO.class, excelFileName, "Sheet");
      sendEmail(targetFile, Collections.singletonList(email));
    } catch (Exception e) {
      log.error("anti fraud overdue genResultFileAndSave is error, e=", e);
    }
  }

  public List<AntiFraudOverdueRecordExcelVO> queryData(MultiQueryCondition<AntiFraudOverdueRecordExcelVO> condition) {
    AntiFraudOverdueExportCondition logCondition = (AntiFraudOverdueExportCondition) condition;
    List<AntiFraudOverdueRecordVO> antiFraudOverdueRecordVOS = batchFetchOverdueListByPage(logCondition.getPageNo(),
        logCondition.getPageSize(),
        logCondition.getLoanAccountIds(),
        logCondition.getKtpAccount(),
        logCondition.getReviewStatus(),
        logCondition.getCaseUpdateTimeStart(),
        logCondition.getCaseUpdateTimeEnd());

    if (CollectionUtils.isEmpty(antiFraudOverdueRecordVOS)) {
      return Lists.newArrayList();
    }
    List<AntiFraudOverdueRecordExcelVO> result = Lists.newArrayList();
    for (AntiFraudOverdueRecordVO antiFraudOverdueRecordVO : antiFraudOverdueRecordVOS) {
      result.add(AntiFraudOverdueRecordExcelVO.from(antiFraudOverdueRecordVO));
    }
    return result;
  }

  private void sendEmail(File file, List<String> emails) {
    if (org.apache.commons.collections.CollectionUtils.isEmpty(emails)) {
      return;
    }
    Map<String, File> accessoryMap = new HashMap<>();
    String title = "anti_fraud_platform";
    accessoryMap.put(file.getName(), file);
    emailService.sendWithAccessory(emails, title, title, null, accessoryMap);
  }

  private void checkOptions(Integer pageNo, Integer pageSize, Long caseUpdateTimeStart, Long caseUpdateTimeEnd) {
    if (Objects.isNull(pageNo) || Objects.isNull(pageSize)) {
      throw EcException.warn(COMMON_ILLEGAL_PARAM_TOAST, TT.gen("翻页数据不能为空"));
    }
    if (caseUpdateTimeStart != null && caseUpdateTimeEnd != null) {
      long oneMonthMillis = 31L * 24 * 60 * 60 * 1000;
      if (caseUpdateTimeEnd - caseUpdateTimeStart > oneMonthMillis) {
        throw EcException.warn(COMMON_ILLEGAL_PARAM_TOAST, TT.gen("时间范围不能超过一个月"));
      }
    }

  }

}
