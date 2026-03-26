package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

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

}
