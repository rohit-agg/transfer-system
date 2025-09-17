package com.transfer.application.dtos.accounts;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateAccount {

    @Positive
    private Integer accountId;

    @Positive
    private Double initialBalance;
}
