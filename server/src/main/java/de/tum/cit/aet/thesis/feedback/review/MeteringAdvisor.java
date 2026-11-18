package de.tum.cit.aet.thesis.feedback.review;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

/**
 * Records each model call on the run's {@link ReviewMeter} as soon as the raw response is back.
 *
 * <p>Structured output is converted after the advisor chain returns, so a call whose answer cannot
 * be parsed throws before the caller ever sees the response. Recording it here instead means a paid
 * call and its tokens are counted even when the conversion then fails.
 */
final class MeteringAdvisor implements CallAdvisor {

	private final ReviewMeter meter;

	MeteringAdvisor(ReviewMeter meter) {
		this.meter = meter;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		ChatClientResponse response = chain.nextCall(request);
		meter.recordCall(response.chatResponse());
		return response;
	}

	@Override
	public String getName() {
		return "review-metering";
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
