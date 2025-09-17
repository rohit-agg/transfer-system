package com.transfer.application.dtos.transactions;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubmitTransaction {

    @Positive
    private Integer sourceAccountId;

    @Positive
    private Integer destinationAccountId;

    @Positive
    private Double amount;
}
