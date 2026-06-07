package gr.unipi.eshop.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateItemRequest(@Min(1) @Max(CartPolicy.MAX_QUANTITY_PER_ITEM) int quantity) {
}
