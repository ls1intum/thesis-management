package de.tum.cit.aet.thesis.service;

import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.exception.UploadException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Service
public class UploadService {
	private static final Logger log = LoggerFactory.getLogger(UploadService.class);
	private final Path rootLocation;

	/**
	 * Initializes the upload directory from the configured storage location, creating it if necessary.
	 *
	 * @param uploadLocation the file system path for uploads
	 */
	@Autowired
	public UploadService(@Value("${thesis-management.storage.upload-location}") String uploadLocation) {
		this.rootLocation = Path.of(uploadLocation);

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

			if (filename.contains("..")) {
				throw new UploadException("Cannot store file with relative path outside current directory");
			}

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

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
			if (filename.contains("..")) {
				throw new UploadException("Cannot load file with relative path outside current directory");
			}

			FileSystemResource file =  new FileSystemResource(rootLocation.resolve(filename));

			file.contentLength();

			return file;
		} catch (IOException e) {
			throw new UploadException("Failed to load file", e);
		}
	}

	/**
	 * Stores raw bytes as a file with the given extension, returning the content-hashed filename.
	 *
	 * @param bytes the file content
	 * @param extension the file extension (e.g. "png")
	 * @param maxSize the maximum allowed size in bytes
	 * @return the content-hashed filename
	 */
	public String storeBytes(byte[] bytes, String extension, int maxSize) {
		try {
			if (bytes == null || bytes.length == 0) {
				throw new UploadException("Failed to store empty file");
			}

			if (bytes.length > maxSize) {
				throw new UploadException("File size exceeds the maximum allowed size");
			}

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(bytes);
			String hash = HexFormat.of().formatHex(hashBytes);
			String filename = hash + "." + extension;

			Files.write(rootLocation.resolve(filename), bytes);
			return filename;
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new UploadException("Failed to store file", e);
		}
	}

	/**
	 * Deletes the specified file from the upload directory on a best-effort basis.
	 *
	 * @param filename the file to delete
	 */
	public void deleteFile(String filename) {
		if (filename == null || filename.isBlank()) {
			return;
		}
		if (filename.contains("..")) {
			return;
		}
		try {
			Path resolved = rootLocation.resolve(filename).normalize();
			if (!resolved.startsWith(rootLocation.normalize())) {
				return;
			}
			Files.deleteIfExists(resolved);
		} catch (IOException e) {
			log.warn("Failed to delete file {}: {}", filename, e.getMessage());
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
