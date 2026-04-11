package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.VotePutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

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

        Session createdSession = sessionService.createSession(newSession, sessionPostDTO.getToken());

        return DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(createdSession);
    }

    @PutMapping("/session/{sessionCode}")
    @ResponseStatus(HttpStatus.OK)
    public SessionGetDTO joinSession(@PathVariable String sessionCode, @RequestBody SessionPutDTO sessionPutDTO) {

        Session session = sessionService.joinSession(sessionCode, sessionPutDTO);

        return DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(session);
    }

    @GetMapping("/session/{sessionCode}/next")
    @ResponseStatus(HttpStatus.OK)
    public MovieGetDTO getNextMovie(@PathVariable String sessionCode) {
        Movie movie = sessionService.getNextMovie(sessionCode);

        return DTOMapper.INSTANCE.convertMovieGetDTOtoEntity(movie);
    }

    @GetMapping("/session/{sessionCode}/current")
    @ResponseStatus(HttpStatus.OK)
    public MovieGetDTO getCurrentMovie(@PathVariable String sessionCode) {
        Movie movie = sessionService.getCurrentMovie(sessionCode);

        return DTOMapper.INSTANCE.convertMovieGetDTOtoEntity(movie);
    }

    @PostMapping("/session/{sessionCode}/vote")
    public String setVote(@PathVariable String sessionCode, @RequestBody VotePutDTO votePutDTO) {

        sessionService.setVote(votePutDTO);

        return "Vote recorded successfully";
    }

    @PutMapping("/session/{sessionCode}/filters")
    @ResponseStatus(HttpStatus.OK)
    public SessionGetDTO updateSessionFilters(@PathVariable String sessionCode,
            @RequestBody SessionFilterPutDTO sessionFilterPutDTO) {
        Session updatedSession = sessionService.updateSessionFilters(sessionCode, sessionFilterPutDTO);
        return DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(updatedSession);
    }
}
