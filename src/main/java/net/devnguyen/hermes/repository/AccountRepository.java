package net.devnguyen.hermes.repository;

import net.devnguyen.hermes.document.AccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountRepository extends MongoRepository<AccountDocument, String> {
}
