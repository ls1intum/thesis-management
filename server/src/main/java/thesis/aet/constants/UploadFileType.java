package de.tum.cit.aet.thesis.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UploadFileType {
    PDF("PDF"),
    IMAGE("IMAGE"),
    ANY("ANY");

    private final String value;
}
