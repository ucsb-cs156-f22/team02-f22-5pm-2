package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Recommendation;
import edu.ucsb.cs156.example.repositories.RecommendationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.aspectj.weaver.tools.cache.AsynchronousFileCacheBacking.RemoveCommand;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = RecommendationController.class)
@Import(TestConfig.class)
public class RecommendationControllerTests extends ControllerTestCase {
    
    @MockBean
    RecommendationRepository recommendationRepository;

    @MockBean
    UserRepository userRepository;

    // Authorization tests for /api/recommendation/admin/all

    @Test
    public void logged_out_users_cannot_get_all() throws Exception {
        mockMvc.perform(get("/api/recommendation/all"))
            .andExpect(status().is(403)); // logged
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_users_can_get_all() throws Exception {
        mockMvc.perform(get("/api/recommendation/all"))
            .andExpect(status().is(200));
    }

    @Test
    public void logged_out_users_cannot_get_by_id() throws Exception {
        mockMvc.perform(get("/api/recommendation?id=1"))
            .andExpect(status().is(403));
    }

    @Test
    public void logged_out_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/recommendation/post"))
            .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_regular_users_cannot_post() throws Exception {
        mockMvc.perform(post("/api/recommendation/post"))
            .andExpect(status().is(403));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
        Recommendation recommendation = Recommendation.builder()
            .id(1)
            .requesterEmail("requester@ucsb.edu")
            .professorEmail("professor@ucsb.edu")
            .explanation("explanation sample")
            .dateRequested(LocalDateTime.now())
            .dateNeeded(LocalDateTime.now())
            .done(false)
            .build();

        when(recommendationRepository.findById(eq(1L))).thenReturn(Optional.of(recommendation));

        MvcResult response = mockMvc.perform(get("/api/recommendation?id=1"))
            .andExpect(status().isOk()).andReturn();
        
        verify(recommendationRepository, times(1)).findById(eq(1L));
        String expectedJson = mapper.writeValueAsString(recommendation);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

            when (recommendationRepository.findById(eq(2L))).thenReturn(Optional.empty());

            MvcResult response = mockMvc.perform(get("/api/recommendation?id=2"))
                .andExpect(status().isNotFound()).andReturn();

            verify(recommendationRepository, times(1)).findById(eq(2L));
            Map<String, Object> json = responseToJson(response);
            assertEquals("EntityNotFoundException", json.get("type"));
            assertEquals("Recommendation with id 2 not found", json.get("message"));
    }

    @WithMockUser(roles = { "USER" })
    @Test
    public void logged_in_user_can_get_all_recommendations() throws Exception {

            // arrange

            Recommendation one = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("explanation sample")
                            .dateRequested(LocalDateTime.now())
                            .dateNeeded(LocalDateTime.now())
                            .done(false)
                            .build();

            Recommendation two = Recommendation.builder()
                            .id(2)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("explanation sample two")
                            .dateRequested(LocalDateTime.now())
                            .dateNeeded(LocalDateTime.now())
                            .done(false)
                            .build();

            ArrayList<Recommendation> expectedRecommendation = new ArrayList<>();
            expectedRecommendation.addAll(Arrays.asList(one, two));

            when(recommendationRepository.findAll()).thenReturn(expectedRecommendation);

            // act
            MvcResult response = mockMvc.perform(get("/api/recommendation/all"))
                            .andExpect(status().isOk()).andReturn();

            // assert

            verify(recommendationRepository, times(1)).findAll();
            String expectedJson = mapper.writeValueAsString(expectedRecommendation);
            String responseString = response.getResponse().getContentAsString();
            assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void an_admin_user_can_post_a_new_recommendation() throws Exception {
            // arrange

            LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

            Recommendation one = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("testexplanation")
                            .dateRequested(ldt1)
                            .dateNeeded(ldt1)
                            .done(false)
                            .build();

            when(recommendationRepository.save(eq(one))).thenReturn(one);

            // act
            MvcResult response = mockMvc.perform(
                            post("/api/recommendation/post?id=1&requesterEmail=requester@ucsb.edu&professorEmail=professor@ucsb.edu&explanation=testexplanation&dateRequested=2022-01-03T00:00:00&dateNeeded=2022-01-03T00:00:00&done=false")
                                            .with(csrf()))
                            .andExpect(status().isOk()).andReturn();

            // assert
            verify(recommendationRepository, times(1)).save(one);
            String expectedJson = mapper.writeValueAsString(one);
            String responseString = response.getResponse().getContentAsString();
            assertEquals(expectedJson, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_delete_a_date() throws Exception {
            // arrange

            Recommendation one = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("testexplanation")
                            .dateRequested(LocalDateTime.now())
                            .dateNeeded(LocalDateTime.now())
                            .done(false)
                            .build();

            when(recommendationRepository.findById(eq(1L))).thenReturn(Optional.of(one));

            // act
            MvcResult response = mockMvc.perform(
                            delete("/api/recommendation?id=1")
                                            .with(csrf()))
                            .andExpect(status().isOk()).andReturn();

            // assert
            verify(recommendationRepository, times(1)).findById(1L);
            verify(recommendationRepository, times(1)).delete(any());

            Map<String, Object> json = responseToJson(response);
            assertEquals("Recommendation with id 1 deleted", json.get("message"));
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_tries_to_delete_non_existant_recommendation_and_gets_right_error_message()
                    throws Exception {
            // arrange

            when(recommendationRepository.findById(eq(1L))).thenReturn(Optional.empty());

            // act
            MvcResult response = mockMvc.perform(
                            delete("/api/recommendation?id=1")
                                            .with(csrf()))
                            .andExpect(status().isNotFound()).andReturn();

            // assert
            verify(recommendationRepository, times(1)).findById(1L);
            Map<String, Object> json = responseToJson(response);
            assertEquals("Recommendation with id 1 not found", json.get("message"));
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_edit_an_existing_recommendation() throws Exception {
            // arrange

            LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

            Recommendation oRecommendation = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("original")
                            .dateRequested(ldt1)
                            .dateNeeded(ldt1)
                            .done(false)
                            .build();

            Recommendation eRecommendation = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester1@ucsb.edu")
                            .professorEmail("professor1@ucsb.edu")
                            .explanation("edited")
                            .dateRequested(ldt1)
                            .dateNeeded(ldt1)
                            .done(false)
                            .build();

            String requestBody = mapper.writeValueAsString(eRecommendation);

            when(recommendationRepository.findById(eq(1L))).thenReturn(Optional.of(oRecommendation));

            // act
            MvcResult response = mockMvc.perform(
                            put("/api/recommendation?id=1&requesterEmail=requester1@ucsb.edu&professorEmail=professor1@ucsb.edu&explanation=edited&dateRequested=2022-01-03T00:00:00&dateNeeded=2022-01-03T00:00:00&done=false")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .characterEncoding("utf-8")
                                            .content(requestBody)
                                            .with(csrf()))
                            .andExpect(status().isOk()).andReturn();

            // assert
            verify(recommendationRepository, times(1)).findById(1L);
            verify(recommendationRepository, times(1)).save(eRecommendation); // should be saved with updated info
            String responseString = response.getResponse().getContentAsString();
            assertEquals(requestBody, responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_cannot_edit_recommendation_that_does_not_exist() throws Exception {
            // arrange

            Recommendation edited = Recommendation.builder()
                            .id(1)
                            .requesterEmail("requester@ucsb.edu")
                            .professorEmail("professor@ucsb.edu")
                            .explanation("explanation sample two")
                            .dateRequested(LocalDateTime.now())
                            .dateNeeded(LocalDateTime.now())
                            .done(false)
                            .build();

            String requestBody = mapper.writeValueAsString(edited);

            when(recommendationRepository.findById(eq(1L))).thenReturn(Optional.empty());

            // act
            MvcResult response = mockMvc.perform(
                            put("/api/recommendation?id=1")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .characterEncoding("utf-8")
                                            .content(requestBody)
                                            .with(csrf()))
                            .andExpect(status().isNotFound()).andReturn();

            // assert
            verify(recommendationRepository, times(1)).findById(1L);
            Map<String, Object> json = responseToJson(response);
            assertEquals("Recommendation with id 1 not found", json.get("message"));

    }
}
