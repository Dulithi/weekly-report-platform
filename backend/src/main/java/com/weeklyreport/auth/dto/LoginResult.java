package com.weeklyreport.auth.dto;

import com.weeklyreport.auth.service.IssuedRefreshToken;

public record LoginResult(
        AccessTokenResponse accessToken,
        IssuedRefreshToken refreshToken
) {
}
