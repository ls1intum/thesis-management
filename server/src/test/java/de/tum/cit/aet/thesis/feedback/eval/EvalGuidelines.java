package de.tum.cit.aet.thesis.feedback.eval;

import de.tum.cit.aet.thesis.feedback.entity.jsonb.CategoryGuidelines;
import de.tum.cit.aet.thesis.feedback.entity.jsonb.StructuredGuidelines;

import java.util.List;

/**
 * The research group's proposal guidelines, held fixed in code for the whole sweep.
 *
 * <p>In the running application guidelines come from the database, per research group. An
 * evaluation must not: the comparison isolates the review strategy, so every arm has to be judging
 * against byte-identical criteria. A guideline edit between two runs would show up as a difference
 * in output and be read as a difference between the strategies.
 *
 * <p>Category slugs match {@code ReviewCategory.getSlug()}, so the same block renders for the
 * fan-out and for the agent's {@code get_category_rules} tool.
 */
@SuppressWarnings("checkstyle:LineLength")
public final class EvalGuidelines {

	private EvalGuidelines() {
	}

	public static final String OVERVIEW = """
			Official proposal guidelines of the research group. A proposal is a high-level overview \
			(about 4-6 text pages) describing the problem, the motivation, the objectives, and a \
			preliminary schedule. It must cite at least 6-8 peer-reviewed publications and include at \
			least two diagrams/mockups (at least one suitable UML diagram, no sequence diagrams). \
			Expected sections: Abstract, Introduction, Problem, Motivation, Objective (with \
			subsections), Schedule, Bibliography, and an AI Transparency Statement.""";

	/**
	 * The guidelines every arm of the sweep judges against.
	 *
	 * @return the fixed structured guidelines
	 */
	public static StructuredGuidelines build() {
		return new StructuredGuidelines(OVERVIEW, List.of(
				new CategoryGuidelines("structure", List.of(
						"Required sections: Abstract, Introduction, Problem, Motivation, Objective (with subsections), Schedule, Bibliography, Transparency statement.",
						"Total proposal text should be at most 4-6 pages; abstract 1/3-1/2 page; introduction < 1 page; problem and motivation each at most 3/4 page.",
						"Objectives (Section 4) start with a short intro and a bullet list of 3-4 objectives, then one subsection per objective (at least two paragraphs each).")),
				new CategoryGuidelines("problem-motivation-objectives", List.of(
						"The Problem section describes only problems and their negative consequences for identified actors — no solutions.",
						"The Motivation is visionary and does not repeat the Problem.",
						"All objectives (including headings) are in action form and avoid double verbs (not 'Design and implement').")),
				new CategoryGuidelines("bibliography", List.of(
						"Cite at least 6-8 peer-reviewed publications; internet sources go in footnotes, not the bibliography.",
						"Use a consistent citation style (ideally alpha, [ABC12]); place citations before the full stop.")),
				new CategoryGuidelines("figures", List.of(
						"Include at least two figures/diagrams/mockups; at least one is a suitable UML diagram; no sequence diagrams.",
						"Every figure is referenced in the text and has a long, informative caption.")),
				new CategoryGuidelines("writing-style", List.of(
						"Use active voice; avoid contractions, filler words, and superlatives.",
						"Do not start sentences with 'As', 'Since', 'To', 'In order to', or 'Because'; define abbreviations before use.")),
				new CategoryGuidelines("writing-structure", List.of(
						"Write mainly in prose (5-8 line paragraphs, one idea each), not bullet points — except in the Schedule and objective lists.",
						"Every section has introductory text before its first subsection; numbering is at most three digits.")),
				new CategoryGuidelines("writing-formatting", List.of(
						"Headings use title case (respecting the capitalization rules of the heading's language).",
						"Terminology is consistent for the same concept throughout the author's prose.")),
				new CategoryGuidelines("ai-transparency", List.of(
						"Include a specific, first-person AI transparency statement naming the tools used, their purpose, and the sections involved.",
						"The statement confirms the author carefully checked all AI-generated text.")),
				new CategoryGuidelines("schedule", List.of(
						"Divide work into 2-4 week iterations with measurable, vertically-integrated deliverables following agile principles.",
						"Do not include a requirements-gathering sprint or thesis-writing/documentation tasks in the schedule."))));
	}
}
