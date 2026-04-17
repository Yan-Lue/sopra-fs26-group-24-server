package ch.uzh.ifi.hase.soprafs26.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.SimilarMovie;
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
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPutDTO;
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

                given(sessionService.createSession(Mockito.any(), Mockito.any())).willReturn(testSession);
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

                given(sessionService.createSession(Mockito.any(), Mockito.any()))
                                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "Failed to create Session"));

                MockHttpServletRequestBuilder postRequest = post("/session")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(sessionPostDTO));

                mockMvc.perform(postRequest)
                                .andExpect(status().isBadRequest()); // Match the status thrown above!
        }

        // Getting a session for joining successfully
        // PUT /session/{sessionCode} 200
        @Test
        void joinSession_validSessionCode_getSuccessful() throws Exception {

                given(sessionService.joinSession(anyString(), any(SessionPutDTO.class))).willReturn(testSession);

                SessionPutDTO sessionPutDTO = new SessionPutDTO();
                sessionPutDTO.setToken("userToken");
                sessionPutDTO.setId(1L);

                MockHttpServletRequestBuilder putRequest = put("/session/test1234")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(sessionPutDTO));

                mockMvc.perform(putRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.sessionCode", is(testSession.getSessionCode())))
                                .andExpect(jsonPath("$.sessionId", is(1)))
                                .andExpect(jsonPath("$.sessionToken", is(testSession.getSessionToken())));
        }

        // Getting a session for joining not successfully
        // PUT /session/{sessionCode} 404
        @Test
        void getSession_invalidSessionCode_thenReturnSessionNotFound() throws Exception {
                // no user needed as error will be thrown anyway
                given(sessionService.joinSession(anyString(), any(SessionPutDTO.class)))
                                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Session could not be found."));

                SessionPutDTO sessionPutDTO = new SessionPutDTO();
                sessionPutDTO.setToken("userToken");
                sessionPutDTO.setId(1L);

                MockHttpServletRequestBuilder getRequest = put("/session/doesNotExist")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(sessionPutDTO));

                mockMvc.perform(getRequest)
                                .andExpect(status().isNotFound());
        }

        @Test
        void getNextMovie_validSessionCode_returnsMovie() throws Exception {
                Movie movie = new Movie(
                                550L,
                                "Fight Club",
                                "Insomnia and soap.",
                                "https://image.tmdb.org/t/p/w500/fight-club.jpg",
                                8.4,
                                "1999-10-15",
                                List.of("Drama", "Thriller"),
                                List.of(new SimilarMovie(
                                                551L,
                                                "Se7en",
                                                "https://image.tmdb.org/t/p/w500/se7en.jpg",
                                                8.3,
                                                "1995-09-22")));

                given(sessionService.getNextMovie("1")).willReturn(movie);

                MockHttpServletRequestBuilder getRequest = get("/session/1/next")
                                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.movieId", is(550)))
                                .andExpect(jsonPath("$.title", is("Fight Club")))
                                .andExpect(jsonPath("$.description", is("Insomnia and soap.")))
                                .andExpect(jsonPath("$.posterPath",
                                                is("https://image.tmdb.org/t/p/w500/fight-club.jpg")))
                                .andExpect(jsonPath("$.rating", is(8.4)))
                                .andExpect(jsonPath("$.releaseDate", is("1999-10-15")))
                                .andExpect(jsonPath("$.genres", hasSize(2)))
                                .andExpect(jsonPath("$.genres[0]", is("Drama")))
                                .andExpect(jsonPath("$.genres[1]", is("Thriller")))
                                .andExpect(jsonPath("$.similarMovies", hasSize(1)))
                                .andExpect(jsonPath("$.similarMovies[0].movieId", is(551)))
                                .andExpect(jsonPath("$.similarMovies[0].title", is("Se7en")))
                                .andExpect(jsonPath("$.similarMovies[0].posterPath",
                                                is("https://image.tmdb.org/t/p/w500/se7en.jpg")))
                                .andExpect(jsonPath("$.similarMovies[0].rating", is(8.3)))
                                .andExpect(jsonPath("$.similarMovies[0].releaseDate", is("1995-09-22")));
        }

        @Test
        void getNextMovie_invalidSessionCode_returnsNotFound() throws Exception {
                given(sessionService.getNextMovie("444"))
                                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Session could not be found."));

                MockHttpServletRequestBuilder getRequest = get("/session/444/next")
                                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                                .andExpect(status().isNotFound());
        }

        @Test
        void updateSessionFilters_validInput_returnsOk() throws Exception {
                SessionFilterPutDTO dto = new SessionFilterPutDTO();
                dto.setRoundLimit(10);
                dto.setGenres(List.of("Action", "Romance"));
                dto.setMinRating(7.5);
                dto.setReleaseYear(2024);

                given(sessionService.updateSessionFilters(eq("test1234"), any(SessionFilterPutDTO.class)))
                                .willReturn(testSession);

                MockHttpServletRequestBuilder putRequest = put("/session/test1234/filters")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(dto));

                mockMvc.perform(putRequest)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.sessionCode", is(testSession.getSessionCode())))
                                .andExpect(jsonPath("$.sessionId", is(1)))
                                .andExpect(jsonPath("$.sessionToken", is(testSession.getSessionToken())));
        }

        @Test
        void updateSessionFilters_unknownSession_returnsNotFound() throws Exception {
                SessionFilterPutDTO dto = new SessionFilterPutDTO();
                dto.setRoundLimit(10);
                dto.setGenres(List.of("Action"));

                given(sessionService.updateSessionFilters(eq("missing"), any(SessionFilterPutDTO.class)))
                                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Session could not be found."));

                MockHttpServletRequestBuilder putRequest = put("/session/missing/filters")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(dto));

                mockMvc.perform(putRequest)
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

        @Test
        void getSessionResults_validSessionCode_returnsDetailedResults() throws Exception {
                MovieResultDTO dto = new MovieResultDTO(
                        550L,
                        "Fight Club",
                        8,
                        "https://someImagetoMovie.jpg",
                        "Insomnia and soap.",
                        8.4,
                        "1999-10-15",
                        List.of("Drama", "Thriller"),
                        List.of(),
                        5,
                        1,
                        2
                );

                given(sessionService.calculateFullLeaderboard("test1234")).willReturn(List.of(dto));

                MockHttpServletRequestBuilder getRequest = get("/session/test1234/results")
                        .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].movieId", is(550)))
                        .andExpect(jsonPath("$[0].title", is("Fight Club")))
                        .andExpect(jsonPath("$[0].score", is(8)))
                        .andExpect(jsonPath("$[0].posterPath", is("https://someImagetoMovie.jpg")))
                        .andExpect(jsonPath("$[0].description", is("Insomnia and soap.")))
                        .andExpect(jsonPath("$[0].rating", is(8.4)))
                        .andExpect(jsonPath("$[0].releaseDate", is("1999-10-15")))
                        .andExpect(jsonPath("$[0].genres", hasSize(2)))
                        .andExpect(jsonPath("$[0].likes", is(5)))
                        .andExpect(jsonPath("$[0].dislikes", is(1)))
                        .andExpect(jsonPath("$[0].neutrals", is(2)));
        }

        @Test
        void getSessionResults_invalidSessionCode_returnsNotFound() throws Exception {

                given(sessionService.calculateFullLeaderboard("random"))
                                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Session could not be found."));

                MockHttpServletRequestBuilder getRequest = get("/session/random/results")
                                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                                .andExpect(status().isNotFound());
        }

        @Test
        void getSessionTime_validSessionCode_returnsOk() throws Exception {
            given(sessionService.getSessionTiming("test123")).willReturn(30);

            MockHttpServletRequestBuilder getRequest = get("/session/test123/time")
                    .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(getRequest)
                    .andExpect(status().isOk())
                    .andExpect(content().string("30"));
        }

        @Test
        void getCurrentMovie_validSessionCode_returnsMovie() throws Exception {
                Movie movie = new Movie(
                550L,
                "Fight Club",
                "Insomnia and soap.",
                "https://image.tmdb.org/t/p/w500/fight-club.jpg",
                8.4,
                "1999-10-15",
                List.of("Drama", "Thriller"),
                List.of(new SimilarMovie(
                551L,
                "Se7en",
                "https://image.tmdb.org/t/p/w500/se7en.jpg",
                8.3,
                "1995-09-22")));

                given(sessionService.getCurrentMovie("1")).willReturn(movie);

                MockHttpServletRequestBuilder getRequest = get("/session/1/current")
                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId", is(550)))
                .andExpect(jsonPath("$.title", is("Fight Club")))
                .andExpect(jsonPath("$.description", is("Insomnia and soap.")));
        }

        @Test
        void getCurrentMovie_invalidSessionCode_returnsNotFound() throws Exception {
                given(sessionService.getCurrentMovie("404"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Session could not be found."));

                MockHttpServletRequestBuilder getRequest = get("/session/404/current")
                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
        }

        @Test
        void getCurrentMovie_notStarted_returnsConflict() throws Exception {
                given(sessionService.getCurrentMovie("nostart"))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "Session has not started yet"));

                MockHttpServletRequestBuilder getRequest = get("/session/nostart/current")
                .contentType(MediaType.APPLICATION_JSON);

                mockMvc.perform(getRequest)
                .andExpect(status().isConflict());
        }

        @Test
        void leaveSession_validInput_returnsNoContent() throws Exception {
                SessionPutDTO sessionPutDTO = new SessionPutDTO();
                sessionPutDTO.setToken("userToken");

                Mockito.doNothing().when(sessionService).leaveSession(eq("test1234"), eq("userToken"));

                mockMvc.perform(delete("/session/test1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(sessionPutDTO)))
                        .andExpect(status().isNoContent());
        }
}
