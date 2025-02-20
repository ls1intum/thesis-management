package de.tum.cit.aet.thesis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import de.tum.cit.aet.thesis.utility.StringToArrayConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StringToArrayConverter stringToArrayConverter;

    @Autowired
    public WebConfig(StringToArrayConverter stringToArrayConverter) {
        this.stringToArrayConverter = stringToArrayConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToArrayConverter);
    }
}