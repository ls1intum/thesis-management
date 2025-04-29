package de.tum.cit.aet.thesis.mock;

import de.tum.cit.aet.thesis.constants.ApplicationState;
import de.tum.cit.aet.thesis.constants.ThesisRoleName;
import de.tum.cit.aet.thesis.constants.ThesisState;
import de.tum.cit.aet.thesis.entity.*;
import de.tum.cit.aet.thesis.entity.key.ThesisRoleId;
import de.tum.cit.aet.thesis.entity.key.UserGroupId;

import java.util.*;

public class EntityMockFactory {
    public static User createUser(String name) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(name);
        user.setLastName(name);
        user.setEmail(name.toLowerCase() + "@example.com");

        return user;
    }

    public static User createUserWithGroup(String name, String... groups) {
        User user = createUser(name);

        setupUserGroups(user, groups);

        return user;
    }

    public static void setupUserGroups(User user, String... groups) {
        Set<UserGroup> userGroups = new HashSet<>();
        for (String group : groups) {
            UserGroup entity = new UserGroup();
            UserGroupId entityId = new UserGroupId();

            entityId.setUserId(user.getId());
            entityId.setGroup(group);
            entity.setId(entityId);
            entity.setUser(user);

            userGroups.add(entity);
        }

        user.setGroups(userGroups);
    }

    public static ResearchGroup createResearchGroup(String name) {
        User user = createUserWithGroup(name, "supervisor");

        ResearchGroup researchGroup = new ResearchGroup();
        researchGroup.setId(UUID.randomUUID());
        researchGroup.setHead(user);
        researchGroup.setName(name);
        researchGroup.setArchived(false);
        researchGroup.setCreatedAt(java.time.Instant.now());
        researchGroup.setUpdatedAt(java.time.Instant.now());
        researchGroup.setCreatedBy(user);
        researchGroup.setUpdatedBy(user);

        user.setResearchGroup(researchGroup);
        return researchGroup;
    }

    public static Topic createTopic(String title, ResearchGroup researchGroup) {
        Topic topic = new Topic();

        topic.setId(UUID.randomUUID());
        topic.setTitle(title);
        topic.setRoles(new ArrayList<>());
        topic.setResearchGroup(researchGroup);

        return topic;
    }

    public static Application createApplication(ResearchGroup researchGroup) {
        Application application = new Application();

        application.setId(UUID.randomUUID());
        application.setUser(createUser("user"));
        application.setTopic(createTopic("title", researchGroup));
        application.setState(ApplicationState.NOT_ASSESSED);
        application.setResearchGroup(researchGroup);

        return application;
    }

    public static Thesis createThesis(String title, ResearchGroup researchGroup) {
        Thesis thesis = new Thesis();

        thesis.setId(UUID.randomUUID());
        thesis.setTitle(title);
        thesis.setState(ThesisState.PROPOSAL);
        thesis.setResearchGroup(researchGroup);

        return thesis;
    }

    public static void setupThesisRole(Thesis thesis, User user, ThesisRoleName roleName) {
        ThesisRole role = new ThesisRole();
        ThesisRoleId roleId = new ThesisRoleId();

        role.setThesis(thesis);
        role.setUser(user);
        roleId.setRole(roleName);
        role.setId(roleId);

        thesis.setRoles(List.of(role));
    }
}
