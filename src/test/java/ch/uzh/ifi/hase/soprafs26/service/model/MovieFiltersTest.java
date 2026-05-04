package ch.uzh.ifi.hase.soprafs26.service.model;

import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class MovieFiltersTest {

    @Test
    void fromDTO_mapsGenresToTmdbIds() {
        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setGenres(List.of("Action", "Romance"));
        dto.setMinRating(7.5);
        dto.setMinReleaseYear(2024);
        dto.setMaxReleaseYear(2025);

        MovieFilters filters = MovieFilters.fromDTO(dto);

        assertEquals(List.of(28L, 10749L), filters.genreIds());
        assertEquals(7.5, filters.minRating());
        assertEquals(LocalDate.of(2024,1,1), filters.minReleaseYear());
        assertEquals(LocalDate.of(2025, 12, 31), filters.maxReleaseYear());
    }

    @Test
    void fromDTO_ignoresUnknownGenres() {
        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setGenres(List.of("Action", "DoesNotExist"));

        MovieFilters filters = MovieFilters.fromDTO(dto);

        assertEquals(List.of(28L), filters.genreIds());
    }

    @Test
    void fromDTO_withoutFilters_returnsEmptyGenreIdsAndNullOptionals() {
        SessionFilterPutDTO dto = new SessionFilterPutDTO();

        MovieFilters filters = MovieFilters.fromDTO(dto);

        assertEquals(List.of(), filters.genreIds());
        assertNull(filters.minRating());
        assertNull(filters.minReleaseYear());
    }
}
