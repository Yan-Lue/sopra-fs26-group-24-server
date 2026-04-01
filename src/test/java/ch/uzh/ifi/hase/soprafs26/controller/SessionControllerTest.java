package ch.uzh.ifi.hase.soprafs26.controller;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.SessionService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(SessionController.class)
public class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    private Session testSession;

    @BeforeEach
    void setup() {
        // Initialize the object once for all tests
        testSession = new Session();
        testSession.setSessionId(1L);
        testSession.setSessionName("testSession");
        testSession.setSessionCode("test1234");
        testSession.setMaxPlayers(5);
        testSession.setRoundLimit(10);
        testSession.setCurrentMovieIndex(0);
        testSession.setHostId(1L);
        testSession.setStatus(SessionStatus.ONLINE);
        testSession.setCreationDate(new java.util.Date());
        testSession.setSessionToken("testSessionToken");
    }

    // When creating new Session correct Session Credentials get returned and Status
    // Created
    // POST /session 201
    @Test
    void createSession_validInput_sessionCreated() throws Exception {

        SessionPostDTO sessionPostDTO = new SessionPostDTO();
        sessionPostDTO.setSessionName("testSession");
        sessionPostDTO.setMaxPlayers(10);
        sessionPostDTO.setHostId(1L);

        given(sessionService.createSession(Mockito.any())).willReturn(testSession);
        MockHttpServletRequestBuilder postRequest = post("/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(sessionPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionCode", is(testSession.getSessionCode())))
                .andExpect(jsonPath("$.sessionId", is(testSession.getSessionId().intValue())))
                .andExpect(jsonPath("$.sessionToken", is(testSession.getSessionToken())));

    }

    // Session Creation not successful
    // POST /session 400
    @Test
    void createSession_InvalidInput_thenReturnBadRequest() throws Exception {
        SessionPostDTO sessionPostDTO = new SessionPostDTO();
        sessionPostDTO.setSessionName("testSession");
        sessionPostDTO.setMaxPlayers(10);

        given(sessionService.createSession(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Session"));

        MockHttpServletRequestBuilder postRequest = post("/session")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(sessionPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest()); // Match the status thrown above!
    }

    // Getting a session for joining successfully
    // GET /session/{sessionId} 200
    @Test
    void getSession_validSessionId_getSuccessful() throws Exception {

        given(sessionService.getSessionById(1L)).willReturn(testSession);

        MockHttpServletRequestBuilder getRequest = get("/session/1").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode", is(testSession.getSessionCode().toString())))
                .andExpect(jsonPath("$.sessionId", is(testSession.getSessionId().intValue())))
                .andExpect(jsonPath("$.sessionToken", is(testSession.getSessionToken().toString())));
    }

    // Getting a session for joining not successfully
    // GET /session/{sessionId} 404
    @Test
    public void getSession_invalidSessionId_thenReturnSessionNotFound() throws Exception {
        // no user needed as error will be thrown anyway
        given(sessionService.getSessionById(444L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found."));

        MockHttpServletRequestBuilder getRequest = get("/session/444").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
