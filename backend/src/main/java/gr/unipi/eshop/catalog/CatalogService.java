package gr.unipi.eshop.catalog;

import gr.unipi.eshop.shared.LogFields;
import gr.unipi.eshop.shared.PageMeta;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@NullMarked
@Service
@AllArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;

    public CatalogResponse list(@Nullable String search, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name"));
        boolean searched = search != null && !search.isBlank();
        String term = searched ? search : "";

        Page<Product> result = productRepository.findByNameContainingIgnoreCase(term, pageable);

        var products = result.getContent().stream()
                .map(ProductSummary::from)
                .toList();

        var pagination = new PageMeta(
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.hasNext(),
                result.isLast());

        log.atInfo()
                .addKeyValue(LogFields.Key.EVENT, LogFields.Event.CATALOG_LIST)
                .addKeyValue(LogFields.Key.SEARCH, searched ? search : "")
                .addKeyValue(LogFields.Key.PAGE, page)
                .addKeyValue(LogFields.Key.SIZE, size)
                .log("catalog list search={} page={} size={}", searched ? search : "", page, size);

        return new CatalogResponse(searched, term, products, pagination);
    }
}
