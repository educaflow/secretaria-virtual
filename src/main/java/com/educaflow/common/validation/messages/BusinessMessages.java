package com.educaflow.common.validation.messages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BusinessMessages extends ArrayList<BusinessMessage> {

    public boolean isValid() {
        return this.isEmpty();
    }


    public BusinessMessages removeDuplicates() {
        BusinessMessages uniqueMessages = new BusinessMessages();
        Set<BusinessMessage> seenMessages = new HashSet<>();

        for (BusinessMessage message : this) {
            if (seenMessages.add(message)) {
                uniqueMessages.add(message);
            }
        }
        return uniqueMessages;
    }


    static public BusinessMessages single(String message) {
        BusinessMessage businessMessage = new BusinessMessage(null, message, null);
        BusinessMessages businessMessages = new BusinessMessages();
        businessMessages.add(businessMessage);
        return businessMessages;
    }

}
