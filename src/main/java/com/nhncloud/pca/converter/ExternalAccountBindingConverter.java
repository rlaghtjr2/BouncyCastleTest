package com.nhncloud.pca.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.model.acme.ExternalAccountBinding;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ExternalAccountBindingConverter implements AttributeConverter<ExternalAccountBinding, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ExternalAccountBinding binding) {
        try {
            if (binding == null) {
                return null;
            }
            return objectMapper.writeValueAsString(binding);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting EAB to JSON", e);
        }
    }

    @Override
    public ExternalAccountBinding convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, ExternalAccountBinding.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to EAB", e);
        }
    }
}