package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisAssessment;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

public record MailThesisAssessment(
        String creatorFirstName,
        String creatorLastName,
        String summary,
        String positives,
        String negatives,
        String gradeSuggestion
) {
    public static MailThesisAssessment fromAssessment(ThesisAssessment assessment) {
        if (assessment == null) {
            return new MailThesisAssessment("", "", "", "", "", "");
        }

        User creator = assessment.getCreatedBy();

        return new MailThesisAssessment(
                valueOrEmpty(creator != null ? creator.getFirstName() : null),
                valueOrEmpty(creator != null ? creator.getLastName() : null),
                valueOrEmpty(assessment.getSummary()),
                valueOrEmpty(assessment.getPositives()),
                valueOrEmpty(assessment.getNegatives()),
                valueOrEmpty(assessment.getGradeSuggestion())
        );
    }

    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Assessment Creator First Name", "[[${assessment.creatorFirstName}]]", "Max", "Assessment"),
                new MailVariableDto("Assessment Creator Last Name", "[[${assessment.creatorLastName}]]", "Mustermann", "Assessment"),
                new MailVariableDto("Assessment Summary", "[[${assessment.summary}]]", "Strong work", "Assessment"),
                new MailVariableDto("Assessment Positives", "[[${assessment.positives}]]", "Good structure", "Assessment"),
                new MailVariableDto("Assessment Negatives", "[[${assessment.negatives}]]", "Needs more evaluation", "Assessment"),
                new MailVariableDto("Assessment Grade Suggestion", "[[${assessment.gradeSuggestion}]]", "1.3", "Assessment")
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
