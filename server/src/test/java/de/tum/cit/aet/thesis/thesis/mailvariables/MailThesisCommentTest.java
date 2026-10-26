package de.tum.cit.aet.thesis.thesis.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.thesis.entity.ThesisComment;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailThesisCommentTest {

	@Test
	void fromComment_null_returnsEmpty() {
		MailThesisComment result = MailThesisComment.fromComment(null);
		assertEquals(new MailThesisComment("", "", ""), result);
	}

	@Test
	void fromComment_withCreator_mapsAllFields() {
		User creator = new User();
		creator.setFirstName("Max");
		creator.setLastName("Mustermann");
		ThesisComment comment = new ThesisComment();
		comment.setCreatedBy(creator);
		comment.setMessage("Hi");

		MailThesisComment result = MailThesisComment.fromComment(comment);
		assertEquals("Max", result.creatorFirstName());
		assertEquals("Mustermann", result.creatorLastName());
		assertEquals("Hi", result.message());
	}

	@Test
	void fromComment_withoutCreator_returnsEmptyNames() {
		ThesisComment comment = new ThesisComment();
		comment.setMessage("hello");
		MailThesisComment result = MailThesisComment.fromComment(comment);
		assertEquals("", result.creatorFirstName());
		assertEquals("", result.creatorLastName());
		assertEquals("hello", result.message());
	}

	@Test
	void templateVariables_threeEntriesInThesisCommentGroup() {
		List<MailVariableDto> vars = MailThesisComment.templateVariables();
		assertEquals(3, vars.size());
		for (MailVariableDto v : vars) {
			assertEquals("Thesis Comment", v.group());
		}
	}
}
