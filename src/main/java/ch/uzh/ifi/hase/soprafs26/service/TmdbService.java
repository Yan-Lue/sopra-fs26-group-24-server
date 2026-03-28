package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TmdbService {

    //Cache time, a movie is stored for 1h, after that a new request would be made to tmdb if needed
    private static final long CACHE_TTL_MILLIS = 60 * 60 * 1000L;

    private final RestClient restClient;
    private final String apiKey;
    private final String imageBaseUrl;
    private final Map<Long, CachedMovie> movieCache = new ConcurrentHashMap<>();

    public TmdbService(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.api-key}") String apiKey,
            @Value("${tmdb.image-base-url}") String imageBaseUrl
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.imageBaseUrl = imageBaseUrl;
    }

    public List<Long> discoverMovieIds(int amount) {

        //restClient builds, sends and receives requests and responses from external sites like tmdb
        DiscoverResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("include_adult", false)
                        .queryParam("language", "en-US")
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("vote_count.gte", 500)
                        .queryParam("page", 1)
                        .build())
                .retrieve()
                .body(DiscoverResponse.class);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TMDB did not return any movies");
        }

        List<Long> ids = response.results().stream()
                .map(DiscoverMovieResult::id)
                .distinct()
                .toList();

        if (ids.size() < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Not enough movies returned by TMDB");
        }

        List<Long> shuffeled = new ArrayList<>(ids);
        Collections.shuffle(shuffeled);

        return shuffeled.subList(0, amount);
    }

    public Movie getMovieDetails(Long movieId) {
        CachedMovie cachedMovie = movieCache.get(movieId);
        long now = System.currentTimeMillis();

        if (cachedMovie != null && cachedMovie.expiresAt() > now) {
            return cachedMovie.movie();
        }

        MovieDetailResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movieId}")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "en-US")
                        .build(movieId))
                .retrieve()
                .body(MovieDetailResponse.class);

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TMDB movie details could not be loaded");
        }

        Movie movie = new Movie(
                response.id(),
                response.title(),
                response.overview(),
                response.posterPath() == null ? null : imageBaseUrl + response.posterPath(),
                response.voteAverage(),
                response.releaseDate(),
                response.genres() == null ? List.of() : response.genres().stream().map(TmdbGenre::name).toList()
        );

        movieCache.put(movieId, new CachedMovie(movie, now + CACHE_TTL_MILLIS));
        return movie;
    }

    private record CachedMovie(Movie movie, long expiresAt) {
    }

    private record DiscoverResponse(List<DiscoverMovieResult> results) {
    }

    private record DiscoverMovieResult(long id) {
    }

    private record MovieDetailResponse(
            Long id,
            String title,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("vote_average") Double voteAverage,
            @JsonProperty("release_date") String releaseDate,
            List<TmdbGenre> genres
    ) {
    }

    private record TmdbGenre(Long id, String name) {
    }
}
