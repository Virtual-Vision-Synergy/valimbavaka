package org.vvs.valimbavaka.shared.excel.web;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractSmartHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;
import org.vvs.valimbavaka.shared.excel.event.ExcelExportedEvent;

/**
 * Base des convertisseurs d'export (xlsx, PDF) d'une route jumelle {@code .../export}. Jamais choisi pour
 * {@code *}{@code /*} ni pour du JSON : seul le type explicite du {@code produces} de la route jumelle l'active.
 * Un {@code Stream} est écrit sans être collecté, puis fermé ; l'événement n'est publié qu'après l'écriture.
 */
abstract class ExportHttpMessageConverter extends AbstractSmartHttpMessageConverter<Object> {

    private static final String KNOWN_EXTENSION = "\\.(xlsx|pdf)$";

    private final String extension;
    private final ApplicationEventPublisher eventPublisher;

    ExportHttpMessageConverter(MediaType mediaType, String extension, ApplicationEventPublisher eventPublisher) {
        super(mediaType);
        this.extension = extension;
        this.eventPublisher = eventPublisher;
    }

    /** Données à exporter et type de leurs éléments (qui porte les {@code @ExcelColumn}). */
    private record Rows(Stream<?> data, Class<?> elementType) {}

    /** Écrit le fichier ; {@code annotation} n'est nulle que si le convertisseur est appelé hors requête. */
    protected abstract void writeFile(
            OutputStream out, Class<?> elementType, Stream<?> data, @Nullable ExcelExport annotation)
            throws IOException;

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints) {
        throw new UnsupportedOperationException("Lecture d'un fichier d'export non prise en charge");
    }

    // Seulement pour une méthode @ExcelExport : un PDF ou un xlsx renvoyé tel quel (byte[], Resource) reste intact
    @Override
    public boolean canWrite(ResolvableType type, Class<?> valueClass, @Nullable MediaType mediaType) {
        return mediaType != null && mediaType.isConcrete() && canWrite(mediaType) && currentAnnotation() != null;
    }

    @Override
    protected final void writeInternal(
            Object body, ResolvableType type, HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints)
            throws IOException {
        Rows rows = toRows(body, type);
        @Nullable ExcelExport annotation = currentAnnotation();
        String filename = filename(annotation == null ? "" : annotation.filename(), rows.elementType());
        outputMessage
                .getHeaders()
                .setContentDisposition(ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build());
        try (Stream<?> data = rows.data()) {
            writeFile(new DeferredOutputStream(outputMessage), rows.elementType(), data, annotation);
        }
        if (annotation != null) {
            eventPublisher.publishEvent(new ExcelExportedEvent(annotation.target(), filename, rows.elementType()));
        }
    }

    private static Rows toRows(Object body, ResolvableType type) {
        return switch (body) {
            case Stream<?> stream ->
                new Rows(
                        stream,
                        declaredElementType(type, Stream.class)
                                .orElseThrow(() ->
                                        new HttpMessageNotWritableException("Déclarez Stream<MonDto> en retour")));
            case Page<?> page -> fromItems(page.getContent(), type, Page.class);
            case PageResponse<?> page -> fromItems(page.items(), type, PageResponse.class);
            case Collection<?> items -> fromItems(items, type, Collection.class);
            default -> new Rows(Stream.of(body), body.getClass());
        };
    }

    private static Rows fromItems(Collection<?> items, ResolvableType type, Class<?> container) {
        Class<?> elementType = declaredElementType(type, container)
                .or(() -> items.stream().filter(Objects::nonNull).findFirst().map(Object::getClass))
                .orElseThrow(() -> new HttpMessageNotWritableException("Type d'élément inconnu : déclarez-le"));
        return new Rows(items.stream(), elementType);
    }

    private static Optional<Class<?>> declaredElementType(ResolvableType type, Class<?> container) {
        Class<?> declared = type.as(container).getGeneric(0).resolve();
        return declared == null || declared == Object.class ? Optional.empty() : Optional.of(declared);
    }

    // « articles » et « articles.xlsx » donnent « articles.pdf » sur la route PDF
    private String filename(String requested, Class<?> elementType) {
        String base = requested.isBlank() ? elementType.getSimpleName() : requested.replaceFirst(KNOWN_EXTENSION, "");
        return base + extension;
    }

    private static @Nullable ExcelExport currentAnnotation() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)
                        instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getMethodAnnotation(ExcelExport.class);
        }
        return null;
    }
}
