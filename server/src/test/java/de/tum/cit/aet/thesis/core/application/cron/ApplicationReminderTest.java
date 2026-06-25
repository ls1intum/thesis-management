package de.tum.cit.aet.thesis.core.application.cron;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.tum.cit.aet.thesis.core.admin.service.MailingService;
import de.tum.cit.aet.thesis.core.application.repository.ApplicationRepository;
import de.tum.cit.aet.thesis.core.group.entity.ResearchGroup;
import de.tum.cit.aet.thesis.core.group.service.ResearchGroupService;
import de.tum.cit.aet.thesis.core.user.entity.User;
import de.tum.cit.aet.thesis.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ApplicationReminderTest {

	@Mock
	private ApplicationRepository applicationRepository;

	@Mock
	private MailingService mailingService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ResearchGroupService researchGroupService;

	@InjectMocks
	private ApplicationReminder applicationReminder;

	private ResearchGroup researchGroup;
	private User user;
	private UUID rgId;

	@BeforeEach
	void setUp() {
		rgId = UUID.randomUUID();
		researchGroup = new ResearchGroup();
		researchGroup.setId(rgId);
		user = new User();
		user.setId(UUID.randomUUID());
		user.setResearchGroup(researchGroup);
	}

	@Test
	void emailReminder_sendsToUsersWithUnreviewedApplications() {
		Page<ResearchGroup> rgPage = new PageImpl<>(List.of(researchGroup));
		when(researchGroupService.getAll(any(), any(), anyBoolean(), any(), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(rgPage);
		when(userRepository.getRoleMembers(eq(Set.of("admin", "supervisor", "advisor")), eq(rgId)))
				.thenReturn(List.of(user));
		when(applicationRepository.countUnreviewedApplications(eq(user.getId()), eq(rgId))).thenReturn(5L);

		applicationReminder.emailReminder();

		verify(mailingService, times(1)).sendApplicationReminderEmail(user, 5L);
	}

	@Test
	void emailReminder_skipsUsersWithoutUnreviewedApplications() {
		Page<ResearchGroup> rgPage = new PageImpl<>(List.of(researchGroup));
		when(researchGroupService.getAll(any(), any(), anyBoolean(), any(), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(rgPage);
		when(userRepository.getRoleMembers(any(), eq(rgId))).thenReturn(List.of(user));
		when(applicationRepository.countUnreviewedApplications(any(), any())).thenReturn(0L);

		applicationReminder.emailReminder();

		verify(mailingService, never()).sendApplicationReminderEmail(any(), anyLong());
	}

	@Test
	void emailReminder_noResearchGroups_sendsNothing() {
		when(researchGroupService.getAll(any(), any(), anyBoolean(), any(), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(new PageImpl<>(List.of()));

		applicationReminder.emailReminder();

		verify(mailingService, never()).sendApplicationReminderEmail(any(), anyLong());
	}

	private static long anyLong() {
		return org.mockito.ArgumentMatchers.anyLong();
	}
}
