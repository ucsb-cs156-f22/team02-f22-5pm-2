package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBOrganization;
import edu.ucsb.cs156.example.repositories.UCSBOrganizationRepository;

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

@WebMvcTest(controllers = UCSBOrganizationController.class)
@Import(TestConfig.class)
public class UCSBOrganizationControllerTests extends ControllerTestCase {

        @MockBean
        UCSBOrganizationRepository ucsbOrganizationRepository;

        @MockBean
        UserRepository userRepository;

        // Authorization tests for /api/UCSBOrganization/admin/all

        @Test
        public void logged_out_users_cannot_get_all() throws Exception {
                mockMvc.perform(get("/api/UCSBOrganization/all"))
                                .andExpect(status().is(403)); // logged out users can't get all
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_can_get_all() throws Exception {
                mockMvc.perform(get("/api/UCSBOrganization/all"))
                                .andExpect(status().is(200)); // logged
        }

    @Test
    public void logged_out_users_cannot_get_by_id() throws Exception {
	mockMvc.perform(get("/api/UCSBOrganization?id=SKY"))
	    .andExpect(status().is(403)); // logged out users can't get by id
        }


        // Authorization tests for /api/UCSBOrganization/post
        // (Perhaps should also have these for put and delete)

        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/UCSBOrganization/post"))
                                .andExpect(status().is(403));
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/UCSBOrganization/post"))
                                .andExpect(status().is(403)); // only admins can post
        }

        // Tests with mocks for database actions

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

                // arrange

	    UCSBOrganization tabletop = UCSBOrganization.builder()
		.orgCode("TTGC")
		.orgTranslationShort("Tabletop Club")
		.orgTranslation("Tabletop Gaming Club")
		.inactive(false)
		.build();

                when(ucsbOrganizationRepository.findById(eq("TTGC"))).thenReturn(Optional.of(tabletop));

                // act
                MvcResult response = mockMvc.perform(get("/api/UCSBOrganization?id=TTGC"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(ucsbOrganizationRepository, times(1)).findById(eq("TTGC"));
                String expectedJson = mapper.writeValueAsString(tabletop);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

                // arrange

                when(ucsbOrganizationRepository.findById(eq("TTGC"))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(get("/api/UCSBOrganization?id=TTGC"))
                                .andExpect(status().isNotFound()).andReturn();

                // assert

                verify(ucsbOrganizationRepository, times(1)).findById(eq("TTGC"));
                Map<String, Object> json = responseToJson(response);
                assertEquals("EntityNotFoundException", json.get("type"));
                assertEquals("UCSBOrganization with id TTGC not found", json.get("message"));
        }

    
        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_user_can_get_all_organizations() throws Exception {

                // arrange

                UCSBOrganization sky = UCSBOrganization.builder()
		    .orgCode("SKY")
		    .orgTranslationShort("Skydiving Club")
		    .orgTranslation("Skydiving Club at UCSB")
		    .inactive(false)
		    .build();

		UCSBOrganization tabletop = UCSBOrganization.builder()
		    .orgCode("TTGC")
		    .orgTranslationShort("Tabletop Club")
		    .orgTranslation("Tabletop Gaming Club")
		    .inactive(false)
		    .build();

                ArrayList<UCSBOrganization> expectedOrg = new ArrayList<>();
                expectedOrg.addAll(Arrays.asList(sky, tabletop));

                when(ucsbOrganizationRepository.findAll()).thenReturn(expectedOrg);

                // act
                MvcResult response = mockMvc.perform(get("/api/UCSBOrganization/all"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(ucsbOrganizationRepository, times(1)).findAll();
                String expectedJson = mapper.writeValueAsString(expectedOrg);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void an_admin_user_can_post_a_new_organization() throws Exception {
                // arrange

                UCSBOrganization sky = UCSBOrganization.builder()
		    .orgCode("SKY")
		    .orgTranslationShort("Skydiving Club")
		    .orgTranslation("Skydiving Club at UCSB")
		    .inactive(true)
		    .build();

                when(ucsbOrganizationRepository.save(eq(sky))).thenReturn(sky);

                // act
                MvcResult response = mockMvc.perform(
						     post("/api/UCSBOrganization/post?orgCode=SKY&orgTranslationShort=Skydiving Club&orgTranslation=Skydiving Club at UCSB&inactive=true")
						     .with(csrf()))
		    .andExpect(status().isOk()).andReturn();

                // assert
                verify(ucsbOrganizationRepository, times(1)).save(sky);
                String expectedJson = mapper.writeValueAsString(sky);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }
}
