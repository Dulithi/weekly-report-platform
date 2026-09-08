package com.weeklyreport.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.user.dto.ManagerAssignmentResponse;
import com.weeklyreport.user.service.UserManagementService;

@RestController
@RequestMapping("/api/v1/admin/manager-assignments")
public class ManagerAssignmentAdminController {

    private final UserManagementService userManagementService;

    public ManagerAssignmentAdminController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<ManagerAssignmentResponse> list() {
        return userManagementService.listManagerAssignments();
    }
}
