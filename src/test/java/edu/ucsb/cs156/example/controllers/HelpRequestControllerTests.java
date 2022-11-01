package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.HelpRequest;
import edu.ucsb.cs156.example.repositories.HelpRequestRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@WebMvcTest(controllers = HelpRequestController.class)
@Import(TestConfig.class)
public class HelpRequestControllerTests extends ControllerTestCase {

        @MockBean
        HelpRequestRepository helpRequestRepository;

        @MockBean
        UserRepository userRepository;

        // Authorization tests for /api/helprequest/admin/all

        @Test
        public void logged_out_users_cannot_get_all() throws Exception {
                mockMvc.perform(get("/api/helprequest/all"))
                                .andExpect(status().is(403)); // logged out users can't get all
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_can_get_all() throws Exception {
                mockMvc.perform(get("/api/helprequest/all"))
                                .andExpect(status().is(200)); // logged
        }

        @Test
        public void logged_out_users_cannot_get_by_id() throws Exception {
                mockMvc.perform(get("/api/helprequest?id=7"))
                                .andExpect(status().is(403)); // logged out users can't get by id
        }

        // Authorization tests for /api/helprequest/post
        // (Perhaps should also have these for put and delete)

        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/helprequest/post"))
                                .andExpect(status().is(403));
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/helprequest/post"))
                                .andExpect(status().is(403)); // only admins can post
        }

        // // Tests with mocks for database actions

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
                HelpRequest helpRequest = HelpRequest.builder()
                        .id(7)
                        .requesterEmail("michael@gmail.com")
                        .teamId("michaelsteam")
                        .tableOrBreakoutRoom("table")
                        .explanation("what's going on")
                        .solved(false)
                        .build();

                when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.of(helpRequest));

                // act
                MvcResult response = mockMvc.perform(get("/api/helprequest?id=7"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(helpRequestRepository, times(1)).findById(eq(7L));
                String expectedJson = mapper.writeValueAsString(helpRequest);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

                // arrange

                when(helpRequestRepository.findById(eq(7L))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(get("/api/helprequest?id=7"))
                                .andExpect(status().isNotFound()).andReturn();

                // assert

                verify(helpRequestRepository, times(1)).findById(eq(7L));
                Map<String, Object> json = responseToJson(response);
                assertEquals("EntityNotFoundException", json.get("type"));
                assertEquals("HelpRequest with id 7 not found", json.get("message"));
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_user_can_get_all_helprequests() throws Exception {

                // arrange
                HelpRequest helpRequest1 = HelpRequest.builder()
                        .id(7)
                        .requesterEmail("michael@gmail.com")
                        .teamId("michaelsteam")
                        .tableOrBreakoutRoom("table")
                        .explanation("what's going on")
                        .solved(false)
                        .build();

                HelpRequest helpRequest2 = HelpRequest.builder()
                        .id(8)
                        .requesterEmail("mike@gmail.com")
                        .teamId("mikesteam")
                        .tableOrBreakoutRoom("breakout")
                        .explanation("going on what's")
                        .solved(false)
                        .build();

                ArrayList<HelpRequest> expectedRequests = new ArrayList<>();
                expectedRequests.addAll(Arrays.asList(helpRequest1, helpRequest2));

                when(helpRequestRepository.findAll()).thenReturn(expectedRequests);

                // act
                MvcResult response = mockMvc.perform(get("/api/helprequest/all"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(helpRequestRepository, times(1)).findAll();
                String expectedJson = mapper.writeValueAsString(expectedRequests);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void an_admin_user_can_post_a_new_ucsbdate() throws Exception {
                
                // arrange
                HelpRequest helpRequest1 = HelpRequest.builder()
                        .requesterEmail("michaelgmailcom")
                        .teamId("michaelsteam")
                        .tableOrBreakoutRoom("table")
                        .explanation("whatgoingon")
                        .solved(true)
                        .build();

                when(helpRequestRepository.save(eq(helpRequest1))).thenReturn(helpRequest1);

                // act
                MvcResult response = mockMvc.perform(
                        post("/api/helprequest/post?requesterEmail=michaelgmailcom&teamId=michaelsteam&tableOrBreakoutRoom=table&explanation=whatgoingon&solved=true")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(helpRequestRepository, times(1)).save(helpRequest1);
                String expectedJson = mapper.writeValueAsString(helpRequest1);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_can_delete_a_date() throws Exception {
                // arrange
                HelpRequest helpRequest1 = HelpRequest.builder()
                        .id(15)
                        .requesterEmail("michael@gmail.com")
                        .teamId("michaelsteam")
                        .tableOrBreakoutRoom("table")
                        .explanation("what's going on")
                        .solved(false)
                        .build();

                when(helpRequestRepository.findById(eq(15L))).thenReturn(Optional.of(helpRequest1));

                // act
                MvcResult response = mockMvc.perform(
                                delete("/api/helprequest?id=15")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(helpRequestRepository, times(1)).findById(15L);
                verify(helpRequestRepository, times(1)).delete(any());

                Map<String, Object> json = responseToJson(response);
                assertEquals("HelpRequest with id 15 deleted", json.get("message"));
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_tries_to_delete_non_existant_ucsbdate_and_gets_right_error_message()
                        throws Exception {
                // arrange

                when(helpRequestRepository.findById(eq(15L))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(
                                delete("/api/helprequest?id=15")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                // assert
                verify(helpRequestRepository, times(1)).findById(15L);
                Map<String, Object> json = responseToJson(response);
                assertEquals("HelpRequest with id 15 not found", json.get("message"));
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_can_edit_an_existing_ucsbdate() throws Exception {
                // arrange
                HelpRequest helpRequestOrig = HelpRequest.builder()
                        .requesterEmail("michaelgmailcom")
                        .teamId("michaelsteam")
                        .tableOrBreakoutRoom("table")
                        .explanation("whatgoingon")
                        .solved(false)
                        .build();

                HelpRequest helpRequestEdited = HelpRequest.builder()
                        .requesterEmail("newfakeemail")
                        .teamId("newfaketeam")
                        .tableOrBreakoutRoom("notatable")
                        .explanation("newtext")
                        .solved(true)
                        .build();

                String requestBody = mapper.writeValueAsString(helpRequestEdited);

                when(helpRequestRepository.findById(eq(67L))).thenReturn(Optional.of(helpRequestOrig));

                // act
                MvcResult response = mockMvc.perform(
                                put("/api/helprequest?id=67")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .characterEncoding("utf-8")
                                                .content(requestBody)
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(helpRequestRepository, times(1)).findById(67L);
                verify(helpRequestRepository, times(1)).save(helpRequestEdited); // should be saved with correct user
                String responseString = response.getResponse().getContentAsString();
                assertEquals(requestBody, responseString);
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_cannot_edit_ucsbdate_that_does_not_exist() throws Exception {
                // arrange

                HelpRequest helpRequestEdited = HelpRequest.builder()
                        .requesterEmail("newfakeemail")
                        .teamId("newfaketeam")
                        .tableOrBreakoutRoom("notatable")
                        .explanation("newtext")
                        .solved(true)
                        .build();

                String requestBody = mapper.writeValueAsString(helpRequestEdited);

                when(helpRequestRepository.findById(eq(67L))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(
                                put("/api/helprequest?id=67")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .characterEncoding("utf-8")
                                                .content(requestBody)
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                // assert
                verify(helpRequestRepository, times(1)).findById(67L);
                Map<String, Object> json = responseToJson(response);
                assertEquals("HelpRequest with id 67 not found", json.get("message"));

        }
}
