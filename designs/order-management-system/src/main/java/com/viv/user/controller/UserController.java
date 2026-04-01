package com.viv.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viv.user.entity.User;
import com.viv.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "Operations backed by the MySQL user datasource")
public class UserController {

    private final UserRepository repo;


    @PostMapping
    @Operation(
        summary = "Create a user",
        description = "Creates a new user record in the MySQL datasource."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = User.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid user payload")
    })
    public User create(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User payload stored in the MySQL datasource",
            required = true,
            content = @Content(schema = @Schema(implementation = User.class))
        )
        @RequestBody User user
    ) {
        return repo.save(user);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a user by id",
        description = "Fetches a user from the MySQL datasource. Hibernate second-level cache can serve repeated reads."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = User.class))
        ),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public User get(
        @Parameter(description = "User identifier", example = "1", required = true)
        @PathVariable Long id
    ) {
        return repo.findById(id).orElseThrow();
    }
}
