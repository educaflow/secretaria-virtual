package com.educaflow.common.validation.messages;

import java.util.Objects;

public class BusinessMessage {

    private final String fieldName;
    private final String message;
    private final String label;


    public BusinessMessage(String fieldName, String message, String label) {
        this.fieldName = fieldName;
        this.message = message;
        this.label = label;
    }
    public String getMessage() {
        return message;
    }
    public String getFieldName() {
        return fieldName;
    }
    public String getLabel() { return label; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessMessage that = (BusinessMessage) o;
        return Objects.equals(fieldName, that.fieldName) &&
                Objects.equals(message, that.message) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, message, label);
    }
}
