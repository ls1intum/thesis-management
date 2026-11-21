package de.tum.cit.aet.thesis.feedback.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class AIFeaturesEnabled implements Condition {
	public static final String PROPERTY = "thesis-management.ai.enabled";

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return Boolean.TRUE.equals(
				context.getEnvironment().getProperty(PROPERTY, Boolean.class, false));
	}
}
