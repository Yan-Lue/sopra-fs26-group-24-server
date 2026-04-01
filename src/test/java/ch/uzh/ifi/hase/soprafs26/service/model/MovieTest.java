package ch.uzh.ifi.hase.soprafs26.service.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovieTest {

    @Test
    void constructorAndSetters_populateAllFields() {
        Movie movie = new Movie(
                1L,
                "Initial Title",
                "Initial Overview",
                "/initial.jpg",
                7.0,
                "2001-01-01",
                List.of("Drama")
        );

        movie.setId(2L);
        movie.setTitle("Updated Title");
        movie.setOverview("Updated Overview");
        movie.setPosterPath("/updated.jpg");
        movie.setRating(8.5);
        movie.setReleaseDate("2002-02-02");
        movie.setGenres(List.of("Comedy", "Action"));

        assertEquals(2L, movie.getId());
        assertEquals("Updated Title", movie.getTitle());
        assertEquals("Updated Overview", movie.getOverview());
        assertEquals("/updated.jpg", movie.getPosterPath());
        assertEquals(8.5, movie.getRating());
        assertEquals("2002-02-02", movie.getReleaseDate());
        assertEquals(List.of("Comedy", "Action"), movie.getGenres());
    }
}
