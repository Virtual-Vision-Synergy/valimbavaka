package org.vvs.valimbavaka.shared.excel.web;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import org.vvs.valimbavaka.shared.dto.PageSizeLimit;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/**
 * Enregistre, pour chaque méthode {@link ExcelExport}, une route jumelle {@code <chemin>/export} qui ne produit
 * que du xlsx. Même méthode Java : paramètres, validation et {@code @HasPermission} s'appliquent à l'identique,
 * seule la taille de page passe de 20 (max 100) à {@code maxRows} par défaut et au maximum.
 */
public class ExportAwareHandlerMapping extends RequestMappingHandlerMapping {

    private static final String EXPORT_SEGMENT = "export";

    private final PageSizeLimit exportPageSize;

    public ExportAwareHandlerMapping(int maxRows) {
        this.exportPageSize = new PageSizeLimit(maxRows, maxRows);
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        super.registerHandlerMethod(handler, method, mapping);
        PathPatternsRequestCondition paths = mapping.getPathPatternsCondition();
        if (paths == null || !AnnotatedElementUtils.hasAnnotation(method, ExcelExport.class)) {
            return;
        }
        String[] exportPaths = paths.getPatternValues().stream()
                .map(path -> path.endsWith("/") ? path + EXPORT_SEGMENT : path + "/" + EXPORT_SEGMENT)
                .toArray(String[]::new);
        super.registerHandlerMethod(
                handler,
                method,
                mapping.mutate()
                        .paths(exportPaths)
                        .produces(ExcelHttpMessageConverter.XLSX_VALUE)
                        .build());
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
        return info.getProducesCondition().getProducibleMediaTypes().contains(ExcelHttpMessageConverter.XLSX)
                && request.getAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(ExcelExport.class);
    }
}
