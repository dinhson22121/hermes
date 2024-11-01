package net.devnguyen.hermes.service;

import lombok.RequiredArgsConstructor;
import net.devnguyen.hermes.document.AccountDocument;
import net.devnguyen.hermes.dto.ResponseDTO;
import net.devnguyen.hermes.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountDocument findOrInsertById(String id, BigDecimal initialBalance) {
        Optional<AccountDocument> existingAccount = accountRepository.findById(id);

        if (existingAccount.isPresent()) {
            return existingAccount.get();
        } else {
            AccountDocument newAccount = new AccountDocument();
            newAccount.setId(id);
            newAccount.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO);
            newAccount.setFreezeBalance(BigDecimal.ZERO);
            newAccount.setCreatedAt(Instant.now());

            AccountDocument savedAccount = accountRepository.save(newAccount);

            return savedAccount;
        }
    }

    public void handleOperation(String id, String operationId) {

    }

    public void incrBalance(String id, BigDecimal amount) {
        var account = findOrInsertById(id, BigDecimal.ZERO);
        account.increaseBalance(amount);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
    }
}
