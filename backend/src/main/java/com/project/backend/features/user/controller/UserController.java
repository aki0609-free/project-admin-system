package com.project.backend.features.user.controller;

import com.project.backend.features.user.service.UserCommandService;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.user.dto.UserCreateRequest;
import com.project.backend.features.user.dto.UserDetailResponse;
import com.project.backend.features.user.dto.UserListItemResponse;
import com.project.backend.features.user.dto.UserUpdateRequest;
import com.project.backend.features.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @GetMapping
    public List<UserListItemResponse> findAll() {
        return userQueryService.findAll();
    }

    @GetMapping("/{id}")
    public UserDetailResponse findDetail(@PathVariable Long id) {
        return userQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(@RequestBody UserCreateRequest request) {

        return userCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
        @PathVariable Long id,
        @RequestBody UserUpdateRequest request
    ) {
        userCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userCommandService.delete(id);
    }
}