package com.ht.shoppingcart.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    @NotBlank(message = "offerId is mandatory")
    private String offerId;

    // action is intentionally NOT included here — it is determined
    // by which endpoint is called, not by the client. The service
    // layer sets it explicitly based on the operation being performed.

    @NotEmpty(message = "At least one price is required")
    @Valid
    private List<PriceDto> prices;

    @JsonIgnore
    @AssertTrue(message = "Each price must be valid for its type: "
            + "RECURRING requires numberOfRecurrences and recurrenceUnit; "
            + "ONE_TIME must not include recurrence fields")
    public boolean isPricesValidForType() {
        if (prices == null || prices.isEmpty()) return true;
        return prices.stream().allMatch(PriceDto::isValidForType);
    }
}