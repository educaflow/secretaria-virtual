package com.educaflow.base.infrastructure.mail;

import java.util.List;

public record Mail(List<String> to, String from, String subject, String htmlBody, String textBody, List<Attach> attachs) {

}
