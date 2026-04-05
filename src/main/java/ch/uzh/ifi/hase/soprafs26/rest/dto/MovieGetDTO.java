package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class MovieGetDTO {
    private Long movieId;
    private String title;
    private String description;
    private String posterPath;
    private Double rating;
    private String releaseDate;
    private List<String> genres;
    private List<SimilarMovieGetDTO> similarMovies;

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<SimilarMovieGetDTO> getSimilarMovies() {
        return similarMovies;
    }

    public void setSimilarMovies(List<SimilarMovieGetDTO> similarMovies) {
        this.similarMovies = similarMovies;
    }
}
