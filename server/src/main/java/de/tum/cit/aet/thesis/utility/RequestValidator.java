package de.tum.cit.aet.thesis.utility;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import de.tum.cit.aet.thesis.exception.request.ResourceInvalidParametersException;

import java.util.Set;

public class RequestValidator {
    public static String validateStringMaxLength(String value, int maxLength) {
        if (value == null) {
            throw new ResourceInvalidParametersException("Required string is null");
        }

        if (value.length() > maxLength) {
            throw new ResourceInvalidParametersException("String exceeds maximum length of " + maxLength + " characters");
        }

        return value;
    }

    public static String validateStringMaxLengthAllowNull(String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return validateStringMaxLength(value, maxLength);
    }

    public static Set<String> validateStringSetItemMaxLength(Set<String> value, int maxLength) {
        if (value == null) {
            throw new ResourceInvalidParametersException("Required set is null");
        }

        for (String s : value) {
            if (s.length() > maxLength) {
                throw new ResourceInvalidParametersException("String exceeds maximum length of " + maxLength + " characters");
            }
        }

        return value;
    }

    public static Set<String> validateStringSetItemMaxLengthAllowNull(Set<String> value, int maxLength) {
        if (value == null) {
            return null;
        }

        return validateStringSetItemMaxLength(value, maxLength);
    }

    public static String validateEmail(String value) {
        if (value == null || value.length() > 200) {
            throw new ResourceInvalidParametersException("Required email is missing");
        }

        try {
            InternetAddress emailAddr = new InternetAddress(value);

            emailAddr.validate();

            return value;
        } catch (AddressException ex) {
            throw new ResourceInvalidParametersException("Invalid email address");
        }
    }

    public static String validateEmailAllowNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return validateEmail(value);
    }

    public static <T> T validateNotNull(T value) {
        if (value == null) {
            throw new ResourceInvalidParametersException("Required value is null");
        }

        return value;
    }
}