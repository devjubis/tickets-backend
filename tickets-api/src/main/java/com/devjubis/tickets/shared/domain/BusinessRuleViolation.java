package com.devjubis.tickets.shared.domain;

public class BusinessRuleViolation extends DomainException {

    public BusinessRuleViolation(String message) {
        super(message);
    }
}
