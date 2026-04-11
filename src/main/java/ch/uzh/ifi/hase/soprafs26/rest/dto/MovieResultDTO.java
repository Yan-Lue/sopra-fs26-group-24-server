package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class MovieResultDTO {

    private Long movieId;
    private String title;
    private Integer score;
    private String posterPath;

    public MovieResultDTO(Long movieId, String title, Integer score, String posterPath) {
        this.movieId = movieId;
        this.title = title;
        this.score = score;
        this.posterPath = posterPath;
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

}
