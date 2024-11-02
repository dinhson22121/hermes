package net.devnguyen.hermes.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Data
public class AccountOperationDTO {
    private String id;
    private String accountId;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant createdAt;
    private BigDecimal balance;
    private String type;

    @JsonIgnore
    public boolean isIncrBalance(){
        return "incr_balance".equals(type);
    }

    @JsonIgnore
    public boolean isDecrBalance(){
        return "decr_balance".equals(type);
    }
}
