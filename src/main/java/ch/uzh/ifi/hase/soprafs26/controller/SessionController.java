package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieResultDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.VotePutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;

import org.springframework.http.HttpStatus;
import java.util.*;

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

        SessionGetDTO dto = DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(createdSession);
        dto.setUsernames(sessionService.getJoinedUsernames(createdSession));
        return dto;
    }

    @PutMapping("/session/{sessionCode}")
    @ResponseStatus(HttpStatus.OK)
    public SessionGetDTO joinSession(@PathVariable String sessionCode, @RequestBody SessionPutDTO sessionPutDTO) {

        Session session = sessionService.joinSession(sessionCode, sessionPutDTO);

        SessionGetDTO dto = DTOMapper.INSTANCE.convertEntitytoSessionGetDTO(session);
        dto.setUsernames(sessionService.getJoinedUsernames(session));
        return dto;
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

    @GetMapping("/session/{sessionCode}/results")
    @ResponseStatus(HttpStatus.OK)
    public List<MovieResultDTO> getSessionResults(@PathVariable String sessionCode) {
        // You don't need a PostDTO here; the sessionCode is enough
        // to find the votes in the database.
        return sessionService.calculateFullLeaderboard(sessionCode);
    }

    @GetMapping("/session/{sessionCode}/time")
    @ResponseStatus(HttpStatus.OK)
    public Integer getSessionTime(@PathVariable String sessionCode) {
        return sessionService.getSessionTiming(sessionCode);
    }

    @DeleteMapping("/session/{sessionCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveSession(@PathVariable String sessionCode, @RequestParam String token) {
        sessionService.leaveSession(sessionCode, token);
    }
}
