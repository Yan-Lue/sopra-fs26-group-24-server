package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/histories")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHistory(@RequestBody HistoryPostDTO historyPostDTO) {
        historyService.saveHistory(historyPostDTO);
    }

}
