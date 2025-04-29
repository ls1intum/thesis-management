package de.tum.cit.aet.thesis.mock;

import de.tum.cit.aet.thesis.entity.ResearchGroup;
import de.tum.cit.aet.thesis.entity.User;
import de.tum.cit.aet.thesis.security.CurrentUserProvider;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CurrentUserMockUtil {
    public static void mockCurrentUser(ObjectProvider<CurrentUserProvider> currentUserProviderProvider, User user) {
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        when(currentUserProviderProvider.getObject()).thenReturn(currentUserProvider);
        when(currentUserProvider.getUser()).thenReturn(user);
        doNothing().when(currentUserProvider).assertCanAccessResearchGroup(any(ResearchGroup.class));
    }
}