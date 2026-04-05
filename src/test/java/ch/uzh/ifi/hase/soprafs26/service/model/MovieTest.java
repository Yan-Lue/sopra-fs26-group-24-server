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
                List.of("Drama"),
                List.of(new SimilarMovie(9L, "Related", "/related.jpg", 7.8, "2000-05-05"))
        );

        movie.setId(2L);
        movie.setTitle("Updated Title");
        movie.setOverview("Updated Overview");
        movie.setPosterPath("/updated.jpg");
        movie.setRating(8.5);
        movie.setReleaseDate("2002-02-02");
        movie.setGenres(List.of("Comedy", "Action"));
        movie.setSimilarMovies(List.of(new SimilarMovie(10L, "Other", "/other.jpg", 7.2, "2003-03-03")));

        assertEquals(2L, movie.getId());
        assertEquals("Updated Title", movie.getTitle());
        assertEquals("Updated Overview", movie.getOverview());
        assertEquals("/updated.jpg", movie.getPosterPath());
        assertEquals(8.5, movie.getRating());
        assertEquals("2002-02-02", movie.getReleaseDate());
        assertEquals(List.of("Comedy", "Action"), movie.getGenres());
        assertEquals(1, movie.getSimilarMovies().size());
        assertEquals(10L, movie.getSimilarMovies().get(0).getId());
    }
}
