package org.vvs.valimbavaka.shared.excel.web;

import java.io.IOException;
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
import org.vvs.valimbavaka.shared.excel.ExcelExporter;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;
import org.vvs.valimbavaka.shared.excel.event.ExcelExportedEvent;

/**
 * Sérialise en xlsx le corps d'une route {@code .../export}. Jamais choisi pour {@code *}{@code /*} ni pour du
 * JSON : seul un type xlsx explicite (le {@code produces} de la route jumelle) l'active.
 * Un {@code Stream} est écrit sans être collecté, puis fermé.
 */
public class ExcelHttpMessageConverter extends AbstractSmartHttpMessageConverter<Object> {

    public static final String XLSX_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final MediaType XLSX = MediaType.parseMediaType(XLSX_VALUE);

    private static final String EXTENSION = ".xlsx";

    private final ExcelExporter exporter;
    private final ApplicationEventPublisher eventPublisher;

    public ExcelHttpMessageConverter(ExcelExporter exporter, ApplicationEventPublisher eventPublisher) {
        super(XLSX);
        this.exporter = exporter;
        this.eventPublisher = eventPublisher;
    }

    /** Données à exporter et type de leurs éléments (qui porte les {@code @ExcelColumn}). */
    private record Rows(Stream<?> data, Class<?> elementType) {
    }

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
        throw new UnsupportedOperationException("Lecture xlsx non prise en charge");
    }

    @Override
    public boolean canWrite(ResolvableType type, Class<?> valueClass, @Nullable MediaType mediaType) {
        return mediaType != null && mediaType.isConcrete() && canWrite(mediaType);
    }

    @Override
    protected void writeInternal(Object body, ResolvableType type, HttpOutputMessage outputMessage,
            @Nullable Map<String, Object> hints) throws IOException {
        Rows rows = toRows(body, type);
        @Nullable ExcelExport annotation = currentAnnotation();
        String simpleName = rows.elementType().getSimpleName();
        String filename = filename(annotation == null ? "" : annotation.filename(), simpleName);
        String sheetName = annotation == null || annotation.sheet().isBlank() ? simpleName : annotation.sheet();
        outputMessage.getHeaders().setContentDisposition(
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        try (Stream<?> data = rows.data()) {
            exporter.export(new DeferredOutputStream(outputMessage), sheet(rows.elementType(), sheetName, data));
        }
        if (annotation != null) {
            eventPublisher.publishEvent(new ExcelExportedEvent(annotation.target(), filename, rows.elementType()));
        }
    }

    private static Rows toRows(Object body, ResolvableType type) {
        return switch (body) {
            case Stream<?> stream -> new Rows(stream, declaredElementType(type, Stream.class)
                    .orElseThrow(() -> new HttpMessageNotWritableException("Déclarez Stream<MonDto> en retour")));
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
        return declared == null || declared == Object.class
                ? Optional.empty() : Optional.of(declared);
    }

    // Typage sans cast non vérifié : chaque élément est converti vers le type déclaré
    private static <T> ExcelExporter.Sheet<T> sheet(Class<T> type, String name, Stream<?> data) {
        return ExcelExporter.sheet(type, name, data.map(type::cast));
    }

    private static String filename(String requested, String fallback) {
        String base = requested.isBlank() ? fallback : requested;
        return base.endsWith(EXTENSION) ? base : base + EXTENSION;
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
