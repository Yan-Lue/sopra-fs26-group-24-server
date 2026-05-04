package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class SessionFilterPutDTO {
    private Integer roundLimit;
    private List<String> genres;
    private Double minRating;
    private Integer minReleaseYear;
    private Integer maxReleaseYear;
    private Integer timePerRound;
    private List<String> providers;

    public Integer getRoundLimit() {
        return roundLimit;
    }

    public void setRoundLimit(Integer roundLimit) {
        this.roundLimit = roundLimit;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Double getMinRating() {
        return minRating;
    }

    public void setMinRating(Double minRating) {
        this.minRating = minRating;
    }

    public Integer getMinReleaseYear() {
        return minReleaseYear;
    }

    public void setMinReleaseYear(Integer minReleaseYear) {
        this.minReleaseYear = minReleaseYear;
    }

    public Integer getMaxReleaseYear() {
        return maxReleaseYear;
    }

    public void setMaxReleaseYear(Integer maxReleaseYear) {
        this.maxReleaseYear = maxReleaseYear;
    }

    public Integer getTimePerRound() {
        return timePerRound;
    }

    public void setTimePerRound(Integer timePerRound) {
        this.timePerRound = timePerRound;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers;
    }
}
