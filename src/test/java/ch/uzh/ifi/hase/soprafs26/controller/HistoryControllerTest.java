package ch.uzh.ifi.hase.soprafs26.controller;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private String asJsonString(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The requeset body could not be created.%s", e));
        }
    }
}
