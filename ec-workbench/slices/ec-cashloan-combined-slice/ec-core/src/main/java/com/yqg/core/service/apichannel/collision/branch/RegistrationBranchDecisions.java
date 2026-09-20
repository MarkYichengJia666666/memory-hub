package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.model.generated.tables.records.LoanUserEncryptInfoRecord;
import com.yqg.core.service.loan.vo.LoanAccountDetailsSimpleVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.user.vo.UserSimpleInfoVO;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: RegistrationBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树注册阶段（6 个节点）：匹配手机号、NIK、纯理财、注销、完件、注册保护期
 */
@Component
public class RegistrationBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.BRANCH_MATCH_MOBILE, this::matchMobile);
        map.put(CollisionProcessNode.BRANCH_DIFFERENT_NIK, this::differentNik);
        map.put(CollisionProcessNode.BRANCH_ONLY_FINANCING, this::onlyFinancing);
        map.put(CollisionProcessNode.BRANCH_IS_DELETED, this::isDeleted);
        map.put(CollisionProcessNode.BRANCH_IS_COMPLETED, this::isCompleted);
        map.put(CollisionProcessNode.BRANCH_REGISTER_PERIOD, this::registerPeriod);
        return map;
    }

    /**
     * match_mobile — 匹配到手机号？
     * 若手机号未匹配但 NIK 回退成功，将 nikFallback 字段应用到主字段后视为"已匹配"
     */
    private boolean matchMobile(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            return true;
        }
        if (!BooleanUtils.isTrue(cond.getEmptyMobileMd5())) {
            return true;
        }
        // 手机号未匹配，尝试应用 NIK 回退
        if (BooleanUtils.isTrue(cond.getMatchedByNik()) && cond.getNikFallbackUserId() != null) {
            cond.setUserId(cond.getNikFallbackUserId());
            cond.setAccountId(cond.getNikFallbackAccountId());
            cond.setEmptyMobileMd5(false);
            return true;
        }
        return false;
    }

    /**
     * different_nik — NIK 不一致？
     * 通过 NIK 回退匹配的场景 NIK 必然一致，跳过此检查
     */
    private boolean differentNik(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            return false;
        }
        if (BooleanUtils.isTrue(cond.getMatchedByNik())) {
            return false;
        }
        if (cond.getIdentityNumberMd5() == null) {
            return false;
        }
        LoanUserEncryptInfoRecord record = cond.getLoanUserEncryptInfoRecord();
        return record.getIdentityNumberMd5() != null
                && !record.getIdentityNumberMd5().equals(cond.getIdentityNumberMd5());
    }

    /**
     * only_financing — 纯理财用户？
     */
    private boolean onlyFinancing(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            return false;
        }
        return cond.getAccountId() == null;
    }

    /**
     * is_deleted — 已注销？
     */
    private boolean isDeleted(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            return false;
        }
        return getUserSimpleInfoVOOrThrow(cond).checkUserDeleted();
    }

    /**
     * is_completed — 已完件？
     */
    private boolean isCompleted(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            return true;
        }
        LoanAccountDetailsSimpleVO details = getDetailsSimpleVOOrNull(cond);
        return details != null && details.timeFinished != null;
    }

    /**
     * register_period — 注册日至判断日间隔 <= N 天？
     */
    private boolean registerPeriod(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        UserSimpleInfoVO userInfo = getUserSimpleInfoVOOrThrow(cond);
        int days = Clock.getDaysBetween(userInfo.timeCreated, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getRegisterProtectionDays(cond.getChannel());
    }
}
