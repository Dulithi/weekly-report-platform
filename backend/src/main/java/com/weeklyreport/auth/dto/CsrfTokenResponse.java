package com.weeklyreport.auth.dto;

public record CsrfTokenResponse(String headerName, String token) {
}
