package de.tum.cit.aet.thesis.feedback.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.tum.cit.aet.thesis.feedback.model.ReviewType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class EvalCorpusTest {

	private static void pdf(Path directory, String name) throws IOException {
		Files.createDirectories(directory);
		Files.writeString(directory.resolve(name), "%PDF-1.4");
	}

	@Test
	void theLayoutDecidesWhetherADocumentIsReviewedAsAProposalOrAThesis(@TempDir Path root) throws IOException {
		pdf(root.resolve("proposal"), "b.pdf");
		pdf(root.resolve("proposal"), "a.pdf");
		pdf(root.resolve("thesis"), "c.pdf");

		EvalCorpus corpus = EvalCorpus.discover(root);

		assertThat(corpus.documents()).extracting(EvalCorpus.EvalDocument::id, EvalCorpus.EvalDocument::type)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("proposal-a", ReviewType.PROPOSAL),
						org.assertj.core.groups.Tuple.tuple("proposal-b", ReviewType.PROPOSAL),
						org.assertj.core.groups.Tuple.tuple("thesis-c", ReviewType.THESIS));
	}

	@Test
	void theDefaultIdIsTheFilenameSoTwoResultsTreesAreWorthDiffing(@TempDir Path root) throws IOException {
		pdf(root.resolve("proposal"), "Federated Learning (v2).pdf");

		assertThat(EvalCorpus.discover(root).documents())
				.singleElement()
				.extracting(EvalCorpus.EvalDocument::id)
				.isEqualTo("proposal-federated-learning-v2");
	}

	@Test
	void filenamesThatSanitizeToTheSameIdStillGetTheirOwnDirectory(@TempDir Path root) throws IOException {
		pdf(root.resolve("proposal"), "my proposal.pdf");
		pdf(root.resolve("proposal"), "my_proposal.pdf");

		assertThat(EvalCorpus.discover(root).documents())
				.extracting(EvalCorpus.EvalDocument::id)
				.containsExactly("proposal-my-proposal", "proposal-my-proposal-2");
	}

	@Test
	void idsFollowSortedFilenameOrderSoARerunProducesTheSameIds(@TempDir Path root) throws IOException {
		pdf(root.resolve("proposal"), "zeta.pdf");
		pdf(root.resolve("proposal"), "alpha.pdf");

		assertThat(EvalCorpus.discover(root).documents())
				.extracting(document -> document.file().getFileName().toString())
				.containsExactly("alpha.pdf", "zeta.pdf");
	}

	@Test
	void aTitleLengthFilenameIsTruncatedAtAWordBoundary(@TempDir Path root) throws IOException {
		// Thesis PDFs are routinely named after the full title; untruncated, every path in the
		// results tree becomes unreadable.
		pdf(root.resolve("proposal"), "Self-Service Management of Time-Limited Access to Heterogeneous Resources.pdf");

		assertThat(EvalCorpus.discover(root).documents())
				.singleElement()
				.extracting(EvalCorpus.EvalDocument::id)
				.isEqualTo("proposal-self-service-management-of-time-limited-access");
	}

	@Test
	void anonymizingStripsEveryPartOfTheFilenameFromTheId(@TempDir Path root) throws IOException {
		// For a real corpus the results tree is the thing that gets shared, and a student's name
		// must not be in a path.
		pdf(root.resolve("proposal"), "Max_Mustermann_proposal_final.pdf");

		assertThat(EvalCorpus.discover(root, true).documents())
				.singleElement()
				.extracting(EvalCorpus.EvalDocument::id)
				.isEqualTo("proposal-01");
	}

	@Test
	void aFlatDirectoryOfPdfsIsReadAsProposals(@TempDir Path root) throws IOException {
		pdf(root, "one.pdf");

		assertThat(EvalCorpus.discover(root).documents())
				.singleElement()
				.extracting(EvalCorpus.EvalDocument::type)
				.isEqualTo(ReviewType.PROPOSAL);
	}

	@Test
	void anEmptyCorpusSaysWhatTheLayoutShouldLookLike(@TempDir Path root) {
		assertThatThrownBy(() -> EvalCorpus.discover(root))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("proposal/*.pdf");
	}

	@Test
	void aMissingCorpusDirectoryFailsImmediately() {
		assertThatThrownBy(() -> EvalCorpus.discover(Path.of("/nonexistent-corpus-xyz")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("does not exist");
	}
}
