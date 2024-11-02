package net.devnguyen.hermes.service;

import lombok.RequiredArgsConstructor;
import net.devnguyen.hermes.document.AccountDocument;
import net.devnguyen.hermes.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountService {

    @Autowired
    private  final AccountRepository accountRepository;

//    @Autowired
//    @Qualifier("myMongoTemplate")
//    private MongoTemplate myMongoTemplate;

    public AccountDocument findOrInsertById(String id, BigDecimal initialBalance) {
//        AccountDocument existingAccount = myMongoTemplate.findById(id, AccountDocument.class);
        AccountDocument existingAccount = accountRepository.findById(id).orElse(null);

        if (existingAccount != null) {
            return existingAccount;
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

    public void incrBalance(String id, BigDecimal amount, Instant operationCreatedAt) {
        var account = findOrInsertById(id, BigDecimal.ZERO);
        account.increaseBalance(amount);
        if (account.getFirstOperationAt() == null) {
            account.setFirstOperationAt(operationCreatedAt);
        }
        account.setUpdatedAt(Instant.now());

        accountRepository.save(account);
    }
}
