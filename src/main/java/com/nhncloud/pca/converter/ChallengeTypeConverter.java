package com.nhncloud.pca.converter;

import com.nhncloud.pca.constant.acme.ChallengeType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChallengeTypeConverter implements AttributeConverter<ChallengeType, String> {

    @Override
    public String convertToDatabaseColumn(ChallengeType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }

    @Override
    public ChallengeType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return ChallengeType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid ChallengeType: " + dbData, e);
        }
    }
}