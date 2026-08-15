package com.strava.userservice.admin;

import com.strava.userservice.domain.Role;
import jakarta.validation.constraints.NotNull;

/** Role change payload, mirrored from the monolith's {@code UpdateUserRoleRequest}. */
public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required") Role role
) {}
