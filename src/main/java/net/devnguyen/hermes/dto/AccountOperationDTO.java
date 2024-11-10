package net.devnguyen.hermes.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devnguyen.hermes.utils.Utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOperationDTO {

    @NotNull
    private String id;

    @NotNull
    private String accountId;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant createdAt;

    @NotNull
    private BigDecimal balance;

    @NotNull
    private AccountOperationType type;

    private String requestId;

    public String toJson(){
        return Utils.toJson(this);
    }

    public enum AccountOperationType {
        increase_balance
    }


    public static AccountOperationDTO readValue(String value){
        try{
            return Utils.objectMapper.readValue(value,AccountOperationDTO.class);
        }catch (Exception e){
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
