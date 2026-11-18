package de.tum.cit.aet.thesis.feedback.review;

import de.tum.cit.aet.thesis.feedback.config.AIFeaturesEnabled;
import de.tum.cit.aet.thesis.feedback.model.AssessmentCategory;
import de.tum.cit.aet.thesis.feedback.model.Finding;
import de.tum.cit.aet.thesis.feedback.model.Location;
import de.tum.cit.aet.thesis.feedback.model.ReviewCategory;
import de.tum.cit.aet.thesis.feedback.model.ReviewResult;
import de.tum.cit.aet.thesis.feedback.progress.ProgressReporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A scripted {@link ThesisReviewer} for live demos: it calls no model and always returns the same
 * four findings, but reports progress exactly like {@link CategoryFanOutReviewer} — every category
 * starts at once, finishes after a staggered delay, and a final consolidation step closes the run —
 * so the review UI shows a realistic run of about fifteen seconds.
 *
 * <p>Demo branch only: as {@code @Primary} it replaces whichever reviewer
 * {@code thesis-management.ai.reviewer} selects, so every deployment of this branch shows the
 * scripted run. This branch is deployed to a test environment and never merged.
 */
@Slf4j
@Service
@Primary
@Conditional(AIFeaturesEnabled.class)
public class DemoReviewer implements ThesisReviewer {

	/** Progress step id of the consolidation step, matching the fan-out reviewer. */
	private static final String MERGE_STEP_ID = "merge";

	/**
	 * When each category finishes, as a fraction of the run. The order is deliberately not the
	 * order the categories start in, so the progress list fills up the way a parallel run does.
	 */
	private static final List<CategoryTiming> CATEGORY_TIMINGS = List.of(
			new CategoryTiming(ReviewCategory.AI_TRANSPARENCY, 0.21),
			new CategoryTiming(ReviewCategory.WRITING_FORMATTING, 0.29),
			new CategoryTiming(ReviewCategory.STRUCTURE, 0.36),
			new CategoryTiming(ReviewCategory.FIGURES, 0.44),
			new CategoryTiming(ReviewCategory.BIBLIOGRAPHY, 0.51),
			new CategoryTiming(ReviewCategory.SCHEDULE, 0.58),
			new CategoryTiming(ReviewCategory.WRITING_STYLE, 0.64),
			new CategoryTiming(ReviewCategory.WRITING_STRUCTURE, 0.71),
			new CategoryTiming(ReviewCategory.PROBLEM_MOTIVATION_OBJECTIVES, 0.79));

	/** Fraction of the run at which the consolidation step starts; it ends with the run. */
	private static final double MERGE_START = 0.81;

	private static final String SUMMARY = "The proposal is ready to submit. Problem, motivation, and objectives are "
			+ "clearly separated, the objectives are phrased in action form, and both figures are referenced and "
			+ "explained in the text. The remaining points are minor: placeholder dates, one redundant term, an "
			+ "unsupported claim, and two schedule iterations that could be more concrete.";

	private static final List<Finding> FINDINGS = List.of(
			new Finding("MINOR", "COMPLETENESS", "Placeholder dates on the title page and in the schedule",
					"The title page and the schedule still give 01.01.2024 as both the start and the submission "
							+ "date, while the iterations run from 08.06.2026 to 25.10.2026. Replace both dates with "
							+ "the actual start and submission dates so the schedule is consistent.",
					List.of(new Location(9, "5 Schedule",
							"The Bachelor thesis starts on 01.01.2024 and the submission is on 01.01.2024."))),
			new Finding("MINOR", "FORMATTING", "Redundant term \"LLM Models\"",
					"\"LLM\" already stands for large language model, so \"LLM Models\" repeats the noun. Use "
							+ "\"Compare Prompting Techniques and LLMs\" in the list of objectives and in the heading "
							+ "of Section 4.3 so the terminology stays consistent.",
					List.of(new Location(8, "4.3 Compare Prompting Techniques and LLM Models",
							"Compare Prompting Techniques and LLM Models"))),
			new Finding("MINOR", "CITATION", "Unsupported claim about writing conventions",
					"The statement that the academic community has not standardized writing conventions is "
							+ "presented as a fact without a source. Add a reference, or phrase it as an observation "
							+ "from the practice of the research group.",
					List.of(new Location(4, "2 Problem", "the academic community has not standardized these conventions"))),
			new Finding("SUGGESTION", "STRUCTURE", "Make the last two iterations measurable",
					"Iteration 9 only lists \"Buffer\", and Iteration 10 lists \"Review the system and check for "
							+ "bugs\" and \"Documentation\". Name concrete, deliverable work items instead, for "
							+ "example a feature that is still open, so every iteration ends with a verifiable result.",
					List.of(new Location(10, "5 Schedule", "Buffer"))));

	private final long durationMillis;

	/**
	 * Creates the demo reviewer.
	 *
	 * @param durationMillis how long one scripted run takes from the first category to the end of the
	 *                       consolidation step
	 */
	public DemoReviewer(@Value("${thesis-management.ai.demo-duration-ms:15000}") long durationMillis) {
		this.durationMillis = durationMillis;
		log.warn("AI review runs in DEMO mode: every review returns the same scripted findings");
	}

	@Override
	public ReviewResult review(ReviewRequest request) {
		ProgressReporter progress = request.progress();
		List<ReviewCategory> categories = List.of(ReviewCategory.values());
		int total = categories.size() + 1;
		long start = System.currentTimeMillis();

		for (int i = 0; i < categories.size(); i++) {
			ReviewCategory category = categories.get(i);
			progress.stepStarted(category.getSlug(), category.getDisplayName(), i + 1, total);
		}
		for (CategoryTiming timing : CATEGORY_TIMINGS) {
			sleepUntil(start, timing.finishedAt());
			progress.stepCompleted(timing.category().getSlug(), categories.indexOf(timing.category()) + 1, total);
		}

		sleepUntil(start, MERGE_START);
		progress.stepStarted(MERGE_STEP_ID, "Consolidating findings", total, total);
		sleepUntil(start, 1.0);
		progress.stepCompleted(MERGE_STEP_ID, total, total);

		return new ReviewResult(AssessmentCategory.GOOD, 87, SUMMARY, FINDINGS);
	}

	@Override
	public String strategy() {
		return "demo";
	}

	/** Blocks until the given fraction of the run has elapsed since {@code start}. */
	private void sleepUntil(long start, double fraction) {
		long wait = start + Math.round(durationMillis * fraction) - System.currentTimeMillis();
		if (wait <= 0) {
			return;
		}
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record CategoryTiming(ReviewCategory category, double finishedAt) {
	}
}
