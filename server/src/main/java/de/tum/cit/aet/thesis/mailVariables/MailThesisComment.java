package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.ThesisComment;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

public record MailThesisComment(
        String creatorFirstName,
        String creatorLastName,
        String message
) {
    public static MailThesisComment fromComment(ThesisComment comment) {
        if (comment == null) {
            return new MailThesisComment("", "", "");
        }

        User creator = comment.getCreatedBy();

        return new MailThesisComment(
                valueOrEmpty(creator != null ? creator.getFirstName() : null),
                valueOrEmpty(creator != null ? creator.getLastName() : null),
                valueOrEmpty(comment.getMessage())
        );
    }

    public static List<MailVariableDto> templateVariables() {
        return List.of(
                new MailVariableDto("Comment Creator First Name", "[[${comment.creatorFirstName}]]", "Max", "Thesis Comment"),
                new MailVariableDto("Comment Creator Last Name", "[[${comment.creatorLastName}]]", "Mustermann", "Thesis Comment"),
                new MailVariableDto("Comment Message", "[[${comment.message}]]", "Comment text", "Thesis Comment")
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
