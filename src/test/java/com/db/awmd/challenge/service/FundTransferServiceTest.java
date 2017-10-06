package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.FundTransferException;
import com.db.awmd.challenge.exception.InSufficientFundException;
import com.db.awmd.challenge.exception.InValidTransferRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for FundTransferService
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FundTransferServiceTest {

    @Autowired
    private AccountsService accountsService;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private FundTransferService underTest;

    private Account accountA = new Account("ID-A", new BigDecimal("10.00"));

    private Account accountB = new Account("ID-B", new BigDecimal("10.00"));

    private Account accountC = new Account("ID-C", new BigDecimal("1000.00"));

    private Account accountD = new Account("ID-D", new BigDecimal("1000.00"));



    @Before
    public void setUp(){
        accountsService.getAccountsRepository().clearAccounts();
    }

    @Test
    public void itShouldTransferFunds(){
        accountsService.createAccount(accountA);
        accountsService.createAccount(accountB);
        Transfer transfer = new Transfer("ID-A", "ID-B", new BigDecimal("10.00"));
        underTest.transferFund(transfer);
        assertThat(accountA.getBalance()).isEqualByComparingTo("0.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("20.00");
        verify(notificationService).notifyAboutTransfer(eq(accountB), eq("An amount of 10.00 received from Account ID-A"));
        verify(notificationService).notifyAboutTransfer(eq(accountA), eq("An amount of 10.00 transferred to Account ID-B"));
    }

    @Test
    public void itShouldThrowInsufficientFundException(){
        accountsService.createAccount(accountA);
        accountsService.createAccount(accountB);
        Transfer transfer = new Transfer("ID-A", "ID-B", new BigDecimal("50.00"));
        try {
            underTest.transferFund(transfer);
            verify(notificationService,never()).notifyAboutTransfer(any(Account.class),anyString());
            fail("Expecting InSufficientFundException to be thrown.");
        }catch (InSufficientFundException ex){
            assertEquals("Insufficient balance in account : ID-A, Unable to withdraw amount: 50.00", ex.getMessage());
        }
    }

    @Test
    public void itShouldNotTransferForSameAccounts(){
        accountsService.createAccount(accountA);
        Transfer transfer = new Transfer("ID-A", "ID-A", new BigDecimal("50.00"));
        try {
            underTest.transferFund(transfer);
            verify(notificationService,never()).notifyAboutTransfer(any(Account.class),anyString());
            fail("Expecting InValidTransferRequestException to be thrown.");
        }catch (InValidTransferRequestException ex){
            assertEquals("Fund transfer to same account is not allowed.", ex.getMessage());
        }

    }

    @Test
    public void thowExceptionIfSourceAccountDoesntExists(){
        accountsService.createAccount(accountB);
        Transfer transfer = new Transfer("ID-A", "ID-B", new BigDecimal("10.00"));
        try {
            underTest.transferFund(transfer);
            verify(notificationService,never()).notifyAboutTransfer(any(Account.class),anyString());
            fail("Expecting AccountNotFoundException to be thrown.");
        }catch (AccountNotFoundException ex){
            assertEquals("Account with ID: ID-A doesnt exists.", ex.getMessage());
        }

    }

    @Test
    public void thowExceptionIfDestinationAccountDoesntExists(){
        accountsService.createAccount(accountA);
        Transfer transfer = new Transfer("ID-A", "ID-B", new BigDecimal("10.00"));
        try {
            underTest.transferFund(transfer);
            verify(notificationService,never()).notifyAboutTransfer(any(Account.class),anyString());
            fail("Expecting AccountNotFoundException to be thrown.");
        }catch (AccountNotFoundException ex){
            assertEquals("Account with ID: ID-B doesnt exists.", ex.getMessage());
        }

    }

    @Test
    public void itShouldThrowExceptionTransferAmountIsInvalid(){
        accountsService.createAccount(accountA);
        accountsService.createAccount(accountB);
        Transfer transfer = new Transfer("ID-A", "ID-B", new BigDecimal("0.00"));
        try {
            underTest.transferFund(transfer);
            verify(notificationService,never()).notifyAboutTransfer(any(Account.class),anyString());
            fail("Expecting InValidTransferRequestException to be thrown.");
        }catch (InValidTransferRequestException ex){
            assertEquals("Fund transfer amount should be greater than Zero.", ex.getMessage());
        }
    }

    @Test
    public void transferFundShouldNotDeadLock(){
        accountsService.createAccount(accountC);
        accountsService.createAccount(accountD);
        Transfer transferA = new Transfer("ID-C", "ID-D", new BigDecimal("5.00"));
        Transfer transferB = new Transfer("ID-D", "ID-C", new BigDecimal("5.00"));
        List<CallableTransferService> list = new ArrayList<>();
        list.add(new CallableTransferService(transferA, underTest));
        list.add(new CallableTransferService(transferB, underTest));
        list.add(new CallableTransferService(transferA, underTest));
        list.add(new CallableTransferService(transferB, underTest));
        list.add(new CallableTransferService(transferA, underTest));
        list.add(new CallableTransferService(transferB, underTest));
        list.add(new CallableTransferService(transferA, underTest));
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> futureList = executorService.invokeAll(list);
            int count = 10;
            do{
                Thread.sleep(1000);
                Iterator<Future<String>> it = futureList.iterator();
                while (it.hasNext()){
                   if(it.next().isDone()){
                       it.remove();
                   }
                    count --;
                }
            } while (!futureList.isEmpty() && count > 0);
            executorService.shutdown();
            assertThat(futureList).isEmpty();
            assertThat(accountC.getBalance()).isEqualByComparingTo("995.00");
            assertThat(accountD.getBalance()).isEqualByComparingTo("1005.00");
        }catch (Exception ex) {
            ex.printStackTrace();
            fail("failed to transfer funds.");
        }
    }

    class CallableTransferService implements Callable<String> {
        Transfer transfer;
        FundTransferService fundTransferService;

        CallableTransferService(Transfer transfer, FundTransferService fundTransferService){
            this.fundTransferService = fundTransferService;
            this.transfer = transfer;
        }

        @Override
        public String call() {
            fundTransferService.transferFund(transfer);
            return "transfer executed";
        }
    }
}