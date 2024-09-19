package com.dws.challenge.repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transaction;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.NotificationService;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();

	private final NotificationService notificationService;

	public AccountsRepositoryInMemory(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public void createAccount(Account account) throws DuplicateAccountIdException {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException("Account id " + account.getAccountId() + " already exists!");
		}
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

	@Override
	public String transaction(final Transaction transaction) throws InsufficientBalanceException, AccountNotFoundException {
		String uuid = "Transaction Id: " + UUID.randomUUID().toString();
		Account accountFrom = getAccount(transaction.getAccountFromId());
		if (null != accountFrom) {
			if (accountFrom.getBalance().compareTo(transaction.getAmount()) >= 0) {
				Account accountTo = getAccount(transaction.getAccountToId());
				if (null != accountTo) {
					BigDecimal afterDeductBalance = accountFrom.getBalance().subtract(transaction.getAmount());
					BigDecimal afterCreditBalance = accountTo.getBalance().add(transaction.getAmount());
					accountTo.setBalance(afterCreditBalance);
					accountFrom.setBalance(afterDeductBalance);
					accounts.put(accountFrom.getAccountId(), accountFrom);
					accounts.put(accountTo.getAccountId(), accountTo);
					notificationService.notifyAboutTransfer(accountFrom, "$"+ transaction.getAmount() + " has been deducted from the account and transferred to account Id: "+transaction.getAccountToId());
					notificationService.notifyAboutTransfer(accountTo, "$"+transaction.getAmount() + " has been credited to the account from the account Id: "+ transaction.getAccountFromId());
				} else {
					throw new AccountNotFoundException("Account id " + transaction.getAccountToId() + " not found!");
				}
			} else {
				throw new InsufficientBalanceException("Account id " + accountFrom.getAccountId()
						+ " doesn't have the sufficient balance to transfer!");
			}
		} else {
			throw new AccountNotFoundException("Account id " + transaction.getAccountFromId() + " not found!");
		}
		return uuid;
	}

}
