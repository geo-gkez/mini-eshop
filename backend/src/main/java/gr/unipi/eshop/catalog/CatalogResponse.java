package gr.unipi.eshop.catalog;

import gr.unipi.eshop.web.PageMeta;

import java.util.List;

public record CatalogResponse(boolean searchActive,
                              List<ProductSummary> products,
                              PageMeta pagination) {
}
