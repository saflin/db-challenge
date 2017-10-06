package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.NotEmpty;

import java.math.BigDecimal;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class Transfer {

    @NotNull
    @NotEmpty
    private final String fromAccountId;

    @NotNull
    @NotEmpty
    private final String toAccountId;

    @NotNull
    @Min(value = 0, message = "Amount to be transfered must be positive")
    private BigDecimal transferAmount;


    @JsonCreator
    public Transfer(@JsonProperty("fromAccountId") String fromAccountId,
                    @JsonProperty("toAccountId") String toAccountId,
                   @JsonProperty("transferAmount") BigDecimal transferAmount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.transferAmount = transferAmount;
    }

    public boolean areAccountsSame() {
        return  getFromAccountId().equals(getToAccountId());
    }

    public boolean isTransferAmountValid(){
        return transferAmount.compareTo(BigDecimal.ZERO) > 0;
    }

}
