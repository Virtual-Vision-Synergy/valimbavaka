package org.vvs.valimbavaka.shared.excel.web;

import java.lang.reflect.Method;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import org.vvs.valimbavaka.shared.dto.PageSizeLimit;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/**
 * Enregistre, pour chaque méthode {@link ExcelExport}, les routes jumelles {@code <chemin>/export} (xlsx seul) et,
 * sauf {@code pdf = false}, {@code <chemin>/export/pdf} (PDF seul). Même méthode Java : paramètres, validation et
 * {@code @HasPermission} s'appliquent à l'identique, seule la taille de page passe de 20 (max 100) à
 * {@code maxRows} par défaut et au maximum, pour les deux formats.
 */
public class ExportAwareHandlerMapping extends RequestMappingHandlerMapping {

    private static final String XLSX_SEGMENT = "export";
    private static final String PDF_SEGMENT = "export/pdf";
    private static final String XLSX_VALUE = ExcelHttpMessageConverter.XLSX_VALUE;
    private static final String PDF_VALUE = MediaType.APPLICATION_PDF_VALUE;

    private final PageSizeLimit exportPageSize;

    public ExportAwareHandlerMapping(int maxRows) {
        this.exportPageSize = new PageSizeLimit(maxRows, maxRows);
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        super.registerHandlerMethod(handler, method, mapping);
        PathPatternsRequestCondition paths = mapping.getPathPatternsCondition();
        @Nullable ExcelExport export = AnnotatedElementUtils.findMergedAnnotation(method, ExcelExport.class);
        if (paths == null || export == null) {
            return;
        }
        super.registerHandlerMethod(handler, method, twin(mapping, paths, XLSX_SEGMENT, XLSX_VALUE));
        if (export.pdf()) {
            super.registerHandlerMethod(handler, method, twin(mapping, paths, PDF_SEGMENT, PDF_VALUE));
        }
    }

    private static RequestMappingInfo twin(
            RequestMappingInfo mapping, PathPatternsRequestCondition paths, String segment, String produces) {
        String[] twinPaths = paths.getPatternValues().stream()
                .map(path -> path.endsWith("/") ? path + segment : path + "/" + segment)
                .toArray(String[]::new);
        return mapping.mutate().paths(twinPaths).produces(produces).build();
    }

    // Appelé avant la liaison des paramètres : PageQuery lit alors les bornes de l'export
    @Override
    protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
        super.handleMatch(info, lookupPath, request);
        if (isExportRoute(info, request)) {
            request.setAttribute(PageSizeLimit.REQUEST_ATTRIBUTE, exportPageSize);
        }
    }

    private static boolean isExportRoute(RequestMappingInfo info, HttpServletRequest request) {
        Set<MediaType> producible = info.getProducesCondition().getProducibleMediaTypes();
        return (producible.contains(ExcelHttpMessageConverter.XLSX) || producible.contains(MediaType.APPLICATION_PDF))
                && request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(ExcelExport.class);
    }
}
