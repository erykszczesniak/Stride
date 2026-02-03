package com.strava.backend.util;

import com.strava.backend.domain.*;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.goal.GoalRequest;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.enums.*;

import java.time.LocalDateTime;
import java.util.HashSet;

/**
 * Factory class for creating test data objects.
 */
public class TestDataFactory {

    public static User createUser() {
        return createUser(1L, "john@example.com", "John", "Doe");
    }

    public static User createUser(Long id, String email, String firstName, String lastName) {
        User user = User.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password("encodedPassword123")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .bio("Test bio")
                .city("Warsaw")
                .country("Poland")
                .weight(70.5)
                .height(175.0)
                .followers(new HashSet<>())
                .following(new HashSet<>())
                .activities(new HashSet<>())
                .clubs(new HashSet<>())
                .challenges(new HashSet<>())
                .events(new HashSet<>())
                .goals(new HashSet<>())
                .build();
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        return user;
    }

    public static Activity createActivity() {
        return createActivity(1L, createUser());
    }

    public static Activity createActivity(Long id, User user) {
        Activity activity = Activity.builder()
                .id(id)
                .title("Morning Run")
                .description("A nice morning run in the park")
                .activityType(ActivityType.RUN)
                .distance(10.5)
                .duration(3600)
                .elevationGain(150)
                .calories(500)
                .averageSpeed(10.5)
                .maxSpeed(15.0)
                .averageHeartRate(145)
                .maxHeartRate(175)
                .averageCadence(85)
                .averagePower(200)
                .startTime(LocalDateTime.now().minusHours(2))
                .startLocation("Central Park")
                .endLocation("Central Park")
                .isPrivate(false)
                .user(user)
                .likedBy(new HashSet<>())
                .comments(new HashSet<>())
                .build();
        activity.setCreatedAt(LocalDateTime.now());
        return activity;
    }

    public static ActivityRequest createActivityRequest() {
        ActivityRequest request = new ActivityRequest();
        request.setTitle("Evening Ride");
        request.setDescription("A cycling session");
        request.setActivityType(ActivityType.RIDE);
        request.setDistance(25.0);
        request.setDuration(5400);
        request.setElevationGain(300);
        request.setCalories(800);
        request.setAverageSpeed(16.7);
        request.setMaxSpeed(35.0);
        request.setAverageHeartRate(140);
        request.setMaxHeartRate(170);
        request.setAverageCadence(80);
        request.setAveragePower(180);
        request.setStartTime(LocalDateTime.now());
        request.setStartLocation("Home");
        request.setEndLocation("Mountain");
        request.setPrivate(false);
        return request;
    }

    public static Goal createGoal() {
        return createGoal(1L, createUser());
    }

    public static Goal createGoal(Long id, User user) {
        Goal goal = Goal.builder()
                .id(id)
                .goalType(GoalType.DISTANCE)
                .period(GoalPeriod.WEEKLY)
                .activityType(ActivityType.RUN)
                .targetValue(50.0)
                .currentValue(25.0)
                .startDate(LocalDateTime.now().minusDays(3))
                .endDate(LocalDateTime.now().plusDays(4))
                .isActive(true)
                .user(user)
                .build();
        goal.setCreatedAt(LocalDateTime.now().minusDays(3));
        return goal;
    }

    public static GoalRequest createGoalRequest() {
        GoalRequest request = new GoalRequest();
        request.setGoalType(GoalType.DISTANCE);
        request.setPeriod(GoalPeriod.WEEKLY);
        request.setActivityType(ActivityType.RUN);
        request.setTargetValue(100.0);
        return request;
    }

    public static UpdateUserRequest createUpdateUserRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setBio("Updated bio");
        request.setCity("Krakow");
        request.setCountry("Poland");
        request.setWeight(65.0);
        request.setHeight(170.0);
        return request;
    }

    public static Club createClub() {
        return createClub(1L, createUser());
    }

    public static Club createClub(Long id, User owner) {
        return Club.builder()
                .id(id)
                .name("Running Club")
                .description("A club for runners")
                .city("Warsaw")
                .country("Poland")
                .sportType(ActivityType.RUN)
                .isPrivate(false)
                .owner(owner)
                .members(new HashSet<>())
                .admins(new HashSet<>())
                .build();
    }

    public static Challenge createChallenge() {
        return createChallenge(1L, createUser());
    }

    public static Challenge createChallenge(Long id, User creator) {
        return Challenge.builder()
                .id(id)
                .name("Monthly Running Challenge")
                .description("Run 100km this month")
                .challengeType(ChallengeType.DISTANCE)
                .activityType(ActivityType.RUN)
                .targetValue(100.0)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                .isPublic(true)
                .creator(creator)
                .participants(new HashSet<>())
                .build();
    }

    public static Event createEvent() {
        return createEvent(1L, createUser());
    }

    public static Event createEvent(Long id, User organizer) {
        return Event.builder()
                .id(id)
                .name("Marathon 2024")
                .description("Annual city marathon")
                .activityType(ActivityType.RUN)
                .location("City Center")
                .city("Warsaw")
                .country("Poland")
                .eventDate(LocalDateTime.now().plusDays(30))
                .maxParticipants(1000)
                .registrationDeadline(LocalDateTime.now().plusDays(20))
                .isPublic(true)
                .organizer(organizer)
                .registeredUsers(new HashSet<>())
                .build();
    }

    public static Comment createComment() {
        return createComment(1L, createUser(), createActivity());
    }

    public static Comment createComment(Long id, User user, Activity activity) {
        Comment comment = Comment.builder()
                .id(id)
                .content("Great activity!")
                .user(user)
                .activity(activity)
                .build();
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    public static CommentReaction createCommentReaction(Long id, User user, Comment comment, String emoji) {
        CommentReaction reaction = CommentReaction.builder()
                .id(id)
                .emoji(emoji)
                .user(user)
                .comment(comment)
                .build();
        reaction.setCreatedAt(LocalDateTime.now());
        return reaction;
    }

    public static com.strava.backend.dto.event.EventRequest createEventRequest() {
        var request = new com.strava.backend.dto.event.EventRequest();
        request.setName("Marathon 2024");
        request.setDescription("Annual city marathon");
        request.setActivityType(ActivityType.RUN);
        request.setEventDate(LocalDateTime.now().plusDays(30));
        request.setRegistrationDeadline(LocalDateTime.now().plusDays(20));
        request.setLocation("City Center");
        request.setCity("Warsaw");
        request.setCountry("Poland");
        request.setDistance(42.195);
        request.setMaxParticipants(1000);
        request.setPublic(true);
        return request;
    }
}
