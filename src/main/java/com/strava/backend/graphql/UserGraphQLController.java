package com.strava.backend.graphql;

import com.strava.backend.domain.User;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.controller.BaseController;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserGraphQLController extends BaseController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public List<User> users(@Argument Integer page, @Argument Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        return userService.getAllUsers(PageRequest.of(pageNum, pageSize)).getContent();
    }

    @QueryMapping
    public User currentUser() {
        return userService.getUserById(getAuthenticatedUserId());
    }

    @QueryMapping
    public List<User> suggestedUsers(@Argument Integer limit) {
        int maxResults = limit != null ? limit : 5;
        return userService.getSuggestedUsers(getAuthenticatedUserId(), maxResults);
    }

    @MutationMapping
    public User updateProfile(@Argument("input") UpdateUserRequest input) {
        return userService.updateUser(getAuthenticatedUserId(), input);
    }

    @MutationMapping
    public Boolean followUser(@Argument Long userId) {
        userService.followUser(getAuthenticatedUserId(), userId);
        return true;
    }

    @MutationMapping
    public Boolean unfollowUser(@Argument Long userId) {
        userService.unfollowUser(getAuthenticatedUserId(), userId);
        return true;
    }

    // Schema mappings for computed fields
    @SchemaMapping(typeName = "User", field = "followersCount")
    public int followersCount(User user) {
        return userRepository.countFollowers(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "followingCount")
    public int followingCount(User user) {
        return userRepository.countFollowing(user.getId());
    }

    @SchemaMapping(typeName = "User", field = "activitiesCount")
    public int activitiesCount(User user) {
        return activityService.countActivitiesByUserId(user.getId());
    }
}
