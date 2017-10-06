package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.NotificationService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransferControllerTest {

    private static final String TRANSFERS_URL = "/v1/transfers";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private NotificationService mockNotificationService;

    @Autowired
    private TransferController transferController;

    @Autowired
    private AccountsRepository accountsRepository;

    private Account accountA = new Account("ID-A", new BigDecimal("10.00"));

    private Account accountB = new Account("ID-B", new BigDecimal("10.00"));


    @Before
    public void setUp(){
     accountsRepository.clearAccounts();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();

    }

    @Test
    public void itShouldTransferFundBetweenAccounts() throws Exception {
        accountsRepository.createAccount(accountA);
        accountsRepository.createAccount(accountB);

        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":10}"))
                .andExpect(status().isOk());

        assertThat(accountA.getBalance()).isEqualByComparingTo("0.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("20.00");

    }

    @Test
    public void itShouldNotTransferForInvalidAmount() throws Exception {

        accountsRepository.createAccount(accountA);
        accountsRepository.createAccount(accountB);

        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":-1}"))
                .andExpect(status().isBadRequest());

        assertThat(accountA.getBalance()).isEqualByComparingTo("10.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("10.00");

        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\": 0}"))
                .andExpect(status().isBadRequest());

        assertThat(accountA.getBalance()).isEqualByComparingTo("10.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("10.00");

    }


    @Test
    public void itShouldNotTransferBetweenSameAccounts() throws Exception {
        accountsRepository.createAccount(accountA);
        accountsRepository.createAccount(accountB);

        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-A\",\"transferAmount\":10}"))
                .andExpect(status().isBadRequest());

        assertThat(accountA.getBalance()).isEqualByComparingTo("10.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("10.00");

    }


    @Test
    public void itShouldNotTransferOnInsufficientFunds() throws Exception {
        accountsRepository.createAccount(accountA);
        accountsRepository.createAccount(accountB);

        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":50}"))
                .andExpect(status().isBadRequest());

        assertThat(accountA.getBalance()).isEqualByComparingTo("10.00");
        assertThat(accountB.getBalance()).isEqualByComparingTo("10.00");

    }

    @Test
    public void itShouldValidateAccounts() throws Exception {
        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":10}"))
                .andExpect(status().isBadRequest());

        accountsRepository.createAccount(accountA);
        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":10}"))
                .andExpect(status().isBadRequest());

        accountsRepository.createAccount(accountB);
        this.mockMvc.perform(post(TRANSFERS_URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccountId\":\"ID-A\",\"toAccountId\":\"ID-B\",\"transferAmount\":10}"))
                .andExpect(status().isOk());

    }
}