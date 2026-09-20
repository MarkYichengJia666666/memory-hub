package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.model.generated.tables.records.ApiChannelUserRecord;
import com.yqg.core.model.sql.apichannel.ApiChannelGopayCreditApplicationModel;
import com.yqg.core.model.sql.apichannel.ApiChannelLazadaCreditApplicationModel;
import com.yqg.core.model.sql.apichannel.ApiChannelUserModel;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: ChannelCreditApplicationService
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 查询 gopay/lazada credit application 表最新通过记录的 time_updated，供 credit_period 决策节点使用
 */
@Service
@Slf4j
public class ChannelCreditApplicationService {

    @Autowired
    private ApiChannelGopayCreditApplicationModel gopayCreditApplicationModel;

    @Autowired
    private ApiChannelLazadaCreditApplicationModel lazadaCreditApplicationModel;

    @Autowired
    private ApiChannelUserModel apiChannelUserModel;

    /**
     * 查询 Gopay credit application 中指定用户最新 APPROVED 记录的 time_updated
     *
     * @param userId 内部用户 ID
     * @return time_updated 毫秒时间戳；无记录或无渠道绑定时返回 null
     */
    public Long getLatestGopayAcceptedTimeUpdated(Long userId) {
        ApiChannelUserRecord gopayUser = apiChannelUserModel.findByUserIdAndChannel(userId, ApiChannel.GOPAY);
        if (gopayUser == null) {
            return null;
        }
        return gopayCreditApplicationModel.getLatestApprovedTimeUpdated(gopayUser.getThirdPartyUserId());
    }

    /**
     * 查询 Lazada credit application 中指定用户最新 APPROVED 记录的 time_updated
     *
     * @param userId 内部用户 ID
     * @return time_updated 毫秒时间戳；无记录或无渠道绑定时返回 null
     */
    public Long getLatestLazadaAcceptedTimeUpdated(Long userId) {
        ApiChannelUserRecord lazadaUser = apiChannelUserModel.findByUserIdAndChannel(userId, ApiChannel.LAZADA_BUYER);
        if (lazadaUser == null) {
            return null;
        }
        return lazadaCreditApplicationModel.getLatestApprovedTimeUpdated(lazadaUser.getThirdPartyUserId());
    }
}
