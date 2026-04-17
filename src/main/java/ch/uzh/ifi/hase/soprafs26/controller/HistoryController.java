package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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


    @GetMapping("/users/{userId}/histories/{historyId}")
    @ResponseStatus(HttpStatus.OK)
    public HistoryGetDTO getHistory(@PathVariable Long userId, @PathVariable Long historyId) {
        History history = historyService.getHistoryByHistoryId(userId, historyId);

        return DTOMapper.INSTANCE.convertEntityToHistoryGetDTO(history);
    }

    @GetMapping("/users/{userId}/histories")
    @ResponseStatus(HttpStatus.OK)
    public List<HistoryGetDTO> getAllHistoryOfUser(@PathVariable Long userId) {

        List<History> histories = historyService.getHistoriesOfUser(userId);
        List<HistoryGetDTO> historyGetDTOs = new ArrayList<>();

        for (History history : histories) {
            historyGetDTOs.add(DTOMapper.INSTANCE.convertEntityToHistoryGetDTO(history));
        }

        return historyGetDTOs;
    }

    @DeleteMapping("/users/{userId}/histories/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistoryEntry(@PathVariable Long userId, @PathVariable Long historyId) {
        historyService.deleteHistory(userId, historyId);
    }
}
