package edu.ucsb.cs156.example.controllers;

import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;
import edu.ucsb.cs156.example.repositories.UCSBDiningCommonsMenuItemRepository;

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

@WebMvcTest(controllers = UCSBDiningCommonsMenuItemController.class)
@Import(TestConfig.class)
public class UCSBDiningCommonsMenuItemControllerTests extends ControllerTestCase{

        @MockBean
        UCSBDiningCommonsMenuItemRepository ucsbDiningCommonsMenuItemRepository;

        @MockBean
        UserRepository userRepository;

        // Authorization tests for /api/ucsbdiningcommonsmenuitem/admin/all

        

        @Test
        public void logged_out_users_cannot_get_all() throws Exception {
                mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem/all"))
                                .andExpect(status().is(403)); // logged out users can't get all
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_users_can_get_all() throws Exception {
                mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem/all"))
                                .andExpect(status().is(200)); // logged
        }
        

        @Test
        public void logged_out_users_cannot_get_by_id() throws Exception {
                mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem?id=1"))
                                .andExpect(status().is(403)); // logged out users can't get by id
        }
        
        // Authorization tests for /api/ucsbdiningcommonsmenuitem/post
        // (Perhaps should also have these for put and delete)

        @Test
        public void logged_out_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/post"))
                                .andExpect(status().is(403));
        }

        
        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_post() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/post"))
                                .andExpect(status().is(403)); // only admins can post
        }

        
        @Test
        public void logged_out_users_cannot_put() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/put"))
                                .andExpect(status().is(403)); // only admins can put
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_put() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/put"))
                                .andExpect(status().is(403)); // only admins can put
        }

        
        @Test
        public void logged_out_users_cannot_delete() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/delete"))
                                .andExpect(status().is(403)); // only admins can delete
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_regular_users_cannot_delete() throws Exception {
                mockMvc.perform(post("/api/ucsbdiningcommonsmenuitem/delete"))
                                .andExpect(status().is(403)); // only admins can delete
        }
        
        // Tests with mocks for database actions

        // get all

        @WithMockUser(roles = { "USER" })
        @Test
        public void logged_in_user_can_get_all_ucsbdiningcommonsmenuitems() throws Exception {

                // arrange

                UCSBDiningCommonsMenuItem ortega = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("ortega")
                                .name("Baked Pesto Pasta with Chicken")
                                .station("Entree Specials")
                                .build();

                UCSBDiningCommonsMenuItem ortega2 = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("ortega")
                                .name("Tofu Banh Mi Sandwich (v)")
                                .station("Entree Specials")
                                .build();

                ArrayList<UCSBDiningCommonsMenuItem> expectedMenuItems = new ArrayList<>();
                expectedMenuItems.addAll(Arrays.asList(ortega, ortega2));

                when(ucsbDiningCommonsMenuItemRepository.findAll()).thenReturn(expectedMenuItems);

                // act
                MvcResult response = mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem/all"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findAll();
                String expectedJson = mapper.writeValueAsString(expectedMenuItems);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        // post new

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void an_admin_user_can_post_a_new_menuitem() throws Exception {
                // arrange

                UCSBDiningCommonsMenuItem portola = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("portola")
                                .name("Cream of Broccoli Soup (v)")
                                .station("Greens")
                                .build();

                when(ucsbDiningCommonsMenuItemRepository.save(eq(portola))).thenReturn(portola);

                // act
                MvcResult response = mockMvc.perform(
                                post("/api/ucsbdiningcommonsmenuitem/post?diningCommonsCode=portola&name=Cream of Broccoli Soup (v)&station=Greens")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(portola);
                String expectedJson = mapper.writeValueAsString(portola);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        // get by id

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

                // arrange

                when(ucsbDiningCommonsMenuItemRepository.findById(eq(1L))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem?id=1"))
                                .andExpect(status().isNotFound()).andReturn();

                // assert

                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(1L));
                Map<String, Object> json = responseToJson(response);
                assertEquals("EntityNotFoundException", json.get("type"));
                assertEquals("UCSBDiningCommonsMenuItem with id 1 not found", json.get("message"));
        }

        @WithMockUser(roles = { "USER" })
        @Test
        public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

                // arrange

                UCSBDiningCommonsMenuItem menuitem = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("portola")
                                .name("Cream of Broccoli Soup (v)")
                                .station("Greens")
                                .build();

                when(ucsbDiningCommonsMenuItemRepository.findById(eq(99L))).thenReturn(Optional.of(menuitem));

                // act
                MvcResult response = mockMvc.perform(get("/api/ucsbdiningcommonsmenuitem?id=99"))
                                .andExpect(status().isOk()).andReturn();

                // assert

                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(99L));
                String expectedJson = mapper.writeValueAsString(menuitem);
                String responseString = response.getResponse().getContentAsString();
                assertEquals(expectedJson, responseString);
        }

        // delete

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_can_delete_a_menuitem() throws Exception {
                // arrange

                UCSBDiningCommonsMenuItem portola = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("portola")
                                .name("Cream of Broccoli Soup (v)")
                                .station("Greens")
                                .build();

                when(ucsbDiningCommonsMenuItemRepository.findById(35L)).thenReturn(Optional.of(portola));

                // act
                MvcResult response = mockMvc.perform(
                                delete("/api/ucsbdiningcommonsmenuitem?id=35")
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(35L);
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).delete(any());

                Map<String, Object> json = responseToJson(response);
                assertEquals("UCSBDiningCommonsMenuItem with id 35 deleted", json.get("message"));
        }

        
        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_tries_to_delete_non_existan_menuitem_and_gets_right_error_message()
                        throws Exception {
                // arrange

                when(ucsbDiningCommonsMenuItemRepository.findById(eq(49L))).thenReturn(Optional.empty());

                // act
                MvcResult response = mockMvc.perform(
                                delete("/api/ucsbdiningcommonsmenuitem?id=49")
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                // assert
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(49L);
                Map<String, Object> json = responseToJson(response);
                assertEquals("UCSBDiningCommonsMenuItem with id 49 not found", json.get("message"));
        }

        // put 
        
        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_can_edit_an_existing_menuitem() throws Exception {
                // arrange

                UCSBDiningCommonsMenuItem portolaOrig = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("portola")
                                .name("Baked Pesto Pasta with Chicken")
                                .station("Entree Specials")
                                .build();

                UCSBDiningCommonsMenuItem portolaEdited = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("ortega")
                                .name("Salad")
                                .station("Salad Bar")
                                .build();

                String requestBody = mapper.writeValueAsString(portolaEdited);

                when(ucsbDiningCommonsMenuItemRepository.findById(eq(2L))).thenReturn(Optional.of(portolaOrig));

                // act
                MvcResult response = mockMvc.perform(
                                put("/api/ucsbdiningcommonsmenuitem?id=2")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .characterEncoding("utf-8")
                                                .content(requestBody)
                                                .with(csrf()))
                                .andExpect(status().isOk()).andReturn();

                // assert
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(2L);
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(portolaEdited); // should be saved with updated info
                String responseString = response.getResponse().getContentAsString();
                assertEquals(requestBody, responseString);
        }

        @WithMockUser(roles = { "ADMIN", "USER" })
        @Test
        public void admin_cannot_edit_menuitem_that_does_not_exist() throws Exception {
                // arrange

                UCSBDiningCommonsMenuItem portolaOrig = UCSBDiningCommonsMenuItem.builder()
                                .diningCommonsCode("portola")
                                .name("Baked Pesto Pasta with Chicken and Beef")
                                .station("Entree Specials")
                                .build();

                String requestBody = mapper.writeValueAsString(portolaOrig);

                when(ucsbDiningCommonsMenuItemRepository.findById(eq(2L))).thenReturn(Optional.of(portolaOrig));

                // act
                MvcResult response = mockMvc.perform(
                                put("/api/ucsbdiningcommonsmenuitem?id=50")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .characterEncoding("utf-8")
                                                .content(requestBody)
                                                .with(csrf()))
                                .andExpect(status().isNotFound()).andReturn();

                // assert
                verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(50L);
                Map<String, Object> json = responseToJson(response);
                assertEquals("UCSBDiningCommonsMenuItem with id 50 not found", json.get("message"));
        }

        

}
