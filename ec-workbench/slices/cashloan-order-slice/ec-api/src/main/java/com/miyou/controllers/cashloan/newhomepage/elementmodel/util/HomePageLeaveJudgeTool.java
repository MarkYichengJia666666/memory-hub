package com.miyou.controllers.cashloan.newhomepage.elementmodel.util;

import com.google.common.collect.ImmutableSet;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.userpage.AutoJumpPageEnum;
import com.yqg.core.service.cashloan.homepage.JumpBillPageTool;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 判断本次 home 响应是否会指示客户端自动跳出首页，用于「一次性消费类」展示（新获额角标、还款升额弹层等）
 * 下发前的前置校验：本次会跳走则跳过消费，留给下次真正停留在首页的请求再展示。
 *
 * <p>自动跳目标页只有 {@link AutoJumpPageEnum} 三个值，客户端跳转后统一回调
 * {@code POST /api/cashloan/reportAutoJumpLevel2} 上报并消费各自频控，三者语义对等，故一视同仁：
 *
 * <table border="1">
 *   <tr><th>目标页</th><th>响应字段</th><th>写入方</th><th>挂载 status（scope）</th></tr>
 *   <tr><td>{@code ORDER}</td><td>{@code canOrderPageResponse.jumpLevel2}</td>
 *       <td>{@code UserInfoCanOrderProcessor}</td><td>{@code canCreateOrder()}</td></tr>
 *   <tr><td>{@code BILL_PAGE}</td><td>{@code pageUserInfoV3.userPage.autoJumpPage}</td>
 *       <td>{@code BillPageJumpV3Processor}</td><td>{@link #BILL_PAGE_JUMP_STATUSES}</td></tr>
 *   <tr><td>{@code AUTHENTICATION}</td><td>{@code pageUserInfoV3.userPage.needOpenAuthentication}</td>
 *       <td>{@code PageJumpV3Processor}</td><td>{@code NEVER_APPLIED}</td></tr>
 * </table>
 *
 * <p>每项判据都<b>先判 scope 再判条件</b>：{@code hitJumpLevel2Strategy()} / {@link JumpBillPageTool} /
 * {@code isNeedOpenAuthentication} 只回答「有没有能力跳」，只有当前 status 真的挂载了写该信号的
 * Processor，本次响应才会真的跳走。反过来会误伤——线上曾因 {@code RELOAN_INIT} 未挂账单跳 Processor
 * 却被判会跳，导致大卡角标与文案消失。
 */
@Slf4j
@Component
public class HomePageLeaveJudgeTool {

  /**
   * 加挂 {@code AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE} 且会渲染一次性展示的结果态。可下单结果态基类
   * {@code AbstractCanOrderProcessorSelector} 默认不挂，仅 {@code MultiLoanCreditsAcceptedSelector}
   * 覆写加挂，故当前只有一个成员；逾期分支只挂在不渲染一次性展示的还款态，未纳入。
   */
  private static final Set<IDNHomepageLoanStatusV5> BILL_PAGE_JUMP_STATUSES =
      ImmutableSet.of(IDNHomepageLoanStatusV5.MULTI_LOAN_CREDITS_ACCEPTED);

  @Autowired
  private JumpBillPageTool jumpBillPageTool;

  /**
   * 任何异常 fail-open 返回 false（不阻断既有消费主流程，退回改动前行为）。
   */
  public boolean willLeaveHomePage(HomePageContext homePageContext) {
    try {
      AutoJumpPageEnum targetPage = resolveAutoJumpTarget(homePageContext);
      if (targetPage == null) {
        return false;
      }
      log.debug("[HomePageLeaveJudgeTool] will leave home page, skip one-time display, userId={}, targetPage={}",
          homePageContext.getUserId(), targetPage);
      return true;
    } catch (Exception e) {
      log.warn("[HomePageLeaveJudgeTool] willLeaveHomePage failed, fail-open to false, userId={}",
          homePageContext == null ? null : homePageContext.getUserId(), e);
      return false;
    }
  }

  /**
   * @return 本次响应会指示跳转的目标页；不会自动跳走时返回 {@code null}
   */
  @Nullable
  private AutoJumpPageEnum resolveAutoJumpTarget(HomePageContext homePageContext) {
    if (willJumpOrderPage(homePageContext)) {
      return AutoJumpPageEnum.ORDER;
    }
    if (willJumpBillPage(homePageContext)) {
      return AutoJumpPageEnum.BILL_PAGE;
    }
    if (willJumpAuthPage(homePageContext)) {
      return AutoJumpPageEnum.AUTHENTICATION;
    }
    return null;
  }

  private boolean willJumpOrderPage(HomePageContext homePageContext) {
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();
    return status != null && status.canCreateOrder() && homePageContext.hitJumpLevel2Strategy();
  }

  private boolean willJumpBillPage(HomePageContext homePageContext) {
    return BILL_PAGE_JUMP_STATUSES.contains(homePageContext.getStatus())
        && jumpBillPageTool.canJumpBillPageForNotOverdue(homePageContext.getUserId());
  }

  /**
   * {@code NEVER_APPLIED} 当前不渲染一次性展示，此判据实际不会命中；保留是为口径完整——一旦后续有
   * 一次性展示下沉到未完件态，无需再补判据。scope 短路也避免了登录态查询在无关状态上产生开销。
   */
  private boolean willJumpAuthPage(HomePageContext homePageContext) {
    return homePageContext.getStatus() == IDNHomepageLoanStatusV5.NEVER_APPLIED
        && BooleanUtils.isTrue(homePageContext.isNeedOpenAuthentication(homePageContext));
  }
}
