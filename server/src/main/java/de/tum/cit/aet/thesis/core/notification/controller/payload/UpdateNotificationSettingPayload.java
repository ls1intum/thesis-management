package de.tum.cit.aet.thesis.core.notification.controller.payload;

public record UpdateNotificationSettingPayload(
		String name,
		String email
) { }
