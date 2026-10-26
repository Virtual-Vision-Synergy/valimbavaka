package org.vvs.valimbavaka.security.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.vvs.valimbavaka.security.handler.ProblemDetailWriter;
import org.vvs.valimbavaka.security.idempotency.IdempotencyStore.StoredRequest;

/** SYS-02, SYS-12 : le filtre réserve la clé, mémorise la réponse, libère la clé sur erreur et refuse un contenu différent. */
@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID KEY = UUID.randomUUID();
    private static final String PATH = "/api/v1/suppliers";
    private static final String BODY = "{\"name\":\"Huiles Ravo\",\"type\":\"LOCAL\"}";

    @Mock
    private IdempotencyStore store;

    @Mock
    private ProblemDetailWriter problemWriter;

    private IdempotencyFilter sut;
    private MockHttpServletResponse response;

    @BeforeEach
    void signIn() {
        sut = new IdempotencyFilter(store, problemWriter);
        response = new MockHttpServletResponse();
        var jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject(USER.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_firstRequest_storesTheResponseSoItCanBeReplayed() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(true);
        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).setStatus(201);
            res.getWriter().write("{\"id\":\"1\"}");
        };

        // Act
        sut.doFilter(request("POST", BODY), response, chain);

        // Assert
        verify(store).complete(USER, KEY, 201, "{\"id\":\"1\"}");
        verify(store, never()).release(any(), any());
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"1\"}");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503, 401, 403})
    void doFilter_responseNotReplayable_releasesTheKeySoTheClientCanRetry(int status) throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(true);
        FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(status);

        // Act
        sut.doFilter(request("POST", BODY), response, chain);

        // Assert
        verify(store).release(USER, KEY);
        verify(store, never()).complete(any(), any(), anyInt(), anyString());
    }

    @Test
    void doFilter_chainThrows_releasesTheKey() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(true);
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("panne");
        };

        // Act + Assert
        assertThatThrownBy(() -> sut.doFilter(request("POST", BODY), response, chain))
                .isInstanceOf(IllegalStateException.class);
        verify(store).release(USER, KEY);
    }

    @Test
    void doFilter_requestStillInProgress_returns409WithoutRunningTheChain() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(false);
        when(store.find(USER, KEY)).thenReturn(Optional.of(stored(null, null, fingerprint(BODY))));
        FilterChain chain = mock(FilterChain.class);

        // Act
        sut.doFilter(request("POST", BODY), response, chain);

        // Assert
        assertThat(rejection().getStatus()).isEqualTo(409);
        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_sameKeySameBody_writesTheStoredResponseAndMarksItReplayed() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(false);
        when(store.find(USER, KEY)).thenReturn(Optional.of(stored(201, "{\"id\":\"1\"}", fingerprint(BODY))));
        FilterChain chain = mock(FilterChain.class);

        // Act
        sut.doFilter(request("POST", BODY), response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":\"1\"}");
        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_sameKeyDifferentBody_returns422() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(false);
        when(store.find(USER, KEY)).thenReturn(Optional.of(stored(201, "{}", fingerprint(BODY))));
        FilterChain chain = mock(FilterChain.class);

        // Act
        sut.doFilter(request("POST", "{\"name\":\"Autre nom\",\"type\":\"LOCAL\"}"), response, chain);

        // Assert
        var problem = rejection();
        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getDetail()).contains("contenu différent");
        verifyNoInteractions(chain);
    }

    @Test
    void doFilter_sameKeyOtherMethod_returns422() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(false);
        when(store.find(USER, KEY)).thenReturn(Optional.of(stored(201, "{}", fingerprint(BODY))));
        FilterChain chain = mock(FilterChain.class);

        // Act
        sut.doFilter(request("PUT", BODY), response, chain);

        // Assert
        assertThat(rejection().getDetail()).contains("autre requête");
    }

    @Test
    void doFilter_storedRequestWithoutBodyHash_isReplayedWhateverTheBody() throws Exception {
        // Arrange : ligne écrite avant l'empreinte du corps (ou corps non lu)
        when(store.claim(any(), any(), any())).thenReturn(false);
        var withoutHash = new RequestFingerprint("POST", PATH, null);
        when(store.find(USER, KEY)).thenReturn(Optional.of(stored(201, "{}", withoutHash)));

        // Act
        sut.doFilter(request("POST", "{\"autre\":true}"), response, mock(FilterChain.class));

        // Assert
        assertThat(response.getHeader("Idempotency-Replayed")).isEqualTo("true");
    }

    @Test
    void doFilter_jsonBody_isHashedAndStillReadableByTheChain() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(true);
        var seen = new StringBuilder();
        FilterChain chain =
                (req, res) -> seen.append(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        // Act
        sut.doFilter(request("POST", BODY), response, chain);

        // Assert
        var captor = ArgumentCaptor.forClass(RequestFingerprint.class);
        verify(store).claim(any(), any(), captor.capture());
        assertThat(captor.getValue().bodyHash())
                .hasSize(64)
                .isEqualTo(fingerprint(BODY).bodyHash());
        assertThat(seen).hasToString(BODY);
    }

    @Test
    void doFilter_multipartBody_isNotReadSoNoHashIsKept() throws Exception {
        // Arrange
        when(store.claim(any(), any(), any())).thenReturn(true);
        var request = request("POST", "--limite--");
        request.setContentType("multipart/form-data; boundary=limite");

        // Act
        sut.doFilter(request, response, (req, res) -> {});

        // Assert
        var captor = ArgumentCaptor.forClass(RequestFingerprint.class);
        verify(store).claim(any(), any(), captor.capture());
        assertThat(captor.getValue().bodyHash()).isNull();
    }

    @Test
    void doFilter_getOrNoKey_isNotFiltered() throws Exception {
        // Arrange
        var chain = mock(FilterChain.class);
        var withoutKey = request("POST", BODY);
        withoutKey.removeHeader("Idempotency-Key");

        // Act
        sut.doFilter(withoutKey, response, chain);
        sut.doFilter(request("GET", ""), new MockHttpServletResponse(), chain);

        // Assert
        verifyNoInteractions(store);
    }

    @Test
    void doFilter_keyIsNotAnUuid_returns400() throws Exception {
        // Arrange
        var request = request("POST", BODY);
        request.removeHeader("Idempotency-Key");
        request.addHeader("Idempotency-Key", "pas-un-uuid");

        // Act
        sut.doFilter(request, response, mock(FilterChain.class));

        // Assert
        assertThat(rejection().getStatus()).isEqualTo(400);
        verifyNoInteractions(store);
    }

    private ProblemDetail rejection() throws Exception {
        var captor = ArgumentCaptor.forClass(ProblemDetail.class);
        verify(problemWriter).write(any(), any(), captor.capture());
        return captor.getValue();
    }

    private static MockHttpServletRequest request(String method, String body) {
        var request = new MockHttpServletRequest(method, PATH);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Idempotency-Key", KEY.toString());
        return request;
    }

    private static RequestFingerprint fingerprint(String body) throws Exception {
        var cached = CachedBodyRequest.of(request("POST", body));
        return new RequestFingerprint("POST", PATH, cached.bodyHash());
    }

    private static StoredRequest stored(Integer status, String body, RequestFingerprint request) {
        return new StoredRequest(request, status, body);
    }
}
