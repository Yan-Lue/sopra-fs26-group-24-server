package ch.uzh.ifi.hase.soprafs26.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
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

import java.util.List;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

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
    // GET /session/{sessionCode} 200
    @Test
    void getSession_validSessionCode_getSuccessful() throws Exception {

<<<<<<< HEAD
        given(sessionService.getSessionById("test1234")).willReturn(testSession);
=======
        given(sessionService.getSessionByCode("test1234")).willReturn(testSession);
>>>>>>> parent of caa0b57 (Revert "22-once the users have joined the host is able to press a button  start session  that starts the session #134")

        MockHttpServletRequestBuilder getRequest = get("/session/test1234").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode", is(testSession.getSessionCode())))
                .andExpect(jsonPath("$.sessionId", is(testSession.getSessionId().intValue())))
                .andExpect(jsonPath("$.sessionToken", is(testSession.getSessionToken())));
    }

    
    // Getting a session for joining not successfully
    // GET /session/{sessionCode} 404
    @Test
<<<<<<< HEAD
    void getSession_invalidSessionId_thenReturnSessionNotFound() throws Exception {
=======
    public void getSession_invalidSessionCode_thenReturnSessionNotFound() throws Exception {
>>>>>>> parent of caa0b57 (Revert "22-once the users have joined the host is able to press a button  start session  that starts the session #134")
        // no user needed as error will be thrown anyway
        given(sessionService.getSessionByCode("doesNotExist"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found."));

        MockHttpServletRequestBuilder getRequest = get("/session/doesNotExist").contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }


    @Test
    void getNextMovie_validSessionId_returnsMovie() throws Exception {
        Movie movie = new Movie(
                550L,
                "Fight Club",
                "Insomnia and soap.",
                "https://image.tmdb.org/t/p/w500/fight-club.jpg",
                8.4,
                "1999-10-15",
                List.of("Drama", "Thriller")
        );

        given(sessionService.getNextMovie(1L)).willReturn(movie);

        MockHttpServletRequestBuilder getRequest = get("/session/1/next")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId", is(550)))
                .andExpect(jsonPath("$.title", is("Fight Club")))
                .andExpect(jsonPath("$.description", is("Insomnia and soap.")))
                .andExpect(jsonPath("$.posterPath", is("https://image.tmdb.org/t/p/w500/fight-club.jpg")))
                .andExpect(jsonPath("$.rating", is(8.4)))
                .andExpect(jsonPath("$.releaseDate", is("1999-10-15")))
                .andExpect(jsonPath("$.genres", hasSize(2)))
                .andExpect(jsonPath("$.genres[0]", is("Drama")))
                .andExpect(jsonPath("$.genres[1]", is("Thriller")));
    }

    @Test
    void getNextMovie_invalidSessionId_returnsNotFound() throws Exception {
        given(sessionService.getNextMovie(444L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found."));

        MockHttpServletRequestBuilder getRequest = get("/session/444/next")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e));
        }
    }
}
