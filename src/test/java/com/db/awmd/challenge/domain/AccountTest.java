package com.db.awmd.challenge.domain;

import com.db.awmd.challenge.exception.InSufficientFundException;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class AccountTest {

    private Account underTest;

    @Before
    public void setUp(){
        underTest = new Account("1234", new BigDecimal("10.00"));
    }

    @Test
    public void itShouldAllowWithdrawal(){
        underTest.withdraw(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("0.00"), underTest.getBalance());
    }

    @Test
    public void itShouldNotAllowOverDraft(){
        try {
            underTest.withdraw(new BigDecimal("20.00"));
            fail("Expecting InSufficientFundException");
        }catch (InSufficientFundException ex){
            assertEquals("Insufficient balance in account : 1234, Unable to withdraw amount: 20.00", ex.getMessage());
        }
    }

    @Test
    public void withDrawShouldHandleNullAmount(){
        underTest.withdraw(null);
        assertEquals(new BigDecimal("10.00"), underTest.getBalance());

    }

    @Test
    public void withDrawShoudlHandleNegativeAmount(){
        underTest.withdraw(new BigDecimal("-10.00"));
        assertEquals(new BigDecimal("10.00"), underTest.getBalance());

    }

    @Test
    public void itShouldAllowDeposit(){
        underTest.deposit(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("30.00"), underTest.getBalance());
    }

    @Test
    public void itShouldHandleNullwDeposit(){
        underTest.deposit(null);
        assertEquals(new BigDecimal("10.00"), underTest.getBalance());
    }

    @Test
    public void itShouldHandleNeagtiveDeposit(){
        underTest.deposit(null);
        assertEquals(new BigDecimal("10.00"), underTest.getBalance());
    }
}