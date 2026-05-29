package gr.unipi.eshop.cart;

import jakarta.validation.constraints.Min;

public record UpdateItemRequest(@Min(1) int quantity) {
}
