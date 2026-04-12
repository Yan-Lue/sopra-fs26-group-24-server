package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class MovieResultDTO {

    private Long movieId;
    private String title;
    private Integer score;
    private String posterPath;
    private String description;
    private Double rating;
    private String releaseDate;
    private List<String> genres;
    private List<SimilarMovieGetDTO> similarMovies;
    private Integer likes;
    private Integer dislikes;
    private Integer neutrals;

    public MovieResultDTO(Long movieId, String title, Integer score, String posterPath, String description, Double rating, String releaseDate, List<String> genres, List<SimilarMovieGetDTO> similarMovies, Integer likes, Integer dislikes, Integer neutrals) {
        this.movieId = movieId;
        this.title = title;
        this.score = score;
        this.posterPath = posterPath;
        this.description = description;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.genres = genres;
        this.similarMovies = similarMovies;
        this.likes = likes;
        this.dislikes = dislikes;
        this.neutrals = neutrals;
    }

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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getDislikes() {
        return dislikes;
    }

    public void setDislikes(Integer dislikes) {
        this.dislikes = dislikes;
    }

    public Integer getNeutrals() {
        return neutrals;
    }

    public void setNeutrals(Integer neutrals) {
        this.neutrals = neutrals;
    }

}
