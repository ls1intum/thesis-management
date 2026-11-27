package de.tum.cit.aet.thesis.core.utility;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Settings of the in-memory mail retry ({@code thesis-management.mail.retry.*}).
 *
 * @param delays waiting time before each retry of a temporarily failed email; the number of entries is the
 *               maximum number of retries, an empty list switches the retry off
 * @param queueCapacity maximum number of emails waiting for a retry; the oldest one is dropped when it is exceeded
 */
@ConfigurationProperties(prefix = "thesis-management.mail.retry")
public record MailRetryProperties(
		@DefaultValue({"1m", "5m", "15m", "30m", "1h"}) List<Duration> delays,
		@DefaultValue("1000") int queueCapacity
) {
}
