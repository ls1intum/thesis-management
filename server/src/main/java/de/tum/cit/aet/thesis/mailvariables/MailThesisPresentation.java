package de.tum.cit.aet.thesis.mailvariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.utility.DataFormatter;

import java.util.List;

/** Mail placeholder model for thesis presentation variables. */
public record MailThesisPresentation(
		String creatorFirstName,
		String creatorLastName,
		String type,
		String scheduledAt,
		String location,
		String streamUrl,
		String language
) {
	/**
	 * Builds a mail-safe thesis presentation model.
	 *
	 * @param presentation the thesis presentation entity
	 * @return mapped thesis presentation mail model
	 */
	public static MailThesisPresentation fromPresentation(ThesisPresentation presentation) {
		if (presentation == null) {
			return new MailThesisPresentation("", "", "", "", "", "", "");
		}

		User creator = presentation.getCreatedBy();

		return new MailThesisPresentation(
				valueOrEmpty(creator != null ? creator.getFirstName() : null),
				valueOrEmpty(creator != null ? creator.getLastName() : null),
				valueOrEmpty(DataFormatter.formatEnum(presentation.getType())),
				valueOrEmpty(DataFormatter.formatDateTime(presentation.getScheduledAt())),
				valueOrEmpty(DataFormatter.formatOptionalString(presentation.getLocation())),
				valueOrEmpty(DataFormatter.formatOptionalString(presentation.getStreamUrl())),
				valueOrEmpty(DataFormatter.formatConstantName(presentation.getLanguage()))
		);
	}

	/**
	 * Returns all selectable template variables for thesis presentations.
	 *
	 * @return thesis presentation variable descriptors
	 */
	public static List<MailVariableDto> templateVariables() {
		return List.of(
				new MailVariableDto("Presentation Creator First Name", "[[${presentation.creatorFirstName}]]", "Max", "Presentation"),
				new MailVariableDto("Presentation Creator Last Name", "[[${presentation.creatorLastName}]]", "Mustermann", "Presentation"),
				new MailVariableDto("Presentation Type", "[[${presentation.type}]]", "Final Presentation", "Presentation"),
				new MailVariableDto("Presentation Date", "[[${presentation.scheduledAt}]]", "01.10.2024 14:00:00 CET", "Presentation"),
				new MailVariableDto("Presentation Location", "[[${presentation.location}]]", "Room 101", "Presentation"),
				new MailVariableDto("Presentation Stream URL", "[[${presentation.streamUrl}]]", "https://video.example.com", "Presentation"),
				new MailVariableDto("Presentation Language", "[[${presentation.language}]]", "English", "Presentation"),
				new MailVariableDto("Presentation URL", "[[${presentationUrl}]]", "https://thesis-management.com/presentations/123", "Presentation")
		);
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
