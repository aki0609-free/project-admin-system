package com.project.backend.features.system.report.util;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class KeyLoader {

    public static PrivateKey getPrivateKey(String p12Path, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = KeyLoader.class.getResourceAsStream(p12Path)) {
            ks.load(in, password.toCharArray());
        }
        return (PrivateKey) ks.getKey(alias, password.toCharArray());
    }

    public static Certificate[] getCertificateChain(String p12Path, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = KeyLoader.class.getResourceAsStream(p12Path)) {
            ks.load(in, password.toCharArray());
        }
        return ks.getCertificateChain(alias);
    }
}
