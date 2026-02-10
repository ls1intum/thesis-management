package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.service.AccessManagementService.KeycloakUserInformation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeycloakUserInformationTest {

    @Test
    void getMatriculationNumber_WithValidAttribute_ReturnsValue() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                Map.of("matrikelnr", List.of("12345678"))
        );

        assertEquals("12345678", info.getMatriculationNumber());
    }

    @Test
    void getMatriculationNumber_WithNullAttributes_ReturnsNull() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                null
        );

        assertNull(info.getMatriculationNumber());
    }

    @Test
    void getMatriculationNumber_WithEmptyAttributes_ReturnsNull() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                Map.of()
        );

        assertNull(info.getMatriculationNumber());
    }

    @Test
    void getMatriculationNumber_WithEmptyValueList_ReturnsNull() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                Map.of("matrikelnr", List.of())
        );

        assertNull(info.getMatriculationNumber());
    }

    @Test
    void getMatriculationNumber_WithMultipleValues_ReturnsFirst() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                Map.of("matrikelnr", List.of("11111111", "22222222"))
        );

        assertEquals("11111111", info.getMatriculationNumber());
    }

    @Test
    void getMatriculationNumber_WithOtherAttributesButNoMatrikelnr_ReturnsNull() {
        KeycloakUserInformation info = new KeycloakUserInformation(
                UUID.randomUUID(), "ab12cde", "First", "Last", "test@test.com",
                Map.of("someOtherAttr", List.of("value"))
        );

        assertNull(info.getMatriculationNumber());
    }
}
