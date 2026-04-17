package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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


    @GetMapping("/histories/{historyId}")
    @ResponseStatus(HttpStatus.OK)
    public HistoryGetDTO getHistory(@PathVariable Long historyId) {
        History history = historyService.getHistoryByHistoryId(historyId);

        return DTOMapper.INSTANCE.convertEntityToHistoryGetDTO(history);
    }
}
