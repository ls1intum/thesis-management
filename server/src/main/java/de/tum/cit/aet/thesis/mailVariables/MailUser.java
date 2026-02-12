package de.tum.cit.aet.thesis.mailVariables;

import de.tum.cit.aet.thesis.dto.MailVariableDto;
import de.tum.cit.aet.thesis.entity.User;

import java.util.List;

public record MailUser(
        String firstName,
        String lastName
) {
    public static MailUser fromUser(User user) {
        if (user == null) {
            return new MailUser("", "");
        }

        return new MailUser(valueOrEmpty(user.getFirstName()), valueOrEmpty(user.getLastName()));
    }

    public static List<MailVariableDto> templateVariables(String placeholder, String labelPrefix, String group) {
        return List.of(
                new MailVariableDto(labelPrefix + " First Name", "[[${" + placeholder + ".firstName}]]", "Max", group),
                new MailVariableDto(labelPrefix + " Last Name", "[[${" + placeholder + ".lastName}]]", "Mustermann", group)
        );
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
