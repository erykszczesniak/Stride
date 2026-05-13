package com.strava.clubevent.club;

import com.strava.clubevent.domain.ClubMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubs;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClubDto create(@AuthenticationPrincipal Long userId,
                          @Valid @RequestBody CreateClubRequest req) {
        return clubs.create(userId, req);
    }

    @GetMapping
    public Page<ClubDto> search(@RequestParam(value = "q", required = false) String q, Pageable pageable) {
        return clubs.search(q, pageable);
    }

    @GetMapping("/{id}")
    public ClubDto get(@PathVariable Long id) {
        return clubs.get(id);
    }

    @PatchMapping("/{id}")
    public ClubDto update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                          @Valid @RequestBody UpdateClubRequest req) {
        return clubs.update(id, userId, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        clubs.delete(id, userId);
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        clubs.join(id, userId);
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        clubs.leave(id, userId);
    }

    @GetMapping("/{id}/members")
    public Page<ClubMember> members(@PathVariable Long id, Pageable pageable) {
        return clubs.listMembers(id, pageable);
    }

    @GetMapping("/me/memberships")
    public List<ClubMember> myClubs(@AuthenticationPrincipal Long userId) {
        return clubs.myClubs(userId);
    }
}
