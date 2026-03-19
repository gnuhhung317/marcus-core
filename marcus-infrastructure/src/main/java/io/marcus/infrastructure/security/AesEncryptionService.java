package io.marcus.infrastructure.security;

import io.marcus.domain.service.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class AesEncryptionService implements EncryptionService {
    @Value("${marcus.security.master-key}")
    private String masterKey;

    @Value("${marcus.security.salt}")
    private String salt;

    private TextEncryptor encryptor;

    @PostConstruct
    private void init(){
        this.encryptor = Encryptors.text(masterKey, salt);
    }
    @Override
    public String encrypt(String plainText) {
        if(plainText == null){
            return null;
        }
        return this.encryptor.encrypt(plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null){
            return null;
        }
        try{
            return this.encryptor.decrypt(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(e); //TODO: custom exception
        }
    }
}
