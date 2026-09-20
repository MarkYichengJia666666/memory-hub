package com.yqg.core.util.thirdparty.loan.antifraud;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.overseasrisk.client.mesh.api.IRiskUserInfoService;
import com.yqg.overseasrisk.common.mvc.userinfo.LoanAntiFraudType;
import com.yqg.overseasrisk.common.mvc.userinfo.RelationUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by ZiP on 2020/6/1.
 */
@Service
@Slf4j
public class LoanAntiFraudService {
  private static final String TYPE_GEOHASH = "geohash";
  private static final String TYPE_WIFI = "wifi";

  @Autowired
  private IRiskUserInfoService riskUserInfoService;
  @Autowired
  private LoanUserCreditsInfoModel loanUserCreditsInfoModel;

  public LoanAntiFraudResponse getLoanAntiFraudResponse(LoanAntiFraudType type, String checkStr, Long startTime, Long endTime) {
    if (StringUtils.isBlank(checkStr)) {
      return generateErrorResponse("Params error, checkStr can not be null!");
    }
    if (StringUtils.equals(TYPE_GEOHASH, type.model)) {
      return getGeoResponse(type, checkStr, startTime, endTime);
    } else if (StringUtils.equals(TYPE_WIFI, type.model)) {
      return getWifiResponse(type, checkStr, startTime, endTime);
    } else {
      return generateErrorResponse("Params error, type not found! type = " + type.model);
    }
  }

  private LoanAntiFraudResponse getGeoResponse(LoanAntiFraudType type, String checkStr, Long startTime, Long endTime) {
    try {
      List<RelationUserResponse> records = riskUserInfoService.fetchByGeohashAndTimeUpdated(type, checkStr, startTime, endTime);
      List<Long> userIds = records.stream()
          .sorted(Comparator.comparingLong(RelationUserResponse::getDataTime).reversed())
          .filter(r -> isFirstLoanData(r.getUserId(), r.getDataTime()))
          .map(RelationUserResponse::getUserId)
          .distinct()
          .collect(Collectors.toList());
      return generateResponse(userIds);
    } catch (Exception e) {
      return generateErrorResponse(e.getMessage());
    }
  }

  private LoanAntiFraudResponse getWifiResponse(LoanAntiFraudType type, String checkStr, Long startTime, Long endTime) {
    List<RelationUserResponse> records = riskUserInfoService.fetchByBssidAndTimeUpdated(checkStr, startTime, endTime);
    List<Long> userIds = records.stream()
        .filter(r -> isFirstLoanData(r.getUserId(), r.getDataTime()))
        .map(RelationUserResponse::getUserId)
        .distinct()
        .collect(Collectors.toList());
    return generateResponse(userIds);
  }

  private boolean isFirstLoanData(Long userId, Long timeCreated) {
    LoanUserCreditsInfoRecord record = loanUserCreditsInfoModel.findByUserId(userId);
    return record == null || record.getTimeAccepted() == null || timeCreated < record.getTimeAccepted();
  }

  private LoanAntiFraudResponse generateResponse(List<Long> userIds) {
    LoanAntiFraudResponse response = new LoanAntiFraudResponse();
    if (CollectionUtils.isEmpty(userIds)) {
      response.status = LoanAntiFraudResponseStatus.NOT_FOUND;
      return response;
    }
    response.status = LoanAntiFraudResponseStatus.FOUND;
    response.userId = userIds;
    return response;
  }

  private LoanAntiFraudResponse generateErrorResponse(String msg) {
    LoanAntiFraudResponse response = new LoanAntiFraudResponse();
    response.status = LoanAntiFraudResponseStatus.ERROR;
    response.msg = msg;
    return response;
  }
}
