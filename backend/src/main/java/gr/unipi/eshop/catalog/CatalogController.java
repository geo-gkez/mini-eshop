package gr.unipi.eshop.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NullMarked
@Validated
@RestController
@AllArgsConstructor
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<CatalogResponse> list(
            @RequestParam(required = false) @Nullable String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(20) int size) {
        return ResponseEntity.ok(catalogService.list(search, page, size));
    }
}
