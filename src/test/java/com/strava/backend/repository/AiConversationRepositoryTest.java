package com.strava.backend.repository;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.AiConversation;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AiConversationRepository Integration Tests")
class AiConversationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AiConversationRepository conversationRepository;

    private User user;
    private Activity activity;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("athlete@example.com")
                .firstName("Ada")
                .lastName("Runner")
                .password("password123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build();
        entityManager.persist(user);

        activity = Activity.builder()
                .title("Morning Run")
                .activityType(ActivityType.RUN)
                .distance(10.0)
                .duration(3000)
                .startTime(LocalDateTime.now().minusDays(1))
                .user(user)
                .build();
        entityManager.persist(activity);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByUserIdAndActivityId returns the conversation with its ordered messages")
    void findByUserIdAndActivityIdReturnsMessages() {
        AiConversation conversation = AiConversation.builder().user(user).activity(activity).build();
        conversation.addMessage(ChatRole.USER, "Analyze this activity");
        conversation.addMessage(ChatRole.ASSISTANT, "Solid aerobic effort at 5:00/km.");
        conversationRepository.saveAndFlush(conversation);
        entityManager.clear();

        Optional<AiConversation> found =
                conversationRepository.findByUserIdAndActivityId(user.getId(), activity.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMessages()).hasSize(2);
        assertThat(found.get().getMessages().get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(found.get().getMessages().get(0).getContent()).isEqualTo("Analyze this activity");
        assertThat(found.get().getMessages().get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
    }

    @Test
    @DisplayName("returns empty when no conversation exists for the (user, activity) pair")
    void returnsEmptyWhenMissing() {
        assertThat(conversationRepository.findByUserIdAndActivityId(user.getId(), activity.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("enforces a single conversation per (user, activity)")
    void enforcesUniqueConversationPerUserActivity() {
        conversationRepository.saveAndFlush(
                AiConversation.builder().user(user).activity(activity).build());

        assertThatThrownBy(() -> conversationRepository.saveAndFlush(
                AiConversation.builder().user(user).activity(activity).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
