package com.devjubis.tickets.shared.domain.vo;

import java.util.regex.Pattern;

import com.devjubis.tickets.shared.domain.BusinessRuleViolation;

public record PasswordHash(String value) {

    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolation("O hash da senha é obrigatório");
        }
        if (!BCRYPT_PATTERN.matcher(value).matches()) {
            throw new BusinessRuleViolation("O hash da senha não está no formato BCrypt");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[protegido]";
    }
}
