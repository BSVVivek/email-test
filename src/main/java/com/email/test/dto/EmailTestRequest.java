package com.email.test.dto;

public record EmailTestRequest(

        String recipient,

        String subject,

        String message

) {
}