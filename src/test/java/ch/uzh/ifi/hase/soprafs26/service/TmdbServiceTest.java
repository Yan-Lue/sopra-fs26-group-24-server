package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
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
    private final AtomicInteger similarMovieCalls = new AtomicInteger();

    private String discoverResponse = "{\"results\":[]}";
    private String movieResponse = "null";
    private String similarMovieResponse = "{\"results\":[]}";
    private URI lastDiscoverUri;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/discover/movie", exchange -> {
            discoverCalls.incrementAndGet();
            lastDiscoverUri = exchange.getRequestURI();
            sendJson(exchange, discoverResponse);
        });

        server.createContext("/movie", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/similar")) {
                similarMovieCalls.incrementAndGet();
                sendJson(exchange, similarMovieResponse);
                return;
            }

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

        List<Long> ids = tmdbService.discoverMovieIds(2, null);

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
                () -> tmdbService.discoverMovieIds(1, null)
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
                () -> tmdbService.discoverMovieIds(2, null)
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
        similarMovieResponse = """
                {
                  "results": [
                    {
                      "id": 551,
                      "title": "Se7en",
                      "poster_path": "/se7en.jpg",
                      "vote_average": 8.3,
                      "release_date": "1995-09-22"
                    }
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
        assertEquals(1, first.getSimilarMovies().size());
        assertEquals(551L, first.getSimilarMovies().get(0).getId());
        assertEquals("Se7en", first.getSimilarMovies().get(0).getTitle());
        assertEquals("https://image.tmdb.org/t/p/w500/se7en.jpg", first.getSimilarMovies().get(0).getPosterPath());
        assertEquals(8.3, first.getSimilarMovies().get(0).getRating());
        assertEquals("1995-09-22", first.getSimilarMovies().get(0).getReleaseDate());

        assertEquals(first, second);
        assertEquals(1, movieCalls.get());
        assertEquals(1, similarMovieCalls.get());
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
        similarMovieResponse = "null";

        Movie movie = tmdbService.getMovieDetails(42L);

        assertEquals(42L, movie.getId());
        assertNull(movie.getPosterPath());
        assertEquals(List.of(), movie.getGenres());
        assertEquals(List.of(), movie.getSimilarMovies());
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

    @Test
    void discoverMovieIds_withFilters_addsExpectedQueryParams() {
        discoverResponse = """
            {
              "results": [
                { "id": 1 },
                { "id": 2 },
                { "id": 3 }
              ]
            }
            """;

        MovieFilters filters = new MovieFilters(List.of(28L, 10749L), 7.5, 2024);

        List<Long> ids = tmdbService.discoverMovieIds(2, filters);

        assertEquals(2, ids.size());
        assertTrue(lastDiscoverUri.getQuery().contains("with_genres=28|10749"));
        assertTrue(lastDiscoverUri.getQuery().contains("vote_average.gte=7.5"));
        assertTrue(lastDiscoverUri.getQuery().contains("primary_release_year=2024"));
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
