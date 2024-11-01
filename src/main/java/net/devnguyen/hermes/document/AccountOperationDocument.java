package net.devnguyen.hermes.document;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "account_operation")
@Data
public class AccountOperationDocument {
    private String id;
    private String accountId;
    private String status;
    private Instant expiredAt;
    private String type;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
