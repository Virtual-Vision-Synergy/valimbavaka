package org.vvs.valimbavaka.core.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Contrat d'erreur RFC 9457 : statut et code machine pour chaque famille d'exception (convention §7.4). */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleBusiness_notFound_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Commande introuvable"));
    }

    @Test
    void handleBusiness_conflict_returns409() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void handleBusiness_ruleViolated_returns422() throws Exception {
        mockMvc.perform(get("/business"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void handleMethodArgumentNotValid_invalidBody_returns422WithFieldErrors() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void handleExceptionInternal_unreadableJson_returns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void handleUnexpected_bug_returns500WithoutInternalDetail() throws Exception {
        mockMvc.perform(get("/bug"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Erreur interne"));
    }

    @Test
    void handleOptimisticLocking_concurrentUpdate_returns409() throws Exception {
        mockMvc.perform(get("/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void handleDataIntegrity_uniqueViolation_returns409() throws Exception {
        mockMvc.perform(get("/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void handleDataIntegrity_otherViolation_returns500WithoutInternalDetail() throws Exception {
        mockMvc.perform(get("/too-long"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Erreur interne"));
    }

    record NamedWDto(@NotBlank String name) {}

    @RestController
    static class FailingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Commande introuvable");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Doublon");
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessException("Montant négatif");
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody NamedWDto body) {
            // le corps est validé avant l'appel
        }

        @GetMapping("/stale")
        void stale() {
            throw new ObjectOptimisticLockingFailureException("Supplier", "id");
        }

        @GetMapping("/duplicate")
        void duplicate() {
            throw new DataIntegrityViolationException(
                    "doublon", new SQLException("duplicate key value violates unique constraint", "23505"));
        }

        @GetMapping("/too-long")
        void tooLong() {
            throw new DataIntegrityViolationException(
                    "trop long", new SQLException("value too long for type character varying(500)", "22001"));
        }

        @GetMapping("/bug")
        void bug() {
            throw new IllegalStateException("détail SQL à ne jamais exposer");
        }
    }
}
