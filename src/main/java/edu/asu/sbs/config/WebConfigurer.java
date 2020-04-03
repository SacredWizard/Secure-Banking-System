package edu.asu.sbs.config;

import edu.asu.sbs.web.filter.CachingHttpHeadersFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.EnumSet;

import static java.net.URLDecoder.decode;

@Configuration
@Slf4j
public class WebConfigurer implements ServletContextInitializer, WebServerFactoryCustomizer<WebServerFactory> {

    private final Environment env;
    private final SbsProperties sbsProperties;

    public WebConfigurer(Environment env, SbsProperties sbsProperties) {
        this.env = env;
        this.sbsProperties = sbsProperties;
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        if (env.getActiveProfiles().length != 0) {
            log.info("Application config, using profiles: {}", (Object) env.getActiveProfiles());
        }
        EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);
        if (env.acceptsProfiles(Profiles.of(Defaults.Constants.SPRING_PROFILE_PRODUCTION))) {
            initCachingHttpHeadersFilter(servletContext, dispatcherTypes);
        }
        log.info("Application configured");
    }

    private void initCachingHttpHeadersFilter(ServletContext servletContext, EnumSet<DispatcherType> dispatcherTypes) {
        log.debug("Registering Caching HTTP Headers Filter");
        FilterRegistration.Dynamic cachingHttpHeadersFilter =
                servletContext.addFilter("cachingHttpHeadersFilter",
                        new CachingHttpHeadersFilter(sbsProperties));
        cachingHttpHeadersFilter.addMappingForUrlPatterns(dispatcherTypes, true, "/content/*");
        cachingHttpHeadersFilter.setAsyncSupported(true);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = sbsProperties.getCorsConfiguration();
        if (corsConfiguration.getAllowedOrigins() != null && !corsConfiguration.getAllowedOrigins().isEmpty()) {
            log.debug("Registering CORS Filter");
            source.registerCorsConfiguration("/api/**", corsConfiguration);
//            source.registerCorsConfiguration("/management/**", corsConfiguration);
//            source.registerCorsConfiguration("/v2/api-docs", corsConfiguration);
        }
        return new CorsFilter(source);
    }

    @Override
    public void customize(WebServerFactory factory) {
        setMappings(factory);
        setLocationForStaticAssets(factory);
    }

    private void setLocationForStaticAssets(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory) {
            ConfigurableServletWebServerFactory configurableServletWebServerFactory = (ConfigurableServletWebServerFactory) factory;
            File root;
            String prefixPath = resolvePathPrefix();
            root = new File(prefixPath + "build/resources/main/static");
            if (root.exists() && root.isDirectory()) {
                configurableServletWebServerFactory.setDocumentRoot(root);
            }
        }
    }

    private String resolvePathPrefix() {
        String fullExecutablePath;
        try {
            fullExecutablePath = decode(this.getClass().getResource("").getPath(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            fullExecutablePath = this.getClass().getResource("").getPath();
        }
        String rootPath = Paths.get(".").toUri().normalize().getPath();
        String extractedPath = fullExecutablePath.replace(rootPath, "");
        int extractionEndIndex = extractedPath.indexOf("build/");
        if (extractionEndIndex <= 0) {
            return "";
        }
        return extractedPath.substring(0, extractionEndIndex);
    }

    private void setMappings(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory) {
            MimeMappings mimeMappings = new MimeMappings(MimeMappings.DEFAULT);
            mimeMappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            mimeMappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            ConfigurableServletWebServerFactory configurableServletWebServerFactory = (ConfigurableServletWebServerFactory) factory;
            configurableServletWebServerFactory.setMimeMappings(mimeMappings);
        }
    }
}
