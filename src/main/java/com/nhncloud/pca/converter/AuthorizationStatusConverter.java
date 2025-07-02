package com.nhncloud.pca.converter;

import com.nhncloud.pca.constant.acme.AuthorizationStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuthorizationStatusConverter implements AttributeConverter<AuthorizationStatus, String> {

    @Override
    public String convertToDatabaseColumn(AuthorizationStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public AuthorizationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return AuthorizationStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid AuthorizationStatus: " + dbData, e);
        }
    }
}