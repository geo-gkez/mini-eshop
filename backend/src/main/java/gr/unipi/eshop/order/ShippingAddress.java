package gr.unipi.eshop.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record ShippingAddress(
        @NotBlank @Size(max = 200) @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain line breaks") String street,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain line breaks") String city,
        @NotBlank @Size(max = 20)  @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain line breaks") String postalCode,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[^\\r\\n]*$", message = "must not contain line breaks") String country
) implements Serializable {}
