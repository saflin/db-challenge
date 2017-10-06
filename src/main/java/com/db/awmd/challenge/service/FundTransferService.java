package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.FundTransferException;
import com.db.awmd.challenge.exception.InSufficientFundException;
import com.db.awmd.challenge.exception.InValidTransferRequestException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

/**
 * Service that manages fund transfer between accounts.
 */
@Service
@Slf4j
public class FundTransferService {

    private NotificationService notificationService;

    private AccountsService accountsService;


    @Autowired
    public FundTransferService(final NotificationService notificationService, final AccountsService accountsService) {
        this.notificationService = notificationService;
        this.accountsService = accountsService;
    }

    /**
     * Transfer fund between two accounts.
     * Throws InValidTransferRequestException if from and to accounts are same.
     * Throws InValidTransferRequestException if transfer amount is <= 0.
     * Throws AccountNotFoundException if accounts doesnt exists
     * Throws InSufficientFundException if there is no fund available to withdraw.
     * Throws FundTransferException if transfer fails.
     * @param transfer
     */
    public void transferFund(final Transfer transfer) {

        if (transfer.areAccountsSame()) {
            throw new InValidTransferRequestException("Fund transfer to same account is not allowed.");
        }

        if(!transfer.isTransferAmountValid()){
            throw new InValidTransferRequestException("Fund transfer amount should be greater than Zero.");
        }

        Account fromAccount = accountsService.getAccount(transfer.getFromAccountId());
        if (fromAccount == null) {
            throw new AccountNotFoundException(format("Account with ID: %s doesnt exists.", transfer.getFromAccountId()));
        }

        Account toAccount = accountsService.getAccount(transfer.getToAccountId());
        if (toAccount == null) {
            throw new AccountNotFoundException(format("Account with ID: %s doesnt exists.", transfer.getToAccountId()));
        }
        transferFundThreadSafely(fromAccount, toAccount, transfer.getTransferAmount());
    }


    /**
     * Perform's fund transfer inside synchronised context.
     * Thread locking is done on account objects .
     * Account objects are sorted in predictive manner to avoid dead lock.
     * @param fromAccount
     * @param toAccount
     * @param amount
     */
    private void transferFundThreadSafely(Account fromAccount, Account toAccount, BigDecimal amount) {
        //prevent dead lock by ordering the lock
        Object lock_1 = fromAccount.getAccountId().compareTo(toAccount.getAccountId()) < 0 ? fromAccount : toAccount;
        Object lock_2 = lock_1 != fromAccount ? fromAccount : toAccount;
        log.debug("Getting lock on lock 1 {} ",lock_1);
        synchronized (lock_1) {
            log.debug("Getting lock on lock 2 {} ",lock_2);
            synchronized (lock_2) {
                withdrawFund(fromAccount, amount);
                depositFund(fromAccount, toAccount, amount);
                notifyTransferStatus(fromAccount, toAccount, amount);
            }
        }

    }


    /**
     * Deposit amount to toAccount.
     * Rollback withdrawal from fromAccount, if deposit fails.
     * To avoid data race condition, this method should be invoked
     * after getting synchronised lock on fromAccount and toAccount.
     * See implementation of transferFundThreadSafely.
     * @param fromAccount
     * @param toAccount
     * @param amount
     */
    private void depositFund(final Account fromAccount, final Account toAccount, final BigDecimal amount) {
        try {
            log.debug("Depositing amount:{} to account {}", amount, toAccount.getAccountId());
            toAccount.deposit(amount);
        } catch (Exception ex) {
            log.error(format("Exception while depositing fund to account %s",toAccount.getAccountId()),ex);
            //rollback withdrawal
            fromAccount.deposit(amount);
            throw new FundTransferException(format("Failed to transfer fund to Account: %s", toAccount.getAccountId()));
        }
    }

    /**
     * Withdraw's amount from fromAccount.
     * To avoid data race condition, this method should be invoked
     * after getting synchronised lock on fromAccount.
     * See implementation of transferFundThreadSafely.
     * @param fromAccount
     * @param amount
     */
    private void withdrawFund(final Account fromAccount, final BigDecimal amount) {
        try {
            log.debug("Withdrawing amount:{} from account {}", amount, fromAccount.getAccountId());
            fromAccount.withdraw(amount);
        } catch (InSufficientFundException ex) {
            log.error(format("Exception while withdrawing fund from account %s",fromAccount.getAccountId()), ex);
            throw ex;
        }
    }

    /**
     * Notify fund transfer to account holder's.
     * @param fromAccount
     * @param toAccount
     * @param amount
     */
    private void notifyTransferStatus(Account fromAccount, Account toAccount, BigDecimal amount) {
        try {
            notificationService.notifyAboutTransfer(toAccount, "An amount of " + amount + " received from Account " + fromAccount.getAccountId());
            notificationService.notifyAboutTransfer(fromAccount, "An amount of " + amount + " transferred to Account " + toAccount.getAccountId());
        } catch (Exception ex) {
            //notification failure should not affect the fund transfer.
            log.error("Exception while notifying account holders ",ex);
        }
    }
}
