package edu.asu.sbs.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class will load the handlebar templates
 *
 * @author Siva
 * @version 1.0
 */
@Component
public class HandlebarsTemplateLoader {

    private Handlebars handlebars;

    /**
     * After the bean initialization, load the templates from the class path
     */
    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/public/templates", ".hbs");
        final Cache<TemplateSource, Template> templateCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000).build();
        setHandlebars(new Handlebars(loader).with((new GuavaTemplateCache(templateCache))));
    }

    public Handlebars getHandlebars() {
        return handlebars;
    }

    public void setHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    /**
     * Get the compiled template
     *
     * @param templateName - name of the template to compile
     * @return the compiled template
     * @throws IOException
     */
    public Template getTemplate(String templateName) throws IOException {
        return this.getHandlebars().compile(templateName);
    }

    /**
     * The Context for the template
     *
     * @param model
     * @return
     */
    public Context getContext(JsonNode model) {

        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE)
                .build();

    }
}

