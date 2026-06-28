package de.tum.cit.aet.thesis.feedback.dto;

import org.springframework.web.multipart.MultipartFile;

public record ReviewRequestDTO(ProviderCategory providerCategory, MultipartFile file) {
}
