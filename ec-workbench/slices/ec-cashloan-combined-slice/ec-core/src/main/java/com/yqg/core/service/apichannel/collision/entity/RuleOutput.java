package com.yqg.core.service.apichannel.collision.entity;

import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleOutput {

    private final boolean hit;
    private final ApiChannelUserType userType;

    private static final RuleOutput MISS = new RuleOutput(false, null);

    public static RuleOutput hit(ApiChannelUserType userType) {
        return new RuleOutput(true, userType);
    }

    public static RuleOutput miss() {
        return MISS;
    }
}
