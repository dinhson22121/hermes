package net.devnguyen.hermes.document;

import lombok.Data;
import net.devnguyen.hermes.dto.ResponseDTO;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "account_5")
@Data
public class AccountDocument {
    @Id
    private String id;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance = BigDecimal.ZERO;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal freezeBalance = BigDecimal.ZERO;


    private Instant createdAt;

    private Instant updatedAt;

    // === PUBLIC METHOD ===

    public ResponseDTO<Boolean> increaseBalance(BigDecimal amount) {
        balance = balance.add(amount);
        return ResponseDTO.success(true);
    }

    public ResponseDTO<Boolean> decreaseBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseDTO.fail("Amount to decrease must be non-negative", false);
        }
        if (balance.compareTo(amount) < 0) {
            return ResponseDTO.fail("Insufficient balance to decrease", false);
        }
        balance = balance.subtract(amount);
        return ResponseDTO.success(true);
    }

    public ResponseDTO<Boolean> freezeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseDTO.fail("Amount to freeze must be non-negative", false);
        }
        if (balance.compareTo(amount) < 0) {
            return ResponseDTO.fail("Insufficient balance to freeze", false);
        }
        balance = balance.subtract(amount);
        freezeBalance = freezeBalance.add(amount);
        return ResponseDTO.success(true);
    }

    public ResponseDTO<Boolean> unfreezeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseDTO.fail("Amount to unfreeze must be non-negative", false);
        }
        if (freezeBalance.compareTo(amount) < 0) {
            return ResponseDTO.fail("Insufficient freeze balance to unfreeze", false);
        }
        balance = balance.add(amount);
        freezeBalance = freezeBalance.subtract(amount);
        return ResponseDTO.success(true);
    }

    public ResponseDTO<Boolean> releaseBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseDTO.fail("Amount to release must be non-negative", false);
        }
        if (freezeBalance.compareTo(amount) < 0) {
            return ResponseDTO.fail("Insufficient freeze balance to release", false);
        }
        freezeBalance = freezeBalance.subtract(amount);
        return ResponseDTO.success(true);
    }

    // === PRIVATE METHOD ===
}
