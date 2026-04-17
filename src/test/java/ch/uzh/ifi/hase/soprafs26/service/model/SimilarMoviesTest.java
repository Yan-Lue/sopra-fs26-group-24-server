package ch.uzh.ifi.hase.soprafs26.service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarMoviesTest {

    @Test
    void testConstructorAndGetters() {
        SimilarMovie movie = new SimilarMovie(1L, "Inception",
                "/poster.jpg", 8.8, "2010-07-16");

        assertEquals(1L, movie.getId());
        assertEquals("Inception", movie.getTitle());
        assertEquals("/poster.jpg", movie.getPosterPath());
        assertEquals(8.8, movie.getRating());
        assertEquals("2010-07-16", movie.getReleaseDate());
    }

    @Test
    void testSetters() {
        SimilarMovie movie = new SimilarMovie(null, null, null, null, null);

        movie.setId(2L);
        movie.setTitle("Interstellar");
        movie.setPosterPath("/interstellar.jpg");
        movie.setRating(9.0);
        movie.setReleaseDate("2014-11-07");

        assertEquals(2L, movie.getId());
        assertEquals("Interstellar", movie.getTitle());
        assertEquals("/interstellar.jpg", movie.getPosterPath());
        assertEquals(9.0, movie.getRating());
        assertEquals("2014-11-07", movie.getReleaseDate());
    }
}
