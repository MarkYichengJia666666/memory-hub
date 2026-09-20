package com.miyou.controllers.admin.loan.coupon;

import com.miyou.controllers.admin.loan.coupon.response.LoanCouponGrantTaskListResponse;
import com.miyou.controllers.admin.loan.coupon.response.LoanCouponGrantTaskResponse;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.utilities.TableResp;
import com.miyou.utilities.TableResponseTitleHelper;
import com.yqg.core.model.sql.notif.enums.NotifCouponGrantTaskStatus;
import com.yqg.core.service.coupongrantrule.CouponGrantTaskLogService;
import com.yqg.core.service.coupongrantrule.enums.CouponGrantRulePlatformType;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantTaskLogVO;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskCondition;
import com.yqg.core.service.notif.NotifCouponGrantTaskService;
import com.yqg.ec.common.enums.NotifCouponGrantTaskSourceType;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskVO;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author fudongyi
 * @date 2022/8/16
 */
@RestController
@RequestMapping("/admin/operation/loan/coupon/task")
public class LoanCouponGrantTaskController extends YqgBaseController {
  @Autowired
  private NotifCouponGrantTaskService notifCouponGrantTaskService;
  @Autowired
  private CouponGrantTaskLogService couponGrantTaskLogService;
  @Autowired
  private TableResponseTitleHelper tableResponseTitleHelper;

  @PreAuthorize("hasAnyAuthority('LOAN.COUPON.QUERY')")
  @GetMapping("/list")
  public Result listCouponGrantTask(
      @RequestParam("pageNo") Integer pageNo,
      @RequestParam("pageSize") Integer pageSize,
      @RequestParam(value = "ruleConfigId", required = false) Long ruleConfigId,
      @RequestParam(value = "status", required = false) NotifCouponGrantTaskStatus status,
      @RequestParam(value = "userId", required = false) Long userId,
      @RequestParam(value = "sourceType", required = false) NotifCouponGrantTaskSourceType sourceType,
      @RequestParam(value = "batchNo", required = false) String batchNo
  ) {
    NotifCouponGrantTaskCondition condition = getCondition(ruleConfigId, status, userId, sourceType, batchNo);
    List<NotifCouponGrantTaskVO> taskVOList = notifCouponGrantTaskService.listOrderedByPageSizeAndPageNoAndCondition(condition, pageSize, pageNo);
    Long total = notifCouponGrantTaskService.getCountByCondition(condition);

    List<Long> taskIds = taskVOList.stream().map(e -> e.id).collect(Collectors.toList());
    Map<Long, List<CouponGrantTaskLogVO>> listMap = couponGrantTaskLogService.mapByTaskIds(taskIds);

    TableResp<LoanCouponGrantTaskResponse> respList = new TableResp<>();
    if (CollectionUtils.isEmpty(taskVOList)) {
      respList.tableTitle = tableResponseTitleHelper.genTitle(LoanCouponGrantTaskResponse.class, null);
    } else {
      taskVOList.forEach(taskVO -> respList.add(LoanCouponGrantTaskResponse.from(taskVO, listMap.get(taskVO.id))));
    }

    return EcResponseUtil.generate(LoanCouponGrantTaskListResponse.from(respList, total));
  }

  private NotifCouponGrantTaskCondition getCondition(
      Long ruleConfigId,
      NotifCouponGrantTaskStatus status,
      Long userId,
      NotifCouponGrantTaskSourceType sourceType,
      String batchNo
  ) {
    return NotifCouponGrantTaskCondition.builder()
        .ruleConfigIds(Objects.isNull(ruleConfigId) ? null : Collections.singletonList(ruleConfigId))
        .userIds(Objects.isNull(userId) ? null : Collections.singletonList(userId))
        .status(status)
        .sourceType(sourceType)
        .platformType(CouponGrantRulePlatformType.LOAN)
        .batchNo(batchNo)
        .build();
  }
}
