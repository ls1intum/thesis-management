package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;

import java.util.List;

public record MailInterview(
        String inviteUrl
) {
    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Interview Invite URL", "[[${inviteUrl}]]", "https://thesis-management.com/interviews/invite/abc123", "Interview")
        );
    }
}
