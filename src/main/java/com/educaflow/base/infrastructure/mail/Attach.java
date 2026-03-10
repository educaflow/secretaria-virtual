package com.educaflow.base.infrastructure.mail;

public record Attach(String fileName, byte[] data, String mimeType) {
}
