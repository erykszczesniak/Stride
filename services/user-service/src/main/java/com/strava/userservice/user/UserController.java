package com.strava.userservice.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService users;

    @GetMapping("/me")
    public UserProfileDto me(@AuthenticationPrincipal Long userId) {
        return users.getById(userId);
    }

    @PatchMapping("/me")
    public UserProfileDto updateMe(@AuthenticationPrincipal Long userId,
                                   @Valid @RequestBody UpdateProfileRequest req) {
        return users.updateProfile(userId, req);
    }

    @GetMapping("/{id}")
    public UserProfileDto getById(@PathVariable Long id) {
        return users.getById(id);
    }

    @GetMapping("/search")
    public Page<UserProfileDto> search(@RequestParam("q") String q, Pageable pageable) {
        return users.search(q, pageable);
    }
}
