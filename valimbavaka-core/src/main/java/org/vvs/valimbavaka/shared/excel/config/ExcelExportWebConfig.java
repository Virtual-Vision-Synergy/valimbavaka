package org.vvs.valimbavaka.shared.excel.config;

import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.vvs.valimbavaka.shared.excel.ExcelExporter;
import org.vvs.valimbavaka.shared.excel.web.ExcelHttpMessageConverter;
import org.vvs.valimbavaka.shared.excel.web.ExportAwareHandlerMapping;

/**
 * Branche l'export Excel dans Spring MVC. Une seule {@link WebMvcRegistrations} peut fournir le
 * {@link RequestMappingHandlerMapping} : toute autre personnalisation du mapping doit être fusionnée ici.
 */
@Configuration(proxyBeanMethods = false)
public class ExcelExportWebConfig implements WebMvcRegistrations, WebMvcConfigurer {

    private final ExcelProperties properties;
    private final ExcelExporter exporter;
    private final ApplicationEventPublisher eventPublisher;

    public ExcelExportWebConfig(ExcelProperties properties, ExcelExporter exporter,
            ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.exporter = exporter;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ExportAwareHandlerMapping(properties.maxRows());
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.addCustomConverter(new ExcelHttpMessageConverter(exporter, eventPublisher));
    }
}
