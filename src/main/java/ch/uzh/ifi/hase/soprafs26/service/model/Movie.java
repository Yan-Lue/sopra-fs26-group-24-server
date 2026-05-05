package ch.uzh.ifi.hase.soprafs26.service.model;

import java.util.List;

public class Movie {

    private Long id;
    private String title;
    private String overview;
    private String posterPath;
    private Double rating;
    private String releaseDate;
    private List<String> genres;
    private List<SimilarMovie> similarMovies;
    private List<String> streamingProviders;

    public Movie(Long id, String title, String overview, String posterPath,
                 Double rating, String releaseDate, List<String> genres, List<SimilarMovie> similarMovies, List<String> streamingProviders) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.posterPath = posterPath;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.genres = genres;
        this.similarMovies = similarMovies;
        this.streamingProviders = streamingProviders;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<SimilarMovie> getSimilarMovies() {
        return similarMovies;
    }

    public void setSimilarMovies(List<SimilarMovie> similarMovies) {
        this.similarMovies = similarMovies;
    }

    public List<String> getStreamingProviders() {
        return streamingProviders;
    }

    public void setStreamingProviders(List<String> streamingProviders) {
        this.streamingProviders = streamingProviders;
    }
}