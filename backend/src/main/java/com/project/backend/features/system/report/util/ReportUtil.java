package com.project.backend.features.system.report.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collections;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class ReportUtil {

    /**
     * PDFにPKCS#7署名を付与してバイト配列で返す
     */
    public static byte[] signPdfWithPKCS7(byte[] inputPdf, PrivateKey privateKey, Certificate[] chain) throws Exception {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(inputPdf));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName("管理者署名");
            signature.setLocation("Tokyo");
            signature.setReason("給与明細確認");
            signature.setSignDate(Calendar.getInstance());

            doc.addSignature(signature);

            ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(out);

            byte[] cmsSignature = createPKCS7(signature, doc, privateKey, chain);
            externalSigning.setSignature(cmsSignature);

            return out.toByteArray();
        }
    }

    /**
     * BouncyCastleでPKCS#7署名生成
     */
    private static byte[] createPKCS7(PDSignature pdSig, PDDocument doc, PrivateKey privateKey, Certificate[] chain) throws Exception {
        // PDFハッシュ取得
        ByteArrayOutputStream pdfHash = new ByteArrayOutputStream();
        doc.saveIncremental(pdfHash);

        CMSProcessableByteArray msg = new CMSProcessableByteArray(pdfHash.toByteArray());

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(
                new org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder(
                        new org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder().build())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(privateKey),
                                (X509Certificate) chain[0])
        );

        gen.addCertificates(new JcaCertStore(Collections.singletonList(chain[0])));

        CMSSignedData signedData = gen.generate(msg, false);
        return signedData.getEncoded();
    }
}