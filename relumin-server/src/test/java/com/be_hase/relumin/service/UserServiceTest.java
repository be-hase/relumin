package com.be_hase.relumin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.be_hase.relumin.model.User;
import com.be_hase.relumin.type.AuthProvider;
import com.be_hase.relumin.type.Role;

import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
    @InjectMocks
    private UserService target;

    @Spy
    private PersistentEntityStore entityStore = PersistentEntityStores.newInstance(
            System.getProperty("user.home") + File.separator + ".testReluminMetaData");

    @Before
    public void before() {
        entityStore.clear();
    }

    @After
    public void after() {
        entityStore.close();
    }

    @Test
    public void crud() {
        // add 1
        final User addUserParam = new User(
                UUID.randomUUID().toString(),
                "displayName",
                "adminadmin",
                AuthProvider.PASSWORD,
                Role.VIEWER);
        final User addUserResult1 = target.add(addUserParam);
        assertThat(addUserResult1.getId()).isNotNull();

        // add 2
        final User addUserResult2 = target.add(
                new User(
                        UUID.randomUUID().toString(),
                        "displayName",
                        "adminadmin",
                        AuthProvider.PASSWORD,
                        Role.VIEWER));

        // add redundant
        assertThatThrownBy(() -> target.add(addUserParam)).isInstanceOf(IllegalArgumentException.class)
                                                          .hasMessage("Same userId exists.");

        // get
        final User getUserResult = target.get(addUserParam.getUserId()).get();
        assertThat(getUserResult.getId()).isEqualTo(addUserResult1.getId());
        assertThat(getUserResult.getUserId()).isEqualTo(addUserParam.getUserId());

        // getAll
        final List<User> allUserResult = target.getAll();
        assertThat(allUserResult.get(0).getUserId()).isEqualTo(addUserResult1.getUserId());
        assertThat(allUserResult.get(1).getUserId()).isEqualTo(addUserResult2.getUserId());

        // loadUserByUsername
        final UserDetails userDetails = target.loadUserByUsername(addUserParam.getUserId());
        assertThat(userDetails.getUsername()).isEqualTo(addUserParam.getUserId());

        // update
        final User updateUserParam = User.builder()
                                         .userId(addUserParam.getUserId())
                                         .displayName("updated")
                                         .role(Role.RELUMIN_ADMIN)
                                         .build();
        final User updateResult = target.update(updateUserParam);
        assertThat(updateResult.getDisplayName()).isEqualTo(updateUserParam.getDisplayName());
        assertThat(updateResult.getRole()).isEqualTo(updateUserParam.getRole());

        // changePassword
        final User changePasswordResult = target.changePassword(addUserParam.getUserId(), "adminadmin",
                                                                "adminadminupdated");
        assertThat(
                new StandardPasswordEncoder()
                        .matches("adminadminupdated", changePasswordResult.getPassword().getValue())).isTrue();

        // changePassword does not match
        assertThatThrownBy(() -> target.changePassword(addUserParam.getUserId(), "invalid", "invalid"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Old password does not match.");

        // changeAuthProvider
        final User changeAPResult = target.changeAuthProvider(addUserParam.getUserId(), AuthProvider.LDAP,
                                                              null);
        assertThat(changeAPResult.getAuthProvider()).isEqualTo(AuthProvider.LDAP);
        assertThat(changeAPResult.getPassword().getValue()).isEmpty();

        // changePassword not password auth
        assertThatThrownBy(
                () -> target.changePassword(addUserParam.getUserId(), "adminadminupdated", "hogehoge"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("This user is not PASSWORD Auth.");

        // delete
        target.delete(addUserParam.getUserId());
        assertThat(target.get(addUserParam.getUserId()).isPresent()).isFalse();
    }
}