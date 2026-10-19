package org.vvs.valimbavaka.shared.dto;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Bornes de {@code size} pour {@link PageQuery} : 20 par défaut, 100 au maximum (§7.3). Une route technique
 * (ex. l'export Excel) peut les relever pour la seule requête en cours en posant {@link #REQUEST_ATTRIBUTE}.
 */
public record PageSizeLimit(int defaultSize, int maxSize) {

    public static final PageSizeLimit STANDARD = new PageSizeLimit(20, 100);

    public static final String REQUEST_ATTRIBUTE = PageSizeLimit.class.getName();

    /** Bornes de la requête HTTP en cours ; {@link #STANDARD} hors requête ou sans dérogation. */
    static PageSizeLimit current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object override = attributes == null
                ? null : attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return override instanceof PageSizeLimit limit ? limit : STANDARD;
    }
}
