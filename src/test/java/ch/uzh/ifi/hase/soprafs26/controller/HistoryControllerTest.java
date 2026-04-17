package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.entity.HistoryMovieEntry;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@WebMvcTest(HistoryController.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoryService historyService;

    private HistoryPostDTO historyPostDTO;

    @BeforeEach
    void setup() {
        historyPostDTO = new HistoryPostDTO();
        historyPostDTO.setSessionCode("ABCDE");
        historyPostDTO.setToken("test-token");
    }

    @Test
    void saveHistory_validInput_returnsCreated() throws Exception {
        Mockito.doNothing().when(historyService).saveHistory(any(HistoryPostDTO.class));

        MockHttpServletRequestBuilder postRequest = post("/histories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(historyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated());
    }

    @Test
    void saveHistory_unknownSession_returnsNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found."))
                .when(historyService).saveHistory(any(HistoryPostDTO.class));

        MockHttpServletRequestBuilder postRequest = post("/histories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(historyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void saveHistory_guestToken_returnsForbidden() throws Exception {
        historyPostDTO.setToken("Guest-123");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Please register as User to save history."))
                .when(historyService).saveHistory(any(HistoryPostDTO.class));

        MockHttpServletRequestBuilder postRequest = post("/histories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(historyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void saveHistory_duplicateSave_returnsConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "History already saved"))
                .when(historyService).saveHistory(any(HistoryPostDTO.class));

        MockHttpServletRequestBuilder postRequest = post("/histories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(historyPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void getHistory_validInput_returnsJsonObject() throws Exception {

        History history = new History();
        history.setSessionCode("ABCDE");
        history.setHistoryId(1L);
        history.setCreationDate(new Date());
        history.setSessionName("Test Round");
        history.setJoinedUsers(3);
        history.setUserId(7L);
        history.setMovies(List.of());

        when(historyService.getHistoryByHistoryId(1L)).thenReturn(history);

        MockHttpServletRequestBuilder getRequest = get("/histories/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historyId", is(1)))
                .andExpect(jsonPath("$.sessionCode", is("ABCDE")))
                .andExpect(jsonPath("$.sessionName", is("Test Round")))
                .andExpect(jsonPath("$.joinedUsers", is(3)))
                .andExpect(jsonPath("$.userId", is(7)))
                .andExpect(jsonPath("$.movies", is(List.of())));
    }

    @Test
    void getHistory_unknownSession_returnsNotFound() throws Exception {
        when(historyService.getHistoryByHistoryId(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found."));

        MockHttpServletRequestBuilder getRequest = get("/histories/1");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistory_invalidId_returnsBadRequest() throws Exception {

        MockHttpServletRequestBuilder getRequest = get("/histories/abc");

        mockMvc.perform(getRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_withMovies_returnsJson() throws Exception {
        HistoryMovieEntry entry = new HistoryMovieEntry();
        entry.setMovieId(10L);
        entry.setScore(5);

        History history = new History();
        history.setSessionCode("ABCDE");
        history.setHistoryId(1L);
        history.setCreationDate(new Date());
        history.setSessionName("Test Round");
        history.setJoinedUsers(3);
        history.setUserId(7L);
        history.setMovies(List.of(entry));

        when(historyService.getHistoryByHistoryId(1L)).thenReturn(history);

        MockHttpServletRequestBuilder getRequest = get("/histories/1");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies[0].movieId", is(10)))
                .andExpect(jsonPath("$.movies[0].score", is(5)));
    }

    private String asJsonString(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e));
        }
    }
}
