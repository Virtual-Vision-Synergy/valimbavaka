package org.vvs.valimbavaka.shared.excel.web;

import java.io.IOException;
import java.io.OutputStream;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpOutputMessage;

/**
 * N'ouvre le corps de la réponse (et n'y fige les en-têtes xlsx) qu'au premier octet écrit. Le classeur n'est
 * écrit qu'une fois complet : une erreur pendant sa construction laisse la réponse intacte pour un ProblemDetail.
 */
final class DeferredOutputStream extends OutputStream {

    private final HttpOutputMessage message;
    private @Nullable OutputStream body;

    DeferredOutputStream(HttpOutputMessage message) {
        this.message = message;
    }

    @Override
    public void write(int b) throws IOException {
        body().write(b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        body().write(bytes, offset, length);
    }

    @Override
    public void flush() throws IOException {
        if (body != null) {
            body.flush();
        }
    }

    private OutputStream body() throws IOException {
        if (body == null) {
            body = message.getBody();
        }
        return body;
    }
}
