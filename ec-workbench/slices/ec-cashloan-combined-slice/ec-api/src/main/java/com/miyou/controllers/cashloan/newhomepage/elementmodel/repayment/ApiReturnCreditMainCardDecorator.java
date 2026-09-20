package com.miyou.controllers.cashloan.newhomepage.elementmodel.repayment;

import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.GREEN;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextStyle.BOLD;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditConfig;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditEligibilitySnapshot;
import com.yqg.translation.client.utils.TT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * API 回端额度主卡片纯展示装饰器。
 */
@Component
public class ApiReturnCreditMainCardDecorator {

  /**
   * 补件态副标题 TT 源值。印尼语译文须以 {@link #NEED_SUPPLEMENT_BOLD_PLACEHOLDER} 作为加粗段占位符，例如：
   * {@code "Sistem mendeteksi Anda berpeluang naik limit! APIBACK_SUPPLEMENT"}。
   */
  public static final String SUB_TITLE_NEED_SUPPLEMENT = "系统检测到你有提额空间！补充资料领取新额度";

  /**
   * 待还一期副标题 TT 源值。印尼语译文须以 {@link #WAIT_REPAY_BOLD_PLACEHOLDER} 作为加粗段占位符，例如：
   * {@code "Data berhasil dikirim! APIBACK_REPAY untuk memperbarui limit pinjaman Anda."}。
   */
  public static final String SUB_TITLE_WAIT_FIRST_REPAYMENT = "资料提交成功！还款1期即可刷新可借额度";

  /** 补件态副标题加粗段在 TT 译文中的占位符 key（供 highlightMap 使用）。 */
  public static final String NEED_SUPPLEMENT_BOLD_PLACEHOLDER = "APIBACK_SUPPLEMENT";

  /**
   * 补件态加粗段印尼语展示文案。
   *
   * @mock Lengkapi data untuk dapatkan limit baru.
   */
  public static final String NEED_SUPPLEMENT_BOLD_DISPLAY = "Lengkapi data untuk dapatkan limit baru.";

  /** 待还一期副标题加粗段在 TT 译文中的占位符 key（供 highlightMap 使用）。 */
  public static final String WAIT_REPAY_BOLD_PLACEHOLDER = "APIBACK_REPAY";

  /**
   * 待还一期加粗段印尼语展示文案。
   *
   * @mock Bayar 1x cicilan
   */
  public static final String WAIT_REPAY_BOLD_DISPLAY = "Bayar 1x cicilan";

  private final ApiReturnCreditConfig config;

  @Autowired
  public ApiReturnCreditMainCardDecorator(ApiReturnCreditConfig config) {
    this.config = config;
  }

  /**
   * 替换补件态的副标题与主按钮；现网缺失时主动补齐，避免进度条等场景丢文案或按钮。
   *
   * @param originalElements 现网主卡片元素
   * @param snapshot 资格快照
   * @return 装饰后的主卡片元素
   */
  public List<IElement> decorate(
      List<IElement> originalElements, ApiReturnCreditEligibilitySnapshot snapshot) {
    if (originalElements == null) {
      return Collections.emptyList();
    }
    if (snapshot == null) {
      return originalElements;
    }
    if (snapshot.getCardMode()
        == ApiReturnCreditEligibilitySnapshot.CardMode.WAIT_FIRST_REPAYMENT) {
      return decorateWaitFirstRepayment(originalElements);
    }
    if (snapshot.getCardMode()
        != ApiReturnCreditEligibilitySnapshot.CardMode.NEED_SUPPLEMENT) {
      return originalElements;
    }
    return decorateWithOverride(
        originalElements, buildSupplementSubTitle(), buildSupplementButton());
  }

  /**
   * 待还一期模式替换/补齐副标题和主按钮；订单页 buttonUrl 取自独立配置。
   */
  public List<IElement> decorateWaitFirstRepayment(List<IElement> originalElements) {
    if (originalElements == null) {
      return Collections.emptyList();
    }
    return decorateWithOverride(
        originalElements, buildWaitFirstRepaymentSubTitle(), buildWaitFirstRepaymentButton());
  }

  /**
   * 替换已有 SUB_TITLE / MAIN_BUTTON；缺失时主动补齐（副标题插在主按钮前，主按钮追加到末尾）。
   */
  private List<IElement> decorateWithOverride(
      List<IElement> originalElements, IElement subTitle, IElement mainButton) {
    List<IElement> decoratedElements = new ArrayList<>(originalElements.size() + 2);
    boolean hasSubTitle = false;
    boolean hasMainButton = false;
    for (IElement element : originalElements) {
      if (element.getId() == MainCardElementId.SUB_TITLE) {
        decoratedElements.add(subTitle);
        hasSubTitle = true;
      } else if (element.getId() == MainCardElementId.MAIN_BUTTON) {
        decoratedElements.add(mainButton);
        hasMainButton = true;
      } else {
        decoratedElements.add(element);
      }
    }
    if (!hasSubTitle) {
      insertSubTitleBeforeMainButton(decoratedElements, subTitle);
    }
    if (!hasMainButton) {
      decoratedElements.add(mainButton);
    }
    return decoratedElements;
  }

  private void insertSubTitleBeforeMainButton(List<IElement> elements, IElement subTitle) {
    for (int i = 0; i < elements.size(); i++) {
      if (elements.get(i).getId() == MainCardElementId.MAIN_BUTTON) {
        elements.add(i, subTitle);
        return;
      }
    }
    elements.add(subTitle);
  }

  private IElement buildSupplementSubTitle() {
    return ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(TT.gen(SUB_TITLE_NEED_SUPPLEMENT))
        .elementParam(TextElement.TextParam.builder()
            .highlightMap(ImmutableMap.of(NEED_SUPPLEMENT_BOLD_PLACEHOLDER, NEED_SUPPLEMENT_BOLD_DISPLAY))
            .highlightStyle(BOLD)
            .highlightColor(GREEN)
            .build())
        .build();
  }

  private IElement buildSupplementButton() {
    PopUpAction.PopUpParam popUpParam = PopUpAction.PopUpParam.builder()
        .redirectUrl(config.getSupplementUrl())
        .build();
    return ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("现在补齐"))
        .action(new PopUpAction(popUpParam))
        .elementParam(ButtonElement.ButtonParam.builder()
            .colorType(ButtonElement.ButtonColor.GREEN)
            .build())
        .build();
  }

  private IElement buildWaitFirstRepaymentSubTitle() {
    return ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(TT.gen(SUB_TITLE_WAIT_FIRST_REPAYMENT))
        .elementParam(TextElement.TextParam.builder()
            .highlightMap(ImmutableMap.of(WAIT_REPAY_BOLD_PLACEHOLDER, WAIT_REPAY_BOLD_DISPLAY))
            .highlightStyle(BOLD)
            .highlightColor(GREEN)
            .build())
        .build();
  }

  private IElement buildWaitFirstRepaymentButton() {
    PopUpAction.PopUpParam popUpParam = PopUpAction.PopUpParam.builder()
        .redirectUrl(config.getRepaymentUrl())
        .title(TT.gen("温馨提醒"))
        .content(TT.gen("您已补充资料成功，还款1期即可刷新可借额度"))
        .buttonContent(TT.gen("去还款"))
        .buttonUrl(config.getRepaymentButtonUrl())
        .build();
    return ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("立即申请"))
        .action(new PopUpAction(popUpParam))
        .elementParam(ButtonElement.ButtonParam.builder()
            .colorType(ButtonElement.ButtonColor.GREEN)
            .build())
        .build();
  }
}
