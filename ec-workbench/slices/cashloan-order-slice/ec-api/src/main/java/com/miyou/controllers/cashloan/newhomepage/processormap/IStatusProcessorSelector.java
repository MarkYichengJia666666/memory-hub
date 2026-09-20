package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;

/**
 * 状态处理器选择器
 * IDNHomepageLoanStatusV5 与 processor绑定关系
 */
public interface IStatusProcessorSelector {
    /**
     * 首页状态
     *
     * @return
     */
    IDNHomepageLoanStatusV5 getStatus();

}
