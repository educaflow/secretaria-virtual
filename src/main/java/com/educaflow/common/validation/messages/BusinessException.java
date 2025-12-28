package com.educaflow.common.validation.messages;

public class BusinessException extends Exception {

    private BusinessMessages businessMessages=new BusinessMessages();

    public BusinessException(BusinessMessages businessMessages) {
        if (businessMessages == null) {
            throw new IllegalArgumentException("BusinessMessages no puede ser null");
        }
        if (businessMessages.isEmpty()) {
            throw new IllegalArgumentException("BusinessMessages no puede estar vacío");
        }
        this.businessMessages = businessMessages;
    }

    public BusinessException(BusinessMessage businessMessage) {
        this.businessMessages.add(businessMessage);
    }

    public BusinessException(String fieldName, String message, String label) {
        this.businessMessages.add(new BusinessMessage(fieldName, message, label));
    }

    public BusinessMessages getBusinessMessages() {
        return businessMessages;
    }

}
