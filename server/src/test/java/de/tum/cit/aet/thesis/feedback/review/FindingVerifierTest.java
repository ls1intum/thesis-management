package de.tum.cit.aet.thesis.feedback.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.converter.StructuredOutputConverter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class FindingVerifierTest {

	private static final List<String> PAGES = List.of(
			"Abstract\nI will design, implement, and evaluate Elapsia, a platform for access.",
			"Introduction\nThe estate is modelled as a resource tree with inheriting grants.");

	private static final String GUIDELINES = "<research-group-guidelines>\n"
			+ "- All objectives are in action form and avoid double verbs.\n"
			+ "</research-group-guidelines>";

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.CallResponseSpec responseSpec;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static Finding finding(String title, String quote) {
		return new Finding("MINOR", "WRITING", title, "Fix it.",
				quote == null ? List.of() : List.of(new Location(1, "Abstract", quote)));
	}

	private FindingVerifier verifier(ReviewMeter meter) {
		return new FindingVerifier(chatClient, objectMapper, meter);
	}

	private void stubVerdicts(String json) {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.system(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptSystemSpec>>any()))
				.thenReturn(requestSpec);
		when(requestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
				.thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.responseEntity(FindingVerifierTest.<FindingVerdicts>anyConverter()))
				.thenReturn(new ResponseEntity<>(null, verdicts(json)));
	}

	private FindingVerdicts verdicts(String json) {
		return LenientConverter.parse(json);
	}

	/** Parses with the same lenient converter the verifier hands to the chat client. */
	private static final class LenientConverter {
		static FindingVerdicts parse(String json) {
			return de.tum.cit.aet.thesis.feedback.config.LenientOutputConverter
					.forType(FindingVerdicts.class).convert(json);
		}
	}

	@Test
	void aFindingEnforcingARuleTheGuidelinesNeverStatedIsDropped() {
		// The failure this stage exists for: a real quote from a compliant document, attached to a
		// convention the research group never wrote down.
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"keep\":false,\"reason\":\"No first-person rule in the guidelines.\"},"
				+ "{\"finding\":2,\"keep\":true,\"reason\":\"Double verbs are explicitly forbidden.\"}]}");

		CategoryFindings result = verifier(new ReviewMeter()).verify(
				ReviewCategory.WRITING_STYLE, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(
						finding("Avoid the first person", "I will design, implement, and evaluate Elapsia"),
						finding("Triple-verb objective", "I will design, implement, and evaluate Elapsia"))),
				PAGES);

		assertThat(result.findings()).singleElement()
				.extracting(Finding::title).isEqualTo("Triple-verb objective");
	}

	@Test
	void aCategoryWhereEverythingWasInventedCorrectlyEndsUpEmpty() {
		// Zero findings for a compliant category is the right answer, not a failed verification.
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"keep\":false,\"reason\":\"Invented rule.\"}]}");

		CategoryFindings result = verifier(new ReviewMeter()).verify(
				ReviewCategory.WRITING_FORMATTING, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(finding("Headings must be sentence case",
						"The estate is modelled as a resource tree"))),
				PAGES);

		assertThat(result.findings()).isEmpty();
	}

	@Test
	void checkReportsWhichTierDroppedWhatAndKeepsTheVerifiersReason() {
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"keep\":false,\"reason\":\"Invented rule.\"}]}");
		Finding invented = finding("Headings must be sentence case", "The estate is modelled as a resource tree");
		Finding fabricated = finding("Fabricated claim", "This sentence appears nowhere in the uploaded proposal.");

		CategoryCandidates result = verifier(new ReviewMeter()).check(
				ReviewCategory.WRITING_FORMATTING, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(fabricated, invented)), PAGES);

		assertThat(result.generated()).containsExactly(fabricated, invented);
		assertThat(result.ungrounded()).containsExactly(fabricated);
		assertThat(result.rejected()).singleElement().satisfies(rejected -> {
			assertThat(rejected.finding()).isEqualTo(invented);
			assertThat(rejected.reason()).isEqualTo("Invented rule.");
		});
		assertThat(result.kept()).isEmpty();
		assertThat(result.ruleCheckFailed()).isFalse();
	}

	@Test
	void anUngroundedQuoteIsDroppedInCodeWithoutSpendingACall() {
		ReviewMeter meter = new ReviewMeter();

		CategoryFindings result = verifier(meter).verify(
				ReviewCategory.BIBLIOGRAPHY, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(
						finding("Fabricated claim", "This sentence appears nowhere in the uploaded proposal."))),
				PAGES);

		// Nothing survived the free tier, so the paid tier is never reached.
		assertThat(result.findings()).isEmpty();
		assertThat(meter.llmCalls()).isZero();
		verify(chatClient, never()).prompt();
	}

	@Test
	void aFindingAboutSomethingMissingHasNoQuoteToGroundAndStillReachesTheVerifier() {
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"keep\":true,\"reason\":\"The Schedule section is required.\"}]}");

		CategoryFindings result = verifier(new ReviewMeter()).verify(
				ReviewCategory.STRUCTURE, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(finding("No Schedule section", null))),
				PAGES);

		assertThat(result.findings()).hasSize(1);
	}

	@Test
	void anEmptyCategoryCostsNothing() {
		ReviewMeter meter = new ReviewMeter();

		CategoryFindings result = verifier(meter).verify(
				ReviewCategory.FIGURES, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of()), PAGES);

		assertThat(result.findings()).isEmpty();
		assertThat(meter.llmCalls()).isZero();
	}

	@Test
	void aVerifierThatFailsKeepsTheFindingsRatherThanLosingThem() {
		when(chatClient.prompt()).thenThrow(new RuntimeException("502 Bad Gateway"));

		CategoryFindings result = verifier(new ReviewMeter()).verify(
				ReviewCategory.WRITING_STYLE, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(finding("Triple-verb objective",
						"I will design, implement, and evaluate Elapsia"))),
				PAGES);

		// Failing open: losing real findings to a flaky verifier is worse than keeping an invented
		// one, which is exactly the situation that held before this stage existed.
		assertThat(result.findings()).hasSize(1);
	}

	@Test
	void aMissingOrMalformedVerdictKeepsItsFinding() {
		// The verifier removes fabrications; a verdict it could not parse is not evidence of one.
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"reason\":\"no keep field\"}]}");

		CategoryFindings result = verifier(new ReviewMeter()).verify(
				ReviewCategory.WRITING_STYLE, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(finding("Triple-verb objective",
						"I will design, implement, and evaluate Elapsia"))),
				PAGES);

		assertThat(result.findings()).hasSize(1);
	}

	@Test
	void theCandidateFindingsReachTheModelAsNumberedFencedData() {
		String prompt = verifier(new ReviewMeter()).buildVerificationPrompt(List.of(
				finding("Avoid the first person", "I will design"),
				finding("Triple-verb objective", "I will design")));

		assertThat(prompt)
				.startsWith("<candidate-findings>\n")
				.endsWith("\n</candidate-findings>\n")
				.contains("\"finding\":1")
				.contains("\"finding\":2")
				.contains("Avoid the first person");
	}

	@Test
	void theVerifierIsGivenOnlyTheRulesOfTheCategoryItIsChecking() {
		stubVerdicts("{\"verdicts\":[{\"finding\":1,\"keep\":true,\"reason\":\"ok\"}]}");
		ChatClient.PromptSystemSpec systemSpec = org.mockito.Mockito.mock(ChatClient.PromptSystemSpec.class);
		when(systemSpec.text(org.mockito.ArgumentMatchers.anyString())).thenReturn(systemSpec);

		verifier(new ReviewMeter()).verify(ReviewCategory.WRITING_STYLE, ReviewType.PROPOSAL, GUIDELINES,
				new CategoryFindings(List.of(finding("Triple-verb objective", "I will design"))), PAGES);

		ArgumentCaptor<Consumer<ChatClient.PromptSystemSpec>> captor = consumerCaptor();
		verify(requestSpec).system(captor.capture());
		captor.getValue().accept(systemSpec);

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(systemSpec).text(text.capture());
		// Both halves must be present: without the rules the verifier cannot tell an invented
		// requirement from a real one, which is the whole job.
		assertThat(text.getValue())
				.contains("the specific requirement it relies on is actually stated in those rules")
				.contains("ZERO IS A VALID ANSWER")
				.contains("avoid double verbs");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> ArgumentCaptor<Consumer<T>> consumerCaptor() {
		return ArgumentCaptor.forClass((Class) Consumer.class);
	}

	private static <T> StructuredOutputConverter<T> anyConverter() {
		return any();
	}
}
