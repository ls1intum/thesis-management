package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisPresentation;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.utility.DataFormatter;

import java.util.List;

public record MailThesisPresentation(
		String creatorFirstName,
		String creatorLastName,
		String type,
		String scheduledAt,
		String location,
		String streamUrl,
		String language
) {
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
