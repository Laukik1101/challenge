package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transaction;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;
	
	@Mock
	private NotificationService notificationService;

	@Test
	void addAccount() {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	void addAccount_failsOnDuplicateId() {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}
	}

	void addAccount(String accountId, double balance) {
		Account account = new Account(accountId);
		account.setBalance(new BigDecimal(balance));
		this.accountsService.createAccount(account);
	}

	@Test
	void transferAmount() {
		String accountFromId = "Id-4321";
		String accountToId = "Id-1234";
		addAccount(accountFromId,2000.00);
		addAccount(accountToId,1000.00);
		Transaction transaction = new Transaction(accountFromId, accountToId, new BigDecimal(200));
		this.accountsService.transaction(transaction);
		Account accountFrom = accountsService.getAccount(accountFromId);
		Account accountTo = accountsService.getAccount(accountToId);
		assertThat(accountFrom.getAccountId()).isEqualTo("Id-4321");
		assertThat(accountFrom.getBalance()).isEqualByComparingTo("1800");
		assertThat(accountTo.getAccountId()).isEqualTo("Id-1234");
		assertThat(accountTo.getBalance()).isEqualByComparingTo("1200");
	}

	@Test
	void transferAmount_AccountNotFound() {
		String accountFrom = "Id-432112";
		Transaction transaction = new Transaction(accountFrom, "Id-1234", new BigDecimal(20000));
		try {
			this.accountsService.transaction(transaction);
			fail("Account not found");
		} catch (AccountNotFoundException e) {
			assertThat(e.getMessage()).isEqualTo("Account id "+accountFrom+" not found!");
		}
	}
	
	@Test
	void transferAmount_InsufficientBalance() {
		String accountFromId = "Id-1111";
		String accountToId = "Id-2222";
		addAccount(accountFromId,2000.00);
		addAccount(accountToId,1000.00);
		Transaction transaction = new Transaction(accountFromId, accountToId, new BigDecimal(20000));
		try {
			this.accountsService.transaction(transaction);
			fail("Insufficient balance can't transfer");
		} catch (InsufficientBalanceException e) {
			assertThat(e.getMessage()).isEqualTo("Account id "+accountFromId+" doesn't have the sufficient balance to transfer!");
		}
		
	}	
	
}
