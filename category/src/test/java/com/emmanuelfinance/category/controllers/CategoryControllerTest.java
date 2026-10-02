package com.emmanuelfinance.category.controllers;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryTestDataBuilder;
import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryDomainException;
import com.emmanuelfinance.category.exceptions.CategoryErrorCode;
import com.emmanuelfinance.category.services.CategoryService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Cenários do POST /categories")
    class CreateCategoryTests {

        @Test
        @DisplayName("Deve retornar 400 Bad Request ao tentar criar com payload inválido")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            CreateCategoryDTO invalidDTO = new CreateCategoryDTO(null, null);

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).create(any());
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando a categoria já existir")
        void shouldReturn409ConflictWhenCategoryAlreadyExists() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();

            when(categoryService.create(any(CreateCategoryDTO.class)))
                    .thenThrow(new CategoryDomainException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS));

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isConflict());

            verify(categoryService, times(1)).create(any(CreateCategoryDTO.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK quando a criação for bem-sucedida")
        void shouldReturn200WhenCategoryCreationIsSuccessful() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category category = CategoryTestDataBuilder.categoryEntity(createDTO, userId, false);
            ResponseCategoryDTO responseDTO = CategoryTestDataBuilder.responseCategoryDTO(category);

            when(categoryService.create(any(CreateCategoryDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.name").value(responseDTO.name()));

            verify(categoryService, times(1)).create(any(CreateCategoryDTO.class));
        }
    }

    @Nested
    @DisplayName("Cenários do VIEW /categories/{id}")
    class ViewCategoryTests {

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a categoria não for encontrada")
        void shouldReturn404WhenTheCategoryIsNotFound() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(categoryService.view(notFoundId)).thenThrow(new CategoryNotFound());

            mockMvc.perform(get("/categories/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).view(notFoundId);
        }

        @Test
        @DisplayName("Deve retornar 200 OK quando a categoria for encontrada")
        void shouldReturn200WhenCategoryIsFound() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category category = CategoryTestDataBuilder.categoryEntity(createDTO, userId, false);
            ResponseCategoryDTO responseDTO = CategoryTestDataBuilder.responseCategoryDTO(category);

            when(categoryService.view(category.getId())).thenReturn(responseDTO);

            mockMvc.perform(get("/categories/{id}", category.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.name").value(responseDTO.name()));

            verify(categoryService, times(1)).view(category.getId());
        }
    }

    @Nested
    @DisplayName("Cenários do GET /categories (Listagem Paginada)")
    class ListCategoriesTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados paginados quando a busca for bem-sucedida")
        void shouldReturn200WithPaginatedDataWhenListIsSuccessful() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category category = CategoryTestDataBuilder.categoryEntity(createDTO, userId, false);
            ResponseCategoryDTO responseDTO = CategoryTestDataBuilder.responseCategoryDTO(category);

            PageResponseDTO<ResponseCategoryDTO> pageResponse = new PageResponseDTO<>(
                    List.of(responseDTO),
                    0,
                    10,
                    1L,
                    1
            );

            when(categoryService.list(any(CategoryFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/categories")
                            .param("name", "Alimentação")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value(responseDTO.name()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(categoryService, times(1)).list(any(CategoryFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com lista vazia no content quando não encontrar registros")
        void shouldReturn200WithEmptyContentWhenNoCategoriesFound() throws Exception {
            PageResponseDTO<ResponseCategoryDTO> emptyPageResponse = new PageResponseDTO<>(
                    Collections.emptyList(),
                    0,
                    10,
                    0L,
                    0
            );

            when(categoryService.list(any(CategoryFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPageResponse);

            mockMvc.perform(get("/categories")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(categoryService, times(1)).list(any(CategoryFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando os filtros de busca forem inválidos")
        void shouldReturn400WhenFiltersAreInvalid() throws Exception {
            mockMvc.perform(get("/categories")
                            .param("type", "ENUM_INVALIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(categoryService, atMostOnce()).list(any(CategoryFiltersDTO.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Cenários do GET /categories/deleted (Listagem Paginada de Categorias Excluídas)")
    class ListDeletedCategoriesTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados das categorias excluídas quando a busca for bem-sucedida")
        void shouldReturn200WithPaginatedDataWhenListDeletedIsSuccessful() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category category = CategoryTestDataBuilder.categoryEntity(createDTO, userId, false);
            ResponseCategoryDTO responseDTO = CategoryTestDataBuilder.responseCategoryDTO(category);

            PageResponseDTO<ResponseCategoryDTO> pageResponse = new PageResponseDTO<>(
                    List.of(responseDTO),
                    0,
                    10,
                    1L,
                    1
            );

            when(categoryService.listDeleted(any(CategoryFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/categories/deleted")
                            .param("name", "Categoria Deletada")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value(responseDTO.name()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(categoryService, times(1)).listDeleted(any(CategoryFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com lista vazia no content quando não houver categorias excluídas")
        void shouldReturn200WithEmptyContentWhenNoDeletedCategoriesFound() throws Exception {
            PageResponseDTO<ResponseCategoryDTO> emptyPageResponse = new PageResponseDTO<>(
                    Collections.emptyList(),
                    0,
                    10,
                    0L,
                    0
            );

            when(categoryService.listDeleted(any(CategoryFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPageResponse);

            mockMvc.perform(get("/categories/deleted")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(categoryService, times(1)).listDeleted(any(CategoryFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando houver erro de filtro na listagem de excluídas")
        void shouldReturn400WhenFiltersAreInvalidInListDeleted() throws Exception {
            mockMvc.perform(get("/categories/deleted")
                            .param("type", "ENUM_INVALIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(categoryService, atMostOnce()).listDeleted(any(CategoryFiltersDTO.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Cenários do PUT /categories/{id}")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando a atualização ocorrer com sucesso")
        void shouldReturn200WhenUpdateIsSuccessful() throws Exception {
            CreateCategoryDTO createDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category category = CategoryTestDataBuilder.categoryEntity(createDTO, userId, false);
            ResponseCategoryDTO responseDTO = CategoryTestDataBuilder.responseCategoryDTO(category);

            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    "Conta Atualizada",
                    TypeEnum.EXPENSE
            );

            when(categoryService.update(eq(category.getId()), any(UpdateCategoryDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(put("/categories/{id}", category.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.name").value(responseDTO.name()));

            verify(categoryService, times(1)).update(eq(category.getId()), any(UpdateCategoryDTO.class));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a categoria a ser atualizada não existir")
        void shouldReturn404WhenCategoryToUpdateDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();
            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    "Conta Atualizada",
                    TypeEnum.EXPENSE
            );

            when(categoryService.update(eq(notFoundId), any(UpdateCategoryDTO.class)))
                    .thenThrow(new CategoryNotFound());

            mockMvc.perform(put("/categories/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).update(eq(notFoundId), any(UpdateCategoryDTO.class));
        }
    }

    @Nested
    @DisplayName("Cenários do DELETE /categories/{id}")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a exclusão for realizada com sucesso")
        void shouldReturn204WhenDeleteIsSuccessful() throws Exception {
            UUID categoryId = UUID.randomUUID();

            doNothing().when(categoryService).delete(categoryId);

            mockMvc.perform(delete("/categories/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(categoryService, times(1)).delete(categoryId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando tentar excluir uma categoria inexistente")
        void shouldReturn404WhenCategoryToDeleteDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            doThrow(new CategoryNotFound()).when(categoryService).delete(notFoundId);

            mockMvc.perform(delete("/categories/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).delete(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do POST /categories/restore/{id}")
    class RestoreCategoryTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a restauração for realizada com sucesso")
        void shouldReturn204WhenRestoreIsSuccessful() throws Exception {
            UUID categoryId = UUID.randomUUID();

            doNothing().when(categoryService).restore(categoryId);

            mockMvc.perform(post("/categories/restore/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(categoryService, times(1)).restore(categoryId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando tentar restaurar uma categoria inexistente")
        void shouldReturn404WhenCategoryToRestoreDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            doThrow(new CategoryNotFound()).when(categoryService).restore(notFoundId);

            mockMvc.perform(post("/categories/restore/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).restore(notFoundId);
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando tentar restaurar uma categoria que não está excluída")
        void shouldReturn409ConflictWhenCategoryIsNotDeleted() throws Exception {
            UUID categoryId = UUID.randomUUID();

            doThrow(new CategoryDomainException(CategoryErrorCode.RESTORE_CATEGORY_NOT_DELETED))
                    .when(categoryService).restore(categoryId);

            mockMvc.perform(post("/categories/restore/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());

            verify(categoryService, times(1)).restore(categoryId);
        }
    }
}