package com.nhncloud.pca.converter;

import com.nhncloud.pca.constant.acme.AccountStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return AccountStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid AccountStatus: " + dbData, e);
        }
    }
}