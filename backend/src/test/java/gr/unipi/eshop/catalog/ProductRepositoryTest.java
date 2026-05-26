package gr.unipi.eshop.catalog;

import gr.unipi.eshop.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRepositoryTest extends BaseIntegrationTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    ProductRepository repo;

    @Test
    void findByNameContainingIgnoreCase_sqlInjectionReturnsEmpty() {
        assertThat(repo.findByNameContainingIgnoreCase("' OR '1'='1", FIRST_PAGE)).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_matchesSubstring() {
        assertThat(repo.findByNameContainingIgnoreCase("Keyboard", FIRST_PAGE)).isNotEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_noMatchReturnsEmpty() {
        assertThat(repo.findByNameContainingIgnoreCase("zzznotexisting", FIRST_PAGE)).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_wildcardCharactersMatchLiterally() {
        assertThat(repo.findByNameContainingIgnoreCase("%%", FIRST_PAGE)).isEmpty();
    }
}
