package de.tum.cit.aet.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.constants.UploadFileType;
import de.tum.cit.aet.thesis.exception.UploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

class UploadServiceTest {

	@TempDir
	Path tempDir;

	private UploadService uploadService;

	@BeforeEach
	void setUp() {
		uploadService = new UploadService(tempDir.toString());
	}

	@Nested
	class StoreFile {
		@Test
		void store_ValidPdf_ReturnsFilename() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.pdf", "application/pdf", new byte[]{1, 2, 3, 4}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.PDF);
			assertThat(filename).endsWith(".pdf");
			assertThat(filename).isNotBlank();
		}

		@Test
		void store_ValidImage_ReturnsFilename() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.png", "image/png", new byte[]{1, 2, 3, 4}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.IMAGE);
			assertThat(filename).endsWith(".png");
		}

		@Test
		void store_EmptyFile_ThrowsException() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "empty.pdf", "application/pdf", new byte[0]
			);

			assertThatThrownBy(() -> uploadService.store(file, 1024 * 1024, UploadFileType.PDF))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("empty");
		}

		@Test
		void store_OversizedFile_ThrowsException() {
			byte[] largeContent = new byte[2 * 1024 * 1024];
			MockMultipartFile file = new MockMultipartFile(
					"file", "large.pdf", "application/pdf", largeContent
			);

			assertThatThrownBy(() -> uploadService.store(file, 1024 * 1024, UploadFileType.PDF))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("size");
		}

		@Test
		void store_InvalidExtensionForPdf_ThrowsException() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.exe", "application/octet-stream", new byte[]{1, 2, 3}
			);

			assertThatThrownBy(() -> uploadService.store(file, 1024 * 1024, UploadFileType.PDF))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("type");
		}

		@Test
		void store_InvalidExtensionForImage_ThrowsException() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.pdf", "application/pdf", new byte[]{1, 2, 3}
			);

			assertThatThrownBy(() -> uploadService.store(file, 1024 * 1024, UploadFileType.IMAGE))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("type");
		}

		@Test
		void store_MaliciousFilename_StoredWithSafeHashedName() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "../../../etc/passwd.pdf", "application/pdf", new byte[]{1, 2, 3}
			);

			// Original filename is ignored — stored filename is a SHA hash of the content,
			// so path traversal via filename is not possible.
			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.PDF);
			assertThat(filename).doesNotContain("..");
			assertThat(filename).doesNotContain("/");
		}

		@Test
		void store_AnyTypeAllowed_Success() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.docx", "application/msword", new byte[]{1, 2, 3}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.ANY);
			assertThat(filename).endsWith(".docx");
		}
	}

	@Nested
	class LoadFile {
		@Test
		void load_ValidFile_ReturnsResource() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "load.pdf", "application/pdf", new byte[]{1, 2, 3, 4, 5}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.PDF);

			FileSystemResource resource = uploadService.load(filename);
			assertThat(resource).isNotNull();
			assertThat(resource.exists()).isTrue();
		}

		@Test
		void load_PathTraversal_ThrowsException() {
			assertThatThrownBy(() -> uploadService.load("../../../etc/passwd"))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("relative path");
		}

		@Test
		void load_NonExistentFile_ThrowsException() {
			assertThatThrownBy(() -> uploadService.load("nonexistent-file.pdf"))
					.isInstanceOf(UploadException.class);
		}
	}
}
