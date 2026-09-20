package com.yqg.ec.common.spring.response.basedata.accountdetail;

import com.yqg.ec.common.spring.response.risk.EnvironmentInfo;
import com.yqg.ec.common.spring.response.risk.TerminalInfo;
import lombok.Data;

@Data
public class BindCardInfo {
    public EnvironmentInfo environmentInfo;
    public TerminalInfo terminalInfo;
    public String relativeId;
}
