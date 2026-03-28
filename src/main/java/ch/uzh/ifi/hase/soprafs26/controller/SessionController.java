package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;

import org.springframework.http.HttpStatus;

@RestController
public class SessionController {

    private final SessionService sessionService;

    SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/session")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionGetDTO createSession(@RequestBody SessionPostDTO sessionPostDTO) {

        Session newSession = DTOMapper.INSTANCE.convertSessionPostDTOtoEntity(sessionPostDTO);

        Session createdSession = sessionService.createSession(newSession);

        return DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(createdSession);
    }

    @GetMapping("/session/{sessionId}/next")
    @ResponseStatus(HttpStatus.OK)
    public MovieGetDTO getNextMovie(@PathVariable Long sessionId) {
        Movie movie = sessionService.getNextMovie(sessionId);

        return DTOMapper.INSTANCE.convertMovieGetDTOtoEntity(movie);
    }
}
