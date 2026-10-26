package de.tum.cit.aet.thesis.core.user.mailvariables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.tum.cit.aet.thesis.core.dto.MailVariableDto;
import de.tum.cit.aet.thesis.core.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

class MailUserTest {

	@Test
	void fromUser_null_returnsEmpty() {
		MailUser result = MailUser.fromUser(null);
		assertEquals(new MailUser("", ""), result);
	}

	@Test
	void fromUser_validUser_mapsNames() {
		User user = new User();
		user.setFirstName("Max");
		user.setLastName("Mustermann");

		MailUser result = MailUser.fromUser(user);
		assertEquals("Max", result.firstName());
		assertEquals("Mustermann", result.lastName());
	}

	@Test
	void fromUser_userWithNullFields_returnsEmptyStrings() {
		User user = new User();
		MailUser result = MailUser.fromUser(user);
		assertEquals("", result.firstName());
		assertEquals("", result.lastName());
	}

	@Test
	void templateVariables_assemblesPrefixedKeys() {
		List<MailVariableDto> vars = MailUser.templateVariables("recipient", "Recipient", "User");
		assertNotNull(vars);
		assertEquals(2, vars.size());
		assertEquals("Recipient First Name", vars.get(0).label());
		assertEquals("[[${recipient.firstName}]]", vars.get(0).templateVariable());
		assertEquals("Recipient Last Name", vars.get(1).label());
		assertEquals("[[${recipient.lastName}]]", vars.get(1).templateVariable());
		assertEquals("User", vars.get(0).group());
	}
}
