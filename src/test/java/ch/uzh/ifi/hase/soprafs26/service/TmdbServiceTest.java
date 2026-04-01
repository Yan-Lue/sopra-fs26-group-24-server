package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TmdbServiceTest {

    private HttpServer server;
    private TmdbService tmdbService;
    private final AtomicInteger discoverCalls = new AtomicInteger();
    private final AtomicInteger movieCalls = new AtomicInteger();

    private String discoverResponse = "{\"results\":[]}";
    private String movieResponse = "null";

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/discover/movie", exchange -> {
            discoverCalls.incrementAndGet();
            sendJson(exchange, discoverResponse);
        });

        server.createContext("/movie", exchange -> {
            movieCalls.incrementAndGet();
            sendJson(exchange, movieResponse);
        });

        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        tmdbService = new TmdbService(baseUrl, "test-key", "https://image.tmdb.org/t/p/w500");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void discoverMovieIds_returnsDistinctSubsetOfRequestedSize() {
        discoverResponse = """
                {
                  "results": [
                    { "id": 1 },
                    { "id": 2 },
                    { "id": 2 },
                    { "id": 3 }
                  ]
                }
                """;

        List<Long> ids = tmdbService.discoverMovieIds(2);

        assertEquals(2, ids.size());
        assertEquals(2, ids.stream().distinct().count());
        assertTrue(Set.of(1L, 2L, 3L).containsAll(ids));
        assertEquals(1, discoverCalls.get());
    }

    @Test
    void discoverMovieIds_whenNoMoviesReturned_throwsBadGateway() {
        discoverResponse = """
                {
                  "results": []
                }
                """;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tmdbService.discoverMovieIds(1)
        );

        assertEquals(502, exception.getStatusCode().value());
        assertEquals("TMDB did not return any movies", exception.getReason());
    }

    @Test
    void discoverMovieIds_whenTooFewDistinctMovies_throwsBadGateway() {
        discoverResponse = """
                {
                  "results": [
                    { "id": 1 },
                    { "id": 1 }
                  ]
                }
                """;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tmdbService.discoverMovieIds(2)
        );

        assertEquals(502, exception.getStatusCode().value());
        assertEquals("Not enough movies returned by TMDB", exception.getReason());
    }

    @Test
    void getMovieDetails_mapsFieldsAndCachesResult() {
        movieResponse = """
                {
                  "id": 550,
                  "title": "Fight Club",
                  "overview": "Insomnia and soap.",
                  "poster_path": "/fight-club.jpg",
                  "vote_average": 8.4,
                  "release_date": "1999-10-15",
                  "genres": [
                    { "id": 18, "name": "Drama" },
                    { "id": 53, "name": "Thriller" }
                  ]
                }
                """;

        Movie first = tmdbService.getMovieDetails(550L);
        Movie second = tmdbService.getMovieDetails(550L);

        assertEquals(550L, first.getId());
        assertEquals("Fight Club", first.getTitle());
        assertEquals("Insomnia and soap.", first.getOverview());
        assertEquals("https://image.tmdb.org/t/p/w500/fight-club.jpg", first.getPosterPath());
        assertEquals(8.4, first.getRating());
        assertEquals("1999-10-15", first.getReleaseDate());
        assertEquals(List.of("Drama", "Thriller"), first.getGenres());

        assertEquals(first, second);
        assertEquals(1, movieCalls.get());
    }

    @Test
    void getMovieDetails_whenPosterAndGenresMissing_handlesNullValues() {
        movieResponse = """
                {
                  "id": 42,
                  "title": "Unknown",
                  "overview": "No poster.",
                  "poster_path": null,
                  "vote_average": 6.5,
                  "release_date": "2000-01-01",
                  "genres": null
                }
                """;

        Movie movie = tmdbService.getMovieDetails(42L);

        assertEquals(42L, movie.getId());
        assertNull(movie.getPosterPath());
        assertEquals(List.of(), movie.getGenres());
    }

    @Test
    void getMovieDetails_whenBodyIsNull_throwsBadGateway() {
        movieResponse = "null";

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tmdbService.getMovieDetails(77L)
        );

        assertEquals(502, exception.getStatusCode().value());
        assertEquals("TMDB movie details could not be loaded", exception.getReason());
    }

    private void sendJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
