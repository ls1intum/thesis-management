package de.tum.cit.aet.thesis.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.tum.cit.aet.thesis.feedback.dto.AIFeedbackDraftDTO;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackCategory;
import de.tum.cit.aet.thesis.thesis.constants.ThesisFeedbackSeverity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class FeedbackMapperTest {

	private static Finding finding(String title, String description, List<Location> locations) {
		return new Finding("MINOR", "WRITING", title, description, locations);
	}

	@Test
	void joinsTitleDescriptionAndTheFirstLocation() {
		String text = FeedbackMapper.toFeedbackText(finding("Missing abstract", "Add one.",
				List.of(new Location(2, "Introduction", "quote"), new Location(9, "Conclusion", "quote"))));

		// Only the first location is rendered: ThesisFeedback.feedback is a plain TEXT column.
		assertThat(text).isEqualTo("Missing abstract — Add one. (Page 2, Introduction)");
	}

	@Test
	void omitsThePartsAFindingDoesNotHave() {
		assertThat(FeedbackMapper.toFeedbackText(finding("Missing abstract", null, List.of())))
				.isEqualTo("Missing abstract");
		assertThat(FeedbackMapper.toFeedbackText(finding("Missing abstract", "  ", List.of(new Location(3, null, null)))))
				.isEqualTo("Missing abstract (Page 3)");
		assertThat(FeedbackMapper.toFeedbackText(finding("Missing abstract", null, List.of(new Location(null, "Abstract", null)))))
				.isEqualTo("Missing abstract (Abstract)");
		// A location carrying only a quote adds no hint, so no empty parentheses appear.
		assertThat(FeedbackMapper.toFeedbackText(finding("Missing abstract", null, List.of(new Location(null, null, "quote")))))
				.isEqualTo("Missing abstract");
	}

	@Test
	void fallsBackWhenTheModelReturnedNothingRenderable() {
		assertThat(FeedbackMapper.toFeedbackText(finding(null, null, List.of())))
				.isEqualTo("AI feedback finding");
	}

	@Test
	void survivesNullEntriesInsideTheLocationList() {
		// The locations come from a model's structured output, which may contain nulls.
		Finding withNullLocation = finding("Missing abstract", null, Arrays.asList(null, new Location(5, null, null)));

		assertThat(FeedbackMapper.toFeedbackText(withNullLocation)).isEqualTo("Missing abstract (Page 5)");
	}

	@Test
	void mapsKnownSeverityAndCategoryTokensCaseInsensitively() {
		AIFeedbackDraftDTO draft = FeedbackMapper.toDraft(
				new Finding("critical", "citation", "Uncited claim", null, List.of()));

		assertThat(draft.severity()).isEqualTo(ThesisFeedbackSeverity.CRITICAL);
		assertThat(draft.category()).isEqualTo(ThesisFeedbackCategory.CITATION);
	}

	@Test
	void fallsBackRatherThanFailingOnTokensOutsideTheEnums() {
		// An unexpected token must not throw away an otherwise complete review.
		AIFeedbackDraftDTO draft = FeedbackMapper.toDraft(
				new Finding("CATASTROPHIC", "vibes", "Odd tokens", null, List.of()));

		assertThat(draft.severity()).isEqualTo(ThesisFeedbackSeverity.MINOR);
		assertThat(draft.category()).isEqualTo(ThesisFeedbackCategory.OTHER);
	}

	@Test
	void leavesSeverityAndCategoryUnsetWhenTheModelOmittedThem() {
		AIFeedbackDraftDTO draft = FeedbackMapper.toDraft(new Finding(null, "  ", "No labels", null, List.of()));

		assertThat(draft.severity()).isNull();
		assertThat(draft.category()).isNull();
	}
}
