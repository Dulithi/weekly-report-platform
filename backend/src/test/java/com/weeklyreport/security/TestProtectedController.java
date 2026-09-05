package com.weeklyreport.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestProtectedController {

    @GetMapping("/api/v1/manager/security-test")
    String manager() {
        return "manager";
    }

    @GetMapping("/api/v1/admin/security-test")
    String admin() {
        return "admin";
    }

    @GetMapping("/api/v1/security-test")
    String authenticated() {
        return "authenticated";
    }
}