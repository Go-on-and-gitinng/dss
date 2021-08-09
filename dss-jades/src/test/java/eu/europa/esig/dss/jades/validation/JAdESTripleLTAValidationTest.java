package eu.europa.esig.dss.jades.validation;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.FoundRevocationsProxy;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.enumerations.RevocationOrigin;
import eu.europa.esig.dss.enumerations.RevocationType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.validationreport.jaxb.AttributeBaseType;
import eu.europa.esig.validationreport.jaxb.SignatureAttributesType;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JAdESTripleLTAValidationTest extends AbstractJAdESTestValidation {

    @Override
    protected DSSDocument getSignedDocument() {
        return new FileDocument("src/test/resources/validation/jades-triple-LTA.json");
    }

    @Override
    protected CertificateSource getTrustedCertificateSource() {
        CommonTrustedCertificateSource trustedCertificateSource = new CommonTrustedCertificateSource();
        trustedCertificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString("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"));
        trustedCertificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString("MIIHQTCCBSugAwIBAgIQIUXI2bEFUA5MvqVCVTrywzALBgkqhkiG9w0BAQswODELMAkGA1UEBhMCRVMxFDASBgNVBAoMC0laRU5QRSBTLkEuMRMwEQYDVQQDDApJemVucGUuY29tMB4XDTEwMTAyMDA4MTYwMloXDTM3MTIxMjIzMDAwMFowgacxCzAJBgNVBAYTAkVTMRQwEgYDVQQKDAtJWkVOUEUgUy5BLjE6MDgGA1UECwwxTlpaIFppdXJ0YWdpcmkgcHVibGlrb2EgLSBDZXJ0aWZpY2FkbyBwdWJsaWNvIFNDSTFGMEQGA1UEAww9SGVycml0YXIgZXRhIEVyYWt1bmRlZW4gQ0EgLSBDQSBkZSBDaXVkYWRhbm9zIHkgRW50aWRhZGVzICg0KTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAN+nfomB85viwoOjLpT8shBiWUJWJhRoGzHd6yz/6O0ZK9bHrbRdScXLclUA/TnId2tK8D2WX4LacNQxFap+ZNlXVyORDE6lqDY/3WQg6I2yPGjeCtnJUo3gGUqe6JLgHqDPTemdbtzu3ICYfgYQ43SFM/NNQRbeBuJn68rkITxsj/x60lPsFwCYLDg+TqjBZSdVTOvjO1rO8+JnVsdnjW9rrpSiubY1Dber/dnWntCg/CWSZg8BXhdAW8N/etZ8suOTwnKbOTBNOIu2lWDr7JsqtQxViDkBlZCIRnfffn8oQzNLPpOUNbQM1HiwnoEuT8i9xK1XNoJV/W9FKl7xSNJXyrOPgstGd6swXcnsMCufnncXUlBcLP/5XNeFX1szYl9bXPOjhFLhRAQUzXjGY72wCMUV8p63g5z0pTHpNCVmJFDR6ionKEtU0zvqAfZndLuoA+wzaUJGss0n08It4B7E02EVjl/j4/9llooFu/rGIkjk200cZq8V3vFsUbEivbs/s4quEXMBePOFkOS0mcKM8b+dcinF/2B5/38Xfz2Zry2w91HzK5F7CovDeVQPr3mOiVH3tfzd+QMZg8qw3pVHub47BjqgujPy74C6BiCEaijtr9vgvsePaC3sG1ZyZK0ISBbWZkvglvXcyQoCxNAT2GuBU4rGyM6eXDJXDo85AgMBAAGjggHZMIIB1TCBxwYDVR0RBIG/MIG8hhVodHRwOi8vd3d3Lml6ZW5wZS5jb22BD2luZm9AaXplbnBlLmNvbaSBkTCBjjFHMEUGA1UECgw+SVpFTlBFIFMuQS4gLSBDSUYgQTAxMzM3MjYwLVJNZXJjLlZpdG9yaWEtR2FzdGVpeiBUMTA1NSBGNjIgUzgxQzBBBgNVBAkMOkF2ZGEgZGVsIE1lZGl0ZXJyYW5lbyBFdG9yYmlkZWEgMTQgLSAwMTAxMCBWaXRvcmlhLUdhc3RlaXowDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFKQXHU5l1++HlS5/jrh1ywWL04x9MB8GA1UdIwQYMBaAFB0cZQ6o8iV7tJHP5LGx5r1VdGwFMDoGA1UdIAQzMDEwLwYEVR0gADAnMCUGCCsGAQUFBwIBFhlodHRwOi8vd3d3Lml6ZW5wZS5jb20vY3BzMDcGCCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL29jc3AuaXplbnBlLmNvbTo4MDk0MDMGA1UdHwQsMCowKKAmoCSGImh0dHA6Ly9jcmwuaXplbnBlLmNvbS9jZ2ktYmluL2FybDIwCwYJKoZIhvcNAQELA4ICAQA3qh2Msr3tOt6jPt1QYuv8ecbxkHCuxvqnFQSDbp+dRnd5GKJN45emgOkUGKQ6Aq68VSO7060r6PuE0EZKksH9ryRu8x9hSEVSPvgelC95ITxstkM7M39xc1QHtBxkqyHtooDIY3vySOcbFy0AAo/HfSr8HoJJjlDm/Q5MROKvpTBtOhoqLkTHe1ufDtrzZsaCiaozmakGgMDSpipCqI9dLNzdvaXyEbt1gIDIiiAjGv4p/ihJyYxJz0LiD/4OODQ4tqpdvtt6a1lL6BHwoahjaF+5N+/gUlNYVcSDrMOCKBIvg2hmj0lt/UaLnxoL63+D0yAQexCBal3me3/sYHL7WgWrgpKNmqYh+eak5Vnbm4YMcGtkbhwL1v/UzUe4gykBTgKerzAlGCIMN42HIvvJELCZiCrNPkuUnGwM1gEfkBC5jE9LZizS33OOuDhCyrDMRUjnz20BmGwJZ4btNLr5D6/IENsOYWhzfxVIC6t6RgZK9vvBQLqTKVduH8u9Lz0p3IcrKRSTfxRrVUeTJ5rytX24OkijIMplPohqvjpOZ5QiOoY1GPPgumxOyFHB+7lbc48S5PB/YtoDSHEqpuYJlz5IPbXURkJkyn1YSdrw2lrB3OORqoOKzYdzFArY7jwExjJRvqXTJUlxJ1HpeCZap+lh1m1pH4kd7hxS/bdhiw=="));
        trustedCertificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString("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"));
        trustedCertificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString("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"));
        trustedCertificateSource.addCertificate(DSSUtils.loadCertificateFromBase64EncodedString("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"));
        return trustedCertificateSource;
    }

    @Override
    protected void checkCertificates(DiagnosticData diagnosticData) {
        super.checkCertificates(diagnosticData);

        SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
        assertNotNull(signature);

        FoundCertificatesProxy foundCertificates = signature.foundCertificates();
        assertEquals(5, foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.CERTIFICATE_VALUES).size());
        assertEquals(3, foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.TIMESTAMP_VALIDATION_DATA).size());

        FoundRevocationsProxy foundRevocations = signature.foundRevocations();
        assertEquals(0, foundRevocations.getRelatedRevocationsByTypeAndOrigin(RevocationType.CRL, RevocationOrigin.REVOCATION_VALUES).size());
        assertEquals(1, foundRevocations.getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.REVOCATION_VALUES).size());
        assertEquals(0, foundRevocations.getRelatedRevocationsByTypeAndOrigin(RevocationType.CRL, RevocationOrigin.TIMESTAMP_VALIDATION_DATA).size());
        assertEquals(1, foundRevocations.getRelatedRevocationsByTypeAndOrigin(RevocationType.OCSP, RevocationOrigin.TIMESTAMP_VALIDATION_DATA).size());
    }

    @Override
    protected void checkTimestamps(DiagnosticData diagnosticData) {
        super.checkTimestamps(diagnosticData);

        SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
        assertEquals(1, signature.getSignatureTimestamps().size());
        assertEquals(3, signature.getArchiveTimestamps().size());
    }

    @Override
    protected void validateETSISignatureAttributes(SignatureAttributesType signatureAttributes) {
        super.validateETSISignatureAttributes(signatureAttributes);

        boolean certificateValuesFound = false;
        boolean revocationValuesFound = false;
        boolean timestampValidationDataFound = false;

        List<Object> signatureAttributeObjects = signatureAttributes.getSigningTimeOrSigningCertificateOrDataObjectFormat();
        for (Object signatureAttributeObj : signatureAttributeObjects) {
            if (signatureAttributeObj instanceof JAXBElement) {
                JAXBElement jaxbElement = (JAXBElement) signatureAttributeObj;
                Object value = jaxbElement.getValue();
                if (value instanceof AttributeBaseType) {
                    AttributeBaseType attributeBase = (AttributeBaseType) value;
                    if ("CertificateValues".equals(jaxbElement.getName().getLocalPart())) {
                        assertEquals(5, attributeBase.getAttributeObject().get(0).getVOReference().size());
                        certificateValuesFound = true;

                    } else if ("RevocationValues".equals(jaxbElement.getName().getLocalPart())) {
                        assertEquals(1, attributeBase.getAttributeObject().get(0).getVOReference().size());
                        revocationValuesFound = true;

                    } else if ("TimeStampValidationData".equals(jaxbElement.getName().getLocalPart())) {
                        assertEquals(4, attributeBase.getAttributeObject().get(0).getVOReference().size()); // 3 cert + 1 ocsp
                        timestampValidationDataFound = true;
                    }
                }
            }
        }
        assertTrue(certificateValuesFound);
        assertTrue(revocationValuesFound);
        assertTrue(timestampValidationDataFound);
    }

}
