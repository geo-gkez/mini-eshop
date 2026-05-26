package gr.unipi.eshop.shared;

public record PageMeta(int page,
                       int totalPages,
                       long totalElements,
                       boolean hasNext,
                       boolean isLast) {
}
