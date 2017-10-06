package com.db.awmd.challenge.domain;

import com.db.awmd.challenge.exception.InSufficientFundException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import static java.lang.String.format;

@Data
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  @Min(value = 0, message = "Initial balance must be positive.")
  private volatile BigDecimal balance;

  public Account(String accountId) {
    this.accountId = accountId;
    this.balance = BigDecimal.ZERO;
  }

  @JsonCreator
  public Account(@JsonProperty("accountId") String accountId,
    @JsonProperty("balance") BigDecimal balance) {
    this.accountId = accountId;
    this.balance = balance;
  }

  public synchronized void deposit(BigDecimal amount){
    if(isValidAmount(amount)){
        balance = balance.add(amount);
    }
  }

  public synchronized void withdraw(BigDecimal amount) throws InSufficientFundException{
    if(isValidAmount(amount)){
      BigDecimal newBalance = balance.subtract(amount);
      if (newBalance.compareTo(BigDecimal.ZERO) < 0){
          throw new InSufficientFundException(format("Insufficient balance in account : %s, Unable to withdraw amount: %s",
                  accountId, amount));
      }else {
        balance = newBalance;
      }
    }
  }

  private boolean isValidAmount(BigDecimal amount) {
    return amount != null && (amount.compareTo(BigDecimal.ZERO) >= 0);
  }
}
