package gr.unipi.eshop.catalog;

import gr.unipi.eshop.shared.PageMeta;

import java.util.List;

public record CatalogResponse(boolean searchActive,
                              List<ProductSummary> products,
                              PageMeta pagination) {
}
