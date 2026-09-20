package com.miyou.controllers.cashloan.utilities;

import static com.yqg.core.service.cashloan.homepage.middle.MiddleType.CORPORATE_LOAN;
import static com.yqg.core.service.cashloan.homepage.middle.MiddleType.INVITE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.enums.IncreaseCreditsEntranceDisplayLocation;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.middle.MiddleConfigVO;
import com.yqg.core.service.cashloan.homepage.middle.MiddleType;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.loan.invitation.LoanInvitationConfigService;
import com.yqg.core.service.loan.invitation.vos.LoanInvitationConfigVO;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loanpurpose.LoanPurposeDetailService;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HomepageMiddleTool {
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private LoanPurposeDetailService loanPurposeDetailService;
  @Autowired
  private HomepageCommonTool homepageCommonTool;
  @Autowired
  private LoanInvitationConfigService loanInvitationConfigService;

  public MiddleListResponse getMiddle(IDNHomepageLoanStatusV5 status,
                                      LoanApiViewerContext viewerContext,
                                      HomepageUserParamsVO paramsVO) {
    switch (status) {
      case NOT_LOGIN:
      case NEVER_APPLIED:
      case REJECTED:
      case RELOAN_REJECTED:
        return getMiddleWithoutSmeEntrance(viewerContext, paramsVO);
      case IN_REVIEW:
      case RELOAN_IN_REVIEW:
      case CAN_REAPPLY_NOW:
      case RELOAN_CAN_REAPPLY_NOW:
      case INCREASE_REVIEW_NEVER_REAPPLIED:
      case INCREASE_REVIEW_REJECT:
      case CAN_REAPPLY_IN_FUTURE:
      case IMAGE_REVIEW_REJECTED:
      case REUPLOAD_FINISHED:
      case FUND_PAYING:
      case RELOAN_FUND_PAYING:
      case MULTI_LOAN_FUND_PAYING:
      case PAYING:
      case RELOAN_PAYING:
      case MULTI_LOAN_PAYING:
      case MULTI_LOAN_IN_REVIEW:
      case OVERDUE:
      case RELOAN_OVERDUE:
      case RELOAN_CALC_CREDITS_IN_REVIEW:
      case MULTI_LOAN_CALC_CREDITS_IN_REVIEW:
      case FUND_CHECK:
      case GRAB_CHECK:
      case RELOAN_FUND_CHECK:
      case RELOAN_GRAB_CHECK:
      case MULTI_LOAN_FUND_CHECK:
      case MULTI_LOAN_GRAB_CHECK:
      case READY:
      case RELOAN_READY:
      case ACCEPTED_BUT_CAN_NOT_LOAN:
      case MULTI_LOAN_INIT:
      case CALC_CREDITS_EXPIRED:
      case LOAN_CREDITS_EXPIRED:
      case NEED_SUPPLEMENT:
      case WAITING_SUPPLEMENT:
      case FINISH_SUPPLEMENT:
      case ACCEPTED:
      case LOAN_CREDITS_DECREASE:
      case MULTI_LOAN_CREDITS_ACCEPTED:
      case RELOAN_INIT:
      case RELOAN_CREDITS_DECREASE:
      case PAYOUT_FAILED:
        return getMiddleWithSmeAndCreditsUpEntrance(paramsVO, viewerContext, status);
      case DEBT_CHECK:
      case RELOAN_DEBT_CHECK:
      case MULTI_LOAN_DEBT_CHECK:
      case ORDER_PRE_CHECK:
      case RELOAN_ORDER_PRE_CHECK:
      case MULTI_LOAN_ORDER_PRE_CHECK:
      case CANCELLED:
      case MINIMALIST_IN_REVIEW:
      case MINIMALIST_ACCEPT:
        return MiddleListResponse.empty();
      default:
        return MiddleListResponse.empty();
    }
  }

  private MiddleListResponse getMiddleWithoutSmeEntrance(LoanApiViewerContext viewerContext,
                                                         HomepageUserParamsVO paramsVO) {
    String middleMessage = homepageV5Config.getMiddleMessageWithoutSmeEntrance();
    if (paramsVO.newHomePageUI()) {
      String middleMessageForNewHomepage = homepageV5Config.getMiddleMessageWithoutSmeEntranceForNewHomepage();
      if (StringUtils.isNotBlank(middleMessageForNewHomepage)) {
        middleMessage = middleMessageForNewHomepage;
      }
    }
    List<MiddleConfigVO> middleList = JsonUtils.from(middleMessage, new TypeReference<List<MiddleConfigVO>>() {});

    if (CollectionUtils.isEmpty(middleList)) {
      return MiddleListResponse.fromList(middleList);
    }

    middleList = middleList.stream()
        .map(configVO -> handleInviteUrl(configVO, viewerContext.userId))
        .collect(Collectors.toList());

    return MiddleListResponse.fromList(middleList);
  }

  private MiddleListResponse getMiddleWithSmeAndCreditsUpEntrance(HomepageUserParamsVO paramsVO, LoanApiViewerContext viewerContext, IDNHomepageLoanStatusV5 status) {
    String middleMessage = homepageV5Config.getMiddleMessageWithSmeEntrance();
    if (paramsVO.newHomePageUI()) {
      String middleMessageForNewHomepage = homepageV5Config.getMiddleMessageWithSmeEntranceForNewHomepage();
      if (StringUtils.isNotBlank(middleMessageForNewHomepage)) {
        middleMessage = middleMessageForNewHomepage;
      }
    }
    List<MiddleConfigVO> middleList = JsonUtils.from(middleMessage, new TypeReference<List<MiddleConfigVO>>() {});

    if (CollectionUtils.isEmpty(middleList)) {
      return MiddleListResponse.fromList(middleList);
    }

    middleList = middleList.stream()
        .filter(configVO -> filterConfigVO(configVO, paramsVO.getLoanAccountId(), status, viewerContext.userId, viewerContext.build))
        .map(configVO -> handleInviteUrl(configVO, viewerContext.userId))
        .collect(Collectors.toList());

    return MiddleListResponse.fromList(middleList);
  }

  private MiddleConfigVO handleInviteUrl(MiddleConfigVO middleConfigVO, Long userId) {
    if (middleConfigVO.type != INVITE) {
      return middleConfigVO;
    }
    LoanInvitationConfigVO invitationConfigVO = loanInvitationConfigService.getCurrentInvitationConfigVOForStyle(userId, Clock.now());
    if (StringUtils.isNotBlank(invitationConfigVO.url)) {
      middleConfigVO.redirectUrl = invitationConfigVO.url;
    }
    return middleConfigVO;
  }

  private boolean filterConfigVO(MiddleConfigVO configVO, Long loanAccountId, IDNHomepageLoanStatusV5 status, Long userId, Long build) {    // 过滤掉已经填完企业贷或者填完被拒绝的
    if (configVO.type == CORPORATE_LOAN) {
      return !loanPurposeDetailService.isCompletedOrUnAssignable(loanAccountId);
    } else if (configVO.type == MiddleType.CREDITS_UP) {
      return homepageCommonTool.shouldDisplayIncreaseCreditsEntrance(IncreaseCreditsEntranceDisplayLocation.MIDDLE, status, userId, loanAccountId, build);
    }
    return true;
  }

  public List<MiddleConfigVO> filterConfigVoList(List<MiddleConfigVO> middleConfigVOS, Long accountId, IDNHomepageLoanStatusV5 status, Long userId, Long build) {
    if (CollectionUtils.isEmpty(middleConfigVOS)) {
      return Lists.newArrayList();
    }

    return middleConfigVOS.stream()
        .filter(configVO -> filterConfigVO(configVO, accountId, status, userId, build))
        .collect(Collectors.toList());
  }

  public List<MiddleConfigVO> handleInviteUrlToMiddleVo(List<MiddleConfigVO> middleConfigVOS, Long userId) {
    if (CollectionUtils.isEmpty(middleConfigVOS)) {
      return Lists.newArrayList();
    }

    return middleConfigVOS.stream()
        .map(configVO -> handleInviteUrl(configVO, userId))
        .collect(Collectors.toList());
  }
}