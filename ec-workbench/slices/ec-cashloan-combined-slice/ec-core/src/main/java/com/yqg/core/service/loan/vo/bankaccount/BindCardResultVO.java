package com.yqg.core.service.loan.vo.bankaccount;

import com.yqg.core.service.cashloan.vo.enums.FinalValidationBankAccountStatus;
import com.yqg.core.service.loan.bankaccount.enums.ValidationBankAccountPopupType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.service.cashloan.vo.enums.ValidationCardStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.translation.client.utils.TT;

public class BindCardResultVO {
  public LoanBankAccountVO loanBankAccountVO;
  public TT errRemind;
  public Boolean canModifyName;

  //这三个提示用于AddBankAccountNotMatchStrategy 的 c 分流
  //对于 c 分流，前端不取errRemind字段，只取这三个，所以需要把errRemind回填到这三个字段中
  //由于目前除了验卡姓名不匹配以以外 全是和账户相关的提示，所以回填errRemindForBankAccount字段即可
  public TT errRemindForName;
  public TT errRemindForBankType;
  public TT errRemindForBankAccount;

  public TT errRemindTitleForPopup;
  public TT errRemindContentForPopup;
  public TT popupLeftButtonText;
  public TT popupRightButtonText;
  public ValidationBankAccountPopupType popupType;

  public FinalValidationBankAccountStatus finalValidationBankAccountStatus;
  public ValidationCardStatus validationCardStatus;

  public static BindCardResultVO from(LoanBankAccountVO loanBankAccountVO, TT errRemind) {
    BindCardResultVO bindCardResultVO = from(loanBankAccountVO);
    if (BankAccountAvailableStatus.NEED_CONFIRM_STATUS_LIST.contains(loanBankAccountVO.bankAccountAvailableStatus)) {
      bindCardResultVO.errRemind = errRemind;
    }
    bindCardResultVO.errRemindForBankAccount = bindCardResultVO.errRemind;
    return bindCardResultVO;
  }

  public static BindCardResultVO from(LoanBankAccountVO loanBankAccountVO) {
    BindCardResultVO bindCardResultVO = new BindCardResultVO();
    bindCardResultVO.loanBankAccountVO = loanBankAccountVO;
    return bindCardResultVO;
  }

  public static BindCardResultVO from(LoanBankAccountVO loanBankAccountVO, ValidationCardStatus validationCardStatus, SDKType sdkType, boolean canModifyName) {
    BindCardResultVO bindCardResultVO = from(loanBankAccountVO);
    bindCardResultVO.validationCardStatus = validationCardStatus;

    if (BankAccountAvailableStatus.NEED_CONFIRM_STATUS_LIST.contains(loanBankAccountVO.bankAccountAvailableStatus)) {
      String errRemind = sdkType == SDKType.IDN_FIN ? validationCardStatus.getFinRemind() : validationCardStatus.getLoanRemind();
      bindCardResultVO.errRemind = TT.gen(errRemind);
    }

    //只有借贷需要用到下面这些字段
    if (sdkType == SDKType.IDN_FIN) {
      return bindCardResultVO;
    }

    bindCardResultVO.errRemindForBankAccount = bindCardResultVO.errRemind;

    if (loanBankAccountVO.bankAccountAvailableStatus == BankAccountAvailableStatus.UNAVAILABLE) {
      bindCardResultVO.canModifyName = canModifyName;
      fillResultVOForNameNotMatchStrategy(bindCardResultVO, validationCardStatus, canModifyName);
    }

    return bindCardResultVO;
  }

  private static void fillResultVOForNameNotMatchStrategy(BindCardResultVO bindCardResultVO, ValidationCardStatus validationCardStatus, boolean canModifyName) {
    if (!ValidationCardStatus.NOT_MATCH_LIST.contains(validationCardStatus)) {
      return;
    }
    bindCardResultVO.errRemindForBankType = TT.gen("若无银行卡，可使用电子钱包作为收款渠道。请确保您的电子钱包账户已升级，否则会因额度不够造成打款失败。");
    bindCardResultVO.errRemindForBankAccount = TT.gen("绑定非本人账号借款会失败，请输入本人收款账号");
    if (canModifyName) {
      bindCardResultVO.errRemindForName = TT.gen("若银行卡姓名错误，请修改姓名");
    }
  }

  /**
   * 填充银行卡返回文案
   *
   * @param loanBankAccountVO
   * @param finalValidationStatus
   * @param canModifyName
   * @return
   */
  public static BindCardResultVO fromIdnLoanBankCardV2(LoanBankAccountVO loanBankAccountVO, FinalValidationBankAccountStatus finalValidationStatus, boolean canModifyName) {
    BindCardResultVO bindCardResultVO = from(loanBankAccountVO);
    bindCardResultVO.canModifyName = canModifyName;
    bindCardResultVO.finalValidationBankAccountStatus = finalValidationStatus;
    switch (finalValidationStatus) {
      case MATCH:
      case PENDING:
        break;
      case CARD_FREEZE:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EWALLET_AND_EDIT);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("该账号不可用，请绑定其他银行账号");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有银行账号可以使用电子钱包借款");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请绑定其他银行账号");
        break;
      case CARD_UNAVAILABLE:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EWALLET_AND_EDIT);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("银行账号不正确，请重新输入。");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有银行账号可以使用电子钱包借款");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请输入正确的银行账号");
        break;
      case NAME_NOT_MATCH:
      case VALIDATION_NAME_EMPTY:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EWALLET_AND_EDIT);
        if (canModifyName) {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请使用您本人的银行账号并填写正确的姓名。");
          bindCardResultVO.errRemindContentForPopup = TT.gen("没有银行账号请使用电子钱包借款");
          bindCardResultVO.errRemindForName = TT.gen("须同银行账号姓名一致");
        } else {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请使用您本人的银行账号。");
          bindCardResultVO.errRemindContentForPopup = TT.gen("本人没有银行账号请使用电子钱包借款");
        }
        bindCardResultVO.errRemindForBankAccount = TT.gen("请输入本人的银行账号");
        break;
      default:
        throw EcException.error("unsupported validationBankAccountStatusV2:{}", finalValidationStatus);
    }

    return bindCardResultVO;
  }

  /**
   * 填充电子钱包返回文案
   *
   * @param loanBankAccountVO
   * @param finalValidationStatus
   * @param canModifyName
   * @return
   */
  public static BindCardResultVO fromIdnLoanEWalletV2(LoanBankAccountVO loanBankAccountVO, FinalValidationBankAccountStatus finalValidationStatus, boolean canModifyName) {
    BindCardResultVO bindCardResultVO = from(loanBankAccountVO);
    bindCardResultVO.canModifyName = canModifyName;
    bindCardResultVO.finalValidationBankAccountStatus = finalValidationStatus;
    switch (finalValidationStatus) {
      case MATCH:
      case PENDING:
        break;
      case CARD_FREEZE:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EDIT_AND_REGISTER);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("该账号不可用，请绑定其他电子钱包账号");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请绑定其他电子钱包账号");
        break;
      case CARD_UNAVAILABLE:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EDIT_AND_REGISTER);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("电子钱包账号不正确，请重新输入。");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请输入正确的电子钱包账号！");
        break;
      case E_WALLET_MOBILE_NUMBER_NOT_MATCH:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EDIT_AND_REGISTER);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("请使用和Easycash注册手机号相同的电子钱包账号");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请输入正确的电子钱包账号！");
        break;
      case NAME_NOT_MATCH:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EDIT_AND_REGISTER);
        if (canModifyName) {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请检查您本人的账号并填写正确的姓名");
          bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
          bindCardResultVO.errRemindForBankAccount = TT.gen("请输入本人的电子钱包账号！");
          bindCardResultVO.errRemindForName = TT.gen("须同电子钱包姓名一致！");
        } else {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请绑定您本人的账号");
          bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
          bindCardResultVO.errRemindForBankAccount = TT.gen("请输入本人的电子钱包账号！");
        }
        break;
      case E_WALLET_MOBILE_NUMBER_AND_NAME_NOT_MATCH:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.EDIT_AND_REGISTER);
        if (canModifyName) {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请检查您本人的账号并填写正确的姓名");
          bindCardResultVO.errRemindContentForPopup = TT.gen("请使用和Easycash注册手机号相同的电子钱包账号或确保电子钱包账号姓名与Easycash账号姓名一致，没有账号请注册！");
          bindCardResultVO.errRemindForBankAccount = TT.gen("请输入本人的电子钱包账号！");
          bindCardResultVO.errRemindForName = TT.gen("须同电子钱包姓名一致！");
        } else {
          bindCardResultVO.errRemindTitleForPopup = TT.gen("请绑定您本人的账号");
          bindCardResultVO.errRemindContentForPopup = TT.gen("请使用和Easycash注册手机号相同的电子钱包账号或确保电子钱包账号姓名与Easycash账号姓名一致，没有账号请注册！");
          bindCardResultVO.errRemindForBankAccount = TT.gen("请输入本人的电子钱包账号！");
        }
        break;
      case VALIDATION_NAME_EMPTY:
        bindCardResultVO.setPopupInfo(ValidationBankAccountPopupType.AUTHENTICATION_AND_REGISTER);
        bindCardResultVO.errRemindTitleForPopup = TT.gen("请绑定已实名的账号");
        bindCardResultVO.errRemindContentForPopup = TT.gen("没有账号请注册！");
        bindCardResultVO.errRemindForBankAccount = TT.gen("请输入已实名的钱包账号！");
        break;
      default:
        throw EcException.error("unsupported validationBankAccountStatusV2:{}", finalValidationStatus);
    }
    return bindCardResultVO;
  }

  private void setPopupInfo(ValidationBankAccountPopupType popupType) {
    this.popupType = popupType;
    switch (popupType) {
      case EWALLET_AND_EDIT:
        this.popupLeftButtonText = TT.gen("电子钱包");
        this.popupRightButtonText = TT.gen("编辑");
        break;
      case EDIT_AND_REGISTER:
        this.popupLeftButtonText = TT.gen("编辑");
        this.popupRightButtonText = TT.gen("注册");
        break;
      case AUTHENTICATION_AND_REGISTER:
        this.popupLeftButtonText = TT.gen("去实名");
        this.popupRightButtonText = TT.gen("注册");
        break;
      default:
        throw EcException.error("unsupported popupType:{}", popupType);
    }
  }
}
