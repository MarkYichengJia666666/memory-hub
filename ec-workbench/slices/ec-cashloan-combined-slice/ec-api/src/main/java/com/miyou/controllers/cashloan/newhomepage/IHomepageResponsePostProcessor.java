package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import org.springframework.core.Ordered;

public interface IHomepageResponsePostProcessor extends Ordered {
  void afterProcess(HomepageResponseV5 responseV5, HomePageContext homePageContext);
}
