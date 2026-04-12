package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import ch.uzh.ifi.hase.soprafs26.service.model.SimilarMovie;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SimilarMovieGetDTO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TmdbService {

        // Cache time, a movie is stored for 1h, after that a new request would be made
        // to tmdb if needed
        private static final long CACHE_TTL_MILLIS = 60 * 60 * 1000L;

        private final RestClient restClient;
        private final String apiKey;
        private final String imageBaseUrl;
        private final Map<Long, CachedMovie> movieCache = new ConcurrentHashMap<>();

        public TmdbService(
                        @Value("${tmdb.base-url}") String baseUrl,
                        @Value("${tmdb.api-key}") String apiKey,
                        @Value("${tmdb.image-base-url}") String imageBaseUrl) {

                this.restClient = RestClient.builder()
                                .baseUrl(baseUrl)
                                .build();
                this.apiKey = apiKey;
                this.imageBaseUrl = imageBaseUrl;
        }

        public List<Long> discoverMovieIds(int amount, MovieFilters filters) {

                // restClient builds, sends and receives requests and responses from external
                // sites like tmdb
                DiscoverResponse response = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/discover/movie")
                                                .queryParam("api_key", apiKey)
                                                .queryParam("include_adult", false)
                                                .queryParam("language", "en-US")
                                                .queryParam("sort_by", "popularity.desc")
                                                .queryParamIfPresent("with_genres", buildGenre(filters))
                                                .queryParamIfPresent("vote_average.gte",
                                                                filters == null ? Optional.empty()
                                                                                : Optional.ofNullable(
                                                                                                filters.minRating()))
                                                .queryParamIfPresent("primary_release_year",
                                                                filters == null ? Optional.empty()
                                                                                : Optional.ofNullable(
                                                                                                filters.releaseYear()))
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

                List<Long> shuffled = new ArrayList<>(ids);
                Collections.shuffle(shuffled);

                return shuffled.subList(0, amount);
        }

        private Optional<String> buildGenre(MovieFilters filters) {
                if (filters == null || filters.genreIds() == null || filters.genreIds().isEmpty()) {
                        return Optional.empty();
                }

                String genres = filters.genreIds().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining("|"));

                return Optional.of(genres);
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
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                        "TMDB movie details could not be loaded");
                }

                SimilarMovieResponse similarMovieResponse = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/movie/{movieId}/similar")
                                                .queryParam("api_key", apiKey)
                                                .queryParam("language", "en-US")
                                                .queryParam("page", 1)
                                                .build(movieId))
                                .retrieve()
                                .body(SimilarMovieResponse.class);

                Movie movie = new Movie(
                                response.id(),
                                response.title(),
                                response.overview(),
                                response.posterPath() == null ? null : imageBaseUrl + response.posterPath(),
                                response.voteAverage(),
                                response.releaseDate(),
                                response.genres() == null ? List.of()
                                                : response.genres().stream().map(TmdbGenre::name).toList(),
                                mapSimilarMovies(similarMovieResponse));

                movieCache.put(movieId, new CachedMovie(movie, now + CACHE_TTL_MILLIS));
                return movie;
        }

        public List<MovieResultDTO> getMovieResults(Map<Long, Integer> movieScores) {
                List<MovieResultDTO> results = new ArrayList<>();

                // as instructions if you want to change the fields
                // this retrieves one entry consisting of (movieId, score) out of the map
                for (Map.Entry<Long, Integer> entry : movieScores.entrySet()) {
                        Long movieId = entry.getKey();
                        Integer score = entry.getValue();

                        // uses the function getMovieDetails to get the details out of the chache
                        Movie movie = getMovieDetails(movieId);
                        List<SimilarMovieGetDTO> similarMovieDTOs =
                        movie.getSimilarMovies() == null
                                ? List.of()
                                : movie.getSimilarMovies().stream()
                                        .map(ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper.INSTANCE::convertSimilarMovieToDTO)
                                        .toList();
                                        
                        results.add(new MovieResultDTO(movie.getId(), movie.getTitle(), score, movie.getPosterPath(), movie.getOverview(), movie.getRating(), movie.getReleaseDate(), movie.getGenres(), similarMovieDTOs, null, null, null));
                }

                return results;
        }

        private List<SimilarMovie> mapSimilarMovies(SimilarMovieResponse response) {
                if (response == null || response.results() == null) {
                        return List.of();
                }

                return response.results().stream()
                                .map(result -> new SimilarMovie(
                                                result.id(),
                                                result.title(),
                                                result.posterPath() == null ? null : imageBaseUrl + result.posterPath(),
                                                result.voteAverage(),
                                                result.releaseDate()))
                                .toList();
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
                        List<TmdbGenre> genres) {
        }

        private record TmdbGenre(Long id, String name) {
        }

        private record SimilarMovieResponse(List<SimilarMovieResult> results) {
        }

        private record SimilarMovieResult(
                        Long id,
                        String title,
                        @JsonProperty("poster_path") String posterPath,
                        @JsonProperty("vote_average") Double voteAverage,
                        @JsonProperty("release_date") String releaseDate) {
        }
}
