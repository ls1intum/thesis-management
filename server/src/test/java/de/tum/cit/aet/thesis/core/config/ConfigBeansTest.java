package de.tum.cit.aet.thesis.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.tum.cit.aet.thesis.core.utility.StringToArrayConverter;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.Clock;
import java.time.ZoneOffset;

class ConfigBeansTest {

	@Test
	void clockConfig_returnsSystemUtcClock() {
		Clock clock = new ClockConfig().clock();
		assertNotNull(clock);
		assertEquals(ZoneOffset.UTC, clock.getZone());
	}

	@Test
	void thymeleafConfig_returnsHtmlNonCacheableResolver() {
		StringTemplateResolver resolver = new ThymeleafConfig().stringTemplateResolver();
		assertNotNull(resolver);
		assertEquals(TemplateMode.HTML, resolver.getTemplateMode());
		assertFalse(resolver.isCacheable());
		assertEquals(1, resolver.getOrder());
	}

	@Test
	void webConfig_addsStringToArrayConverter() {
		StringToArrayConverter converter = new StringToArrayConverter();
		WebConfig webConfig = new WebConfig(converter);

		DefaultFormattingConversionService registry = new DefaultFormattingConversionService();
		webConfig.addFormatters(registry);

		assertTrue(registry.canConvert(String.class, String[].class), "Expected converter to be registered");
		Object converted = registry.convert("a,b", String[].class);
		assertInstanceOf(String[].class, converted);
		String[] result = (String[]) converted;
		assertEquals(2, result.length);
		assertEquals("a", result[0]);
		assertEquals("b", result[1]);
	}
}
