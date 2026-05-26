package gr.unipi.eshop.catalog;

import gr.unipi.eshop.shared.PageMeta;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

        return new CatalogResponse(searched, products, pagination);
    }
}
