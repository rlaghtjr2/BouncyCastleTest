package com.nhncloud.pca.converter;

import com.nhncloud.pca.constant.acme.OrderStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid OrderStatus: " + dbData, e);
        }
    }
}