package com.weeklyreport.user.exception;

public class InvalidInvitationException extends RuntimeException {

    public InvalidInvitationException() {
        super("Invalid or expired invitation");
    }
}
