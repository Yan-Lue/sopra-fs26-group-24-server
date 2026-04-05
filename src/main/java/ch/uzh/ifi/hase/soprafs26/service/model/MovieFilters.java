package ch.uzh.ifi.hase.soprafs26.service.model;

import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record MovieFilters(
        List<Long> genreIds,
        Double minRating,
        Integer releaseYear
) {
    private static final Map<String, Long> GENRE_NAME_TO_ID = Map.ofEntries(
            Map.entry("Action", 28L),
            Map.entry("Adventure", 12L),
            Map.entry("Animation", 16L),
            Map.entry("Comedy", 35L),
            Map.entry("Crime", 80L),
            Map.entry("Documentary", 99L),
            Map.entry("Drama", 18L),
            Map.entry("Family", 10751L),
            Map.entry("Fantasy", 14L),
            Map.entry("History", 36L),
            Map.entry("Horror", 27L),
            Map.entry("Music", 10402L),
            Map.entry("Mystery", 9648L),
            Map.entry("Romance", 10749L),
            Map.entry("Science Fiction", 878L),
            Map.entry("TV Movie", 10770L),
            Map.entry("Thriller", 53L),
            Map.entry("War", 10752L),
            Map.entry("Western", 37L)
    );

    public static MovieFilters fromDTO(SessionFilterPutDTO dto) {
        List<Long> genreIds = dto.getGenres() == null ? List.of() : dto.getGenres()
                                                                    .stream()
                                                                    .map(GENRE_NAME_TO_ID::get)
                                                                    .filter(Objects::nonNull)
                                                                    .toList();
        return new MovieFilters(genreIds, dto.getMinRating(), dto.getReleaseYear());
    }
}

