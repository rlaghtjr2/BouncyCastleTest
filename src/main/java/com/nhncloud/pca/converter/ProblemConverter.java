package com.nhncloud.pca.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhncloud.pca.model.acme.Problem;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProblemConverter implements AttributeConverter<Problem, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Problem problem) {
        try {
            if (problem == null) {
                return null;
            }
            return objectMapper.writeValueAsString(problem);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting Problem to JSON", e);
        }
    }

    @Override
    public Problem convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(json, Problem.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JSON to Problem", e);
        }
    }
}