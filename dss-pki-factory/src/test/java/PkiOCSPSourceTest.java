/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * <p>
 * This file is part of the "DSS - Digital Signature Services" project.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureValidity;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pki.business.PostConstructInitializr;
import eu.europa.esig.dss.pki.db.Db;
import eu.europa.esig.dss.pki.exception.Error404Exception;
import eu.europa.esig.dss.pki.model.CertEntity;
import eu.europa.esig.dss.pki.model.DBCertEntity;
import eu.europa.esig.dss.pki.repository.CertEntityRepository;
import eu.europa.esig.dss.pki.revocation.ocsp.PkiOCSPSource;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PkiOCSPSourceTest {

    private static CertificateToken certificateToken;
    private static CertificateToken rootToken;

    private static CertificateToken goodUser;
    private static CertificateToken goodUserOCSPWithReqCertId;
    private static CertificateToken goodCa;
    private static CertificateToken ed25519goodUser;
    private static CertificateToken ed25519goodCa;
    static CertEntityRepository<DBCertEntity> certificateEntityService = Db.getInstance();
    private static CertEntity certEntity;
    private static CertificateToken revokedCa;
    private static CertificateToken revokedUser;

    @BeforeAll
    public static void init() {
        PostConstructInitializr.getInstance();

        certEntity = certificateEntityService.getCertEntity("good-user");
        rootToken = certificateEntityService.getCertEntity("root-ca").getCertificateToken();


        goodUser = certEntity.getCertificateToken();
        certificateToken = certEntity.getCertificateToken();
        goodUserOCSPWithReqCertId = certificateEntityService.getCertEntity("good-user-ocsp-certid-digest").getCertificateToken();
        goodCa = certEntity.getCertificateToken();
        ed25519goodUser = certificateEntityService.getCertEntity("Ed25519-good-user").getCertificateToken();
        ed25519goodCa = certificateEntityService.getCertEntity("Ed25519-good-ca").getCertificateToken();
        revokedCa = certificateEntityService.getCertEntity("revoked-ca").getCertificateToken();
        revokedUser = certificateEntityService.getCertEntity("revoked-user").getCertificateToken();
    }

    @Test
    public void testOCSPWithoutNonce() {
        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService, certEntity);
        OCSPToken ocspToken = ocspSource.getRevocationToken(certificateToken, rootToken);
        assertNotNull(ocspToken);
        assertNotNull(ocspToken.getBasicOCSPResp());
    }

    @Test
    public void testOCSP() {

        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService, certEntity);
        OCSPToken ocspToken = ocspSource.getRevocationToken(goodUser, goodCa);
        System.out.println(ocspToken.toString());
        assertNotNull(ocspToken);
        assertNotNull(ocspToken.getBasicOCSPResp());
    }
  @Test
    public void testOCSPRevoked() {

        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService);
        OCSPToken ocspToken = ocspSource.getRevocationToken(revokedCa,revokedCa);
        System.out.println(ocspToken.toString());
        assertNotNull(ocspToken);
        assertNotNull(ocspToken.getBasicOCSPResp());
    }


    @Test
    public void testWithSetDataLoader() {
        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService);
        OCSPToken ocspToken = ocspSource.getRevocationToken(goodUser, goodCa);
        assertNotNull(ocspToken);
        assertNotNull(ocspToken.getBasicOCSPResp());
    }

    @Test
    public void testOCSPEd25519() {
        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService);
        ocspSource.setDigestAlgorithm(DigestAlgorithm.SHA512);
        Exception exception = assertThrows(Error404Exception.class, () -> ocspSource.getRevocationToken(ed25519goodUser, ed25519goodCa));
        assertTrue(exception.getMessage().contains("not found for CA '"));
//        OCSPToken ocspToken = ocspSource.getRevocationToken(ed25519goodUser, ed25519goodCa);
//        assertNotNull(ocspToken);
//        assertNotNull(ocspToken.getBasicOCSPResp());
//        assertEquals(SignatureAlgorithm.ED25519, ocspToken.getSignatureAlgorithm());
//        assertEquals(SignatureValidity.VALID, ocspToken.getSignatureValidity());
    }

    @Test
    public void testOCSPWithNonce() {
        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService, certEntity);
        ocspSource.setProductionDate(new Date());
        OCSPToken ocspToken = ocspSource.getRevocationToken(certificateToken, rootToken);
        assertNotNull(ocspToken);
    }

    @Test
    public void customCertIDDigestAlgorithmTest() {

        PkiOCSPSource ocspSource = new PkiOCSPSource(certificateEntityService);

        OCSPToken ocspToken = ocspSource.getRevocationToken(goodUserOCSPWithReqCertId, goodCa);
        assertNotNull(ocspToken);
        assertEquals(SignatureAlgorithm.RSA_SHA1, ocspToken.getSignatureAlgorithm()); // default value

        ocspSource.setDigestAlgorithm(DigestAlgorithm.SHA256);
        ocspToken = ocspSource.getRevocationToken(goodUserOCSPWithReqCertId, goodCa);
        assertEquals(SignatureAlgorithm.RSA_SHA256, ocspToken.getSignatureAlgorithm());

        ocspSource.setDigestAlgorithm(DigestAlgorithm.SHA512);
        ocspToken = ocspSource.getRevocationToken(goodUserOCSPWithReqCertId, goodCa);
        assertEquals(SignatureAlgorithm.RSA_SHA512, ocspToken.getSignatureAlgorithm());

        ocspSource.setDigestAlgorithm(DigestAlgorithm.SHA3_256);
        ocspToken = ocspSource.getRevocationToken(goodUserOCSPWithReqCertId, goodCa);
        assertEquals(SignatureAlgorithm.RSA_SHA3_256, ocspToken.getSignatureAlgorithm());

        ocspSource.setDigestAlgorithm(DigestAlgorithm.SHA3_512);
        ocspToken = ocspSource.getRevocationToken(goodUserOCSPWithReqCertId, goodCa);
        assertEquals(SignatureAlgorithm.RSA_SHA3_512, ocspToken.getSignatureAlgorithm());
    }


}
