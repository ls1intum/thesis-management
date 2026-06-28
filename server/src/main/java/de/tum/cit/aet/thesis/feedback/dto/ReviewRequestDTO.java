package de.tum.cit.aet.thesis.feedback.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public record ReviewRequestDTO(
		@NotNull ProviderCategory providerCategory,
		@NotNull MultipartFile file) {
}
