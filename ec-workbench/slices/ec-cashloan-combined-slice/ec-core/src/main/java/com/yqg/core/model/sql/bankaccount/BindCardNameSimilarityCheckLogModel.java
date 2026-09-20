package com.yqg.core.model.sql.bankaccount;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.BindCardNameSimilarityCheckLog;
import com.yqg.core.util.NameSimilarityUtil.NameSimilarityScoreDetail;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

/**
 * 绑卡姓名相似度校验明细表 Model（TAPD-360957）。
 * 追加型日志表，一次校验一行，不做更新；PII 明文列（baseline_name/submitted_name/bank_account_number）
 * 由 JDBC 加密驱动透明加解密，本 Model 只写明文列，不手动填充 encrypted_xxx/hashed_xxx 列。
 */
@Repository
public class BindCardNameSimilarityCheckLogModel extends YqgBaseModel {

  private static final BindCardNameSimilarityCheckLog TABLE = Tables.BIND_CARD_NAME_SIMILARITY_CHECK_LOG;

  /**
   * 插入一条姓名相似度校验明细记录。
   *
   * @param userId            用户ID
   * @param loanAccountId     借款人账户ID
   * @param entry             绑卡场景（{@code BindCardNameSimilarityCheckService.BindCardLogEntry} 枚举名）
   * @param experimentGroup   AB实验分组：只有 EXPERIMENT_GROUP 会触发校验与落库（调用方门禁保证），恒为该值
   * @param baselineName      原姓名基准
   * @param submittedName     用户本次提交的持卡人姓名
   * @param bankAccountNumber 银行卡号
   * @param detail            相似度分值明细（3 项子分值 + 综合分值）
   * @param similarityResult  相似度是否达标：MATCH/NOT_MATCH
   */
  public void insert(Long userId,
      Long loanAccountId,
      String entry,
      String experimentGroup,
      String baselineName,
      String submittedName,
      String bankAccountNumber,
      NameSimilarityScoreDetail detail,
      String similarityResult) {
    long now = Clock.now();
    create()
        .insertInto(
            TABLE,
            TABLE.USER_ID,
            TABLE.LOAN_ACCOUNT_ID,
            TABLE.ENTRY,
            TABLE.EXPERIMENT_GROUP,
            TABLE.BASELINE_NAME,
            TABLE.SUBMITTED_NAME,
            TABLE.BANK_ACCOUNT_NUMBER,
            TABLE.TOKEN_SET_RATIO_SCORE,
            TABLE.RATIO_SCORE,
            TABLE.PARTIAL_RATIO_SCORE,
            TABLE.FINAL_SCORE,
            TABLE.SIMILARITY_RESULT,
            TABLE.TIME_CREATED,
            TABLE.TIME_UPDATED)
        .values(
            userId,
            loanAccountId,
            entry,
            experimentGroup,
            baselineName,
            submittedName,
            bankAccountNumber,
            detail.getTokenSetRatio(),
            detail.getRatio(),
            detail.getPartialRatio(),
            detail.getFinalScore(),
            similarityResult,
            now,
            now)
        .execute();
  }
}
