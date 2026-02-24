package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.exception.UploadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/** Handles file uploads and retrieval, including size and type validation and content-based hashing. */
@Slf4j
@Service
public class UploadService {
	private final Path rootLocation;

	/**
	 * Initializes the upload directory from the configured storage location, creating it if necessary.
	 *
	 * @param uploadLocation the file system path for uploads
	 */
	@Autowired
	public UploadService(@Value("${thesis-management.storage.upload-location}") String uploadLocation) {
		this.rootLocation = Path.of(uploadLocation).toAbsolutePath().normalize();

		File uploadDirectory = rootLocation.toFile();

		if (!uploadDirectory.exists() && !uploadDirectory.mkdirs()) {
			throw new UploadException("Failed to create upload directory");
		}
	}

	/**
	 * Validates and stores the uploaded file, returning the content-hashed filename.
	 *
	 * @param file the file to store
	 * @param maxSize the maximum allowed file size in bytes
	 * @param type the allowed upload file type
	 * @return the content-hashed filename
	 */
	public String store(MultipartFile file, Integer maxSize, UploadFileType type) {
		try {
			if (file.isEmpty()) {
				throw new UploadException("Failed to store empty file");
			}

			if (file.getSize() > maxSize) {
				throw new UploadException("File size exceeds the maximum allowed size");
			}

			Set<String> allowedExtensions = null;

			if (type == UploadFileType.PDF) {
				allowedExtensions = Set.of("pdf");
			}

			if (type == UploadFileType.IMAGE) {
				allowedExtensions = Set.of(
						"jpg",
						"jpeg",
						"png",
						"gif",
						"webp"
				);
			}

			String originalFilename = file.getOriginalFilename();
			String extension = FilenameUtils.getExtension(originalFilename);

			if (allowedExtensions != null && !allowedExtensions.contains(extension)) {
				throw new UploadException("File type not allowed");
			}

			String filename = StringUtils.cleanPath(computeFileHash(file) + "." + extension);
			Path target = rootLocation.resolve(filename).normalize();

			if (!target.startsWith(rootLocation)) {
				throw new UploadException("Cannot store file outside upload directory");
			}

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);

				return filename;
			}
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new UploadException("Failed to store file", e);
		}
	}

	/**
	 * Loads and returns a previously stored file as a FileSystemResource.
	 *
	 * @param filename the name of the file to load
	 * @return the file as a FileSystemResource
	 */
	public FileSystemResource load(String filename) {
		try {
			Path resolved = rootLocation.resolve(filename).normalize();

			if (!resolved.startsWith(rootLocation)) {
				throw new UploadException("Cannot load file outside upload directory");
			}

			FileSystemResource file = new FileSystemResource(resolved);

			file.contentLength();

			return file;
		} catch (IOException e) {
			throw new UploadException("Failed to load file", e);
		}
	}

	private String computeFileHash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (InputStream inputStream = file.getInputStream()) {
			byte[] fileBytes = inputStream.readAllBytes();
			byte[] hashBytes = digest.digest(fileBytes);

			return HexFormat.of().formatHex(hashBytes);
		}
	}
}
