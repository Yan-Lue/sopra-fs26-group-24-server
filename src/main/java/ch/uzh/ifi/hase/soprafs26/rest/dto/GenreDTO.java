package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GenreDTO {
    private String name;

    public GenreDTO() {
    }

    public GenreDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
