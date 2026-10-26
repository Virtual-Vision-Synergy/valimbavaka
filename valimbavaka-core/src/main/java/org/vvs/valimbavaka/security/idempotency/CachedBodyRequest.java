package org.vvs.valimbavaka.security.idempotency;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.jspecify.annotations.Nullable;

/**
 * Requête dont le corps a été lu une fois pour en calculer l'empreinte (SHA-256), puis remis intact à la
 * suite de la chaîne. Les envois de fichier (multipart) et les corps de plus de 1 Mio ne sont pas lus.
 */
final class CachedBodyRequest extends HttpServletRequestWrapper {

    static final int MAX_BODY_BYTES = 1_048_576;

    private final byte @Nullable [] body;
    private final @Nullable String bodyHash;

    private CachedBodyRequest(HttpServletRequest request, byte @Nullable [] body) {
        super(request);
        this.body = body;
        this.bodyHash = body == null ? null : sha256(body);
    }

    static CachedBodyRequest of(HttpServletRequest request) throws IOException {
        return new CachedBodyRequest(
                request, isCacheable(request) ? request.getInputStream().readAllBytes() : null);
    }

    @Nullable
    String bodyHash() {
        return bodyHash;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (body == null) {
            return super.getInputStream();
        }
        return new CachedInputStream(body);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (body == null) {
            return super.getReader();
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
    }

    private Charset charset() {
        var encoding = getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    private static boolean isCacheable(HttpServletRequest request) {
        var length = request.getContentLengthLong();
        var type = request.getContentType();
        return length >= 0
                && length <= MAX_BODY_BYTES
                && (type == null || !type.toLowerCase().startsWith("multipart/"));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Flux relisable sur le corps déjà lu. */
    private static final class CachedInputStream extends ServletInputStream {

        private final ByteArrayInputStream delegate;

        CachedInputStream(byte[] content) {
            this.delegate = new ByteArrayInputStream(content);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            throw new UnsupportedOperationException("Lecture asynchrone non prise en charge");
        }
    }
}
