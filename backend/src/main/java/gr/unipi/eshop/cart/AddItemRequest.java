package gr.unipi.eshop.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddItemRequest(@NotNull UUID productReference,
                             @Min(1) @Max(CartPolicy.MAX_QUANTITY_PER_ITEM) int quantity) {
}
