package com.nhncloud.pca.converter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ContactInfoConverter implements AttributeConverter<List<String>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> contactList) {
        try {
            if (contactList == null || contactList.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(contactList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting contact info to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to contact info", e);
        }
    }
}