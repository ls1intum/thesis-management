package de.tum.cit.aet.thesis.feedback.config;

import de.tum.cit.aet.thesis.feedback.review.ReviewMetricsRecorder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/** Supplies the default {@link ReviewMetricsRecorder} unless something else already has. */
@Configuration
@Conditional(AIFeaturesEnabled.class)
public class ReviewMetricsConfig {

	@Bean
	@ConditionalOnMissingBean
	ReviewMetricsRecorder reviewMetricsRecorder() {
		return ReviewMetricsRecorder.LOGGING;
	}
}
