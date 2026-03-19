package io.marcus.domain.service;

public interface EncryptionService {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
