package gr.unipi.eshop.web;

public record PageMeta(int page, int totalPages, long totalElements, boolean hasNext, boolean isLast) {
}
