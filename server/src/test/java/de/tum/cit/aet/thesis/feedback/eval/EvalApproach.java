package de.tum.cit.aet.thesis.feedback.eval;

import java.util.Arrays;
import java.util.List;

/**
 * The prompting techniques a sweep compares, each as the server configuration that produces it.
 *
 * <p>An arm is not always a reviewer of its own. The two fan-out arms run the same
 * {@code CategoryFanOutReviewer} and differ only in {@code thesis-management.ai.verify-findings},
 * which is the point: the benchmark prices the verification stage by switching it, not by comparing
 * two implementations. Both fan-out arms therefore name the setting explicitly — a bare
 * {@code category-fan-out} would inherit whatever the property defaults to, and a change of that
 * default would silently redefine what an earlier sweep measured.
 */
public enum EvalApproach {
	/** Every criterion and the whole document in one call. The naive baseline. */
	SINGLE_PROMPT("single-prompt", "single-prompt", false),
	/** One call per category plus the merge, without checking findings against their rules. */
	CATEGORY_FAN_OUT_UNVERIFIED("category-fan-out-unverified", "category-fan-out", false),
	/** The fan-out with each category's findings verified before the merge, as deployed. */
	CATEGORY_FAN_OUT_VERIFIED("category-fan-out-verified", "category-fan-out", true),
	/** One agent driving its own tool calls. */
	AGENTIC("agentic", "agentic", false);

	private final String id;
	private final String reviewer;
	private final boolean verifyFindings;

	EvalApproach(String id, String reviewer, boolean verifyFindings) {
		this.id = id;
		this.reviewer = reviewer;
		this.verifyFindings = verifyFindings;
	}

	/**
	 * The name a sweep is configured with, and the directory segment its results are written under.
	 *
	 * @return the approach id
	 */
	public String id() {
		return id;
	}

	/**
	 * The value of {@code thesis-management.ai.reviewer} this arm boots with.
	 *
	 * @return the reviewer property value
	 */
	public String reviewer() {
		return reviewer;
	}

	/**
	 * The value of {@code thesis-management.ai.verify-findings} this arm boots with. Only the fan-out
	 * reads it; the other arms pass {@code false} so no arm depends on the property's default.
	 *
	 * @return whether findings are verified before the merge
	 */
	public boolean verifyFindings() {
		return verifyFindings;
	}

	/**
	 * Every approach id, in the order a full sweep runs them.
	 *
	 * @return the ids
	 */
	public static List<String> ids() {
		return Arrays.stream(values()).map(EvalApproach::id).toList();
	}

	/**
	 * Resolves an approach from its id.
	 *
	 * @param id the approach id
	 * @return the approach
	 * @throws IllegalArgumentException if no approach has that id
	 */
	public static EvalApproach fromId(String id) {
		for (EvalApproach approach : values()) {
			if (approach.id.equals(id)) {
				return approach;
			}
		}
		throw new IllegalArgumentException("Unknown approach '" + id + "'; expected one of " + ids());
	}
}
