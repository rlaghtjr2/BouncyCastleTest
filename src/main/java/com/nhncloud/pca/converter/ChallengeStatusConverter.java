package com.nhncloud.pca.converter;

import com.nhncloud.pca.constant.acme.ChallengeStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChallengeStatusConverter implements AttributeConverter<ChallengeStatus, String> {

    @Override
    public String convertToDatabaseColumn(ChallengeStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public ChallengeStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return ChallengeStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid ChallengeStatus: " + dbData, e);
        }
    }
}