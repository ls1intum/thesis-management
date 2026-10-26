package de.tum.cit.aet.thesis.core.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.core.exception.UploadException;
import de.tum.cit.aet.thesis.core.upload.constants.UploadFileType;
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
		void store_DocumentTypeAllowed_Success() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "test.docx", "application/msword", new byte[]{1, 2, 3}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.DOCUMENT);
			assertThat(filename).endsWith(".docx");
		}

		@Test
		void store_KeynoteAsDocument_Success() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "presentation.key", "application/vnd.apple.keynote", new byte[]{1, 2, 3}
			);

			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.DOCUMENT);
			assertThat(filename).endsWith(".key");
		}

		@Test
		void store_UnsupportedExtension_ErrorMessageListsAllowedExtensions() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "archive.rar", "application/x-rar-compressed", new byte[]{1, 2, 3}
			);

			assertThatThrownBy(() -> uploadService.store(file, 1024 * 1024, UploadFileType.DOCUMENT))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("Unsupported file type .rar")
					.hasMessageContaining(".pdf")
					.hasMessageContaining(".pptx")
					.hasMessageContaining(".key");
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
					.hasMessageContaining("outside upload directory");
		}

		@Test
		void load_NonExistentFile_ThrowsException() {
			assertThatThrownBy(() -> uploadService.load("nonexistent-file.pdf"))
					.isInstanceOf(UploadException.class);
		}
	}

	@Nested
	class StoreBytes {
		@Test
		void storeBytes_ValidContent_ReturnsHashedFilename() {
			byte[] payload = new byte[] { 1, 2, 3, 4 };
			String filename = uploadService.storeBytes(payload, "pdf", 1024 * 1024);
			assertThat(filename).endsWith(".pdf");
			assertThat(filename).doesNotContain("..");
		}

		@Test
		void storeBytes_EmptyBytes_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(new byte[0], "pdf", 1024))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("empty");
		}

		@Test
		void storeBytes_NullBytes_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(null, "pdf", 1024))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("empty");
		}

		@Test
		void storeBytes_TooLarge_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(new byte[] { 1, 2, 3, 4 }, "pdf", 2))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("size");
		}

		@Test
		void storeBytes_TraversalExtension_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(new byte[] { 1 }, "../etc", 1024))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("Invalid file extension");
		}

		@Test
		void storeBytes_NullExtension_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(new byte[] { 1 }, null, 1024))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("Invalid file extension");
		}

		@Test
		void storeBytes_SlashExtension_ThrowsException() {
			assertThatThrownBy(() -> uploadService.storeBytes(new byte[] { 1 }, "a/b", 1024))
					.isInstanceOf(UploadException.class)
					.hasMessageContaining("Invalid file extension");
		}
	}

	@Nested
	class DeleteFile {
		@Test
		void deleteFile_ExistingFile_RemovesIt() {
			MockMultipartFile file = new MockMultipartFile(
					"file", "del.pdf", "application/pdf", new byte[] { 1, 2, 3 }
			);
			String filename = uploadService.store(file, 1024 * 1024, UploadFileType.PDF);

			uploadService.deleteFile(filename);

			assertThatThrownBy(() -> uploadService.load(filename))
					.isInstanceOf(UploadException.class);
		}

		@Test
		void deleteFile_NullOrBlank_DoesNothing() {
			uploadService.deleteFile(null);
			uploadService.deleteFile("");
			uploadService.deleteFile("   ");
		}

		@Test
		void deleteFile_TraversalPath_Ignored() {
			uploadService.deleteFile("../foo.pdf");
		}

		@Test
		void deleteFile_NonExistent_DoesNothing() {
			uploadService.deleteFile("does-not-exist.pdf");
		}
	}
}
