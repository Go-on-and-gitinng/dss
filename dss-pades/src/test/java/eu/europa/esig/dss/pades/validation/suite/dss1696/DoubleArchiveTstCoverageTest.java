package eu.europa.esig.dss.pades.validation.suite.dss1696;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.FoundCertificatesProxy;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.validation.PAdESSignature;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.pades.validation.suite.AbstractPAdESTestValidation;
import eu.europa.esig.dss.pdf.PdfDssDict;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.PdfRevision;
import eu.europa.esig.dss.validation.SignatureCertificateSource;
import eu.europa.esig.dss.validation.SignedDocumentValidator;

public class DoubleArchiveTstCoverageTest extends AbstractPAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new InMemoryDocument(
				getClass().getResourceAsStream("/validation/dss-1696/Test.signed_Certipost-2048-SHA512.extended.extended-2019-07-02.pdf"));
	}
	
	@Override
	protected void checkAdvancedSignatures(List<AdvancedSignature> signatures) {
		super.checkAdvancedSignatures(signatures);
		
		assertEquals(1, signatures.size());
	}
	
	@Override
	protected void checkValidationContext(SignedDocumentValidator validator) {
		super.checkValidationContext(validator);
		
		PDFDocumentValidator pdfDocumentValidator = (PDFDocumentValidator) validator;

		List<AdvancedSignature> signatures = pdfDocumentValidator.getSignatures();

		// <</Type /DSS
		// /Certs [20 0 R 26 0 R 30 0 R 35 0 R 39 0 R 40 0 R]
		// /CRLs [21 0 R 22 0 R 27 0 R 28 0 R 29 0 R 34 0 R 36 0 R 37 0 R 38 0 R]>>
		PAdESSignature pades = (PAdESSignature) signatures.get(0);
		PdfDssDict dssDictionary = pades.getDssDictionary();
		assertNotNull(dssDictionary);
		assertEquals(6, dssDictionary.getCERTs().size());
		assertEquals(9, dssDictionary.getCRLs().size());

		PdfRevision pdfRevision = pades.getPdfRevision();
		assertNotNull(pdfRevision);
		
		List<PdfDssDict> dssDictionaries = pdfDocumentValidator.getDssDictionaries();
		assertEquals(3, dssDictionaries.size());
		
		Iterator<PdfDssDict> iterator = dssDictionaries.iterator();

		// <</Type /DSS
		// /Certs [20 0 R]
		// /CRLs [21 0 R 22 0 R]>>
		dssDictionary = iterator.next();
		
		assertNotNull(dssDictionary);
		assertEquals(1, dssDictionary.getCERTs().size());
		assertEquals(2, dssDictionary.getCRLs().size());

		// <</Type /DSS
		// /Certs [20 0 R 26 0 R 30 0 R]
		// /CRLs [21 0 R 22 0 R 27 0 R 28 0 R 29 0 R]>>
		dssDictionary = iterator.next();
		
		assertNotNull(dssDictionary);
		assertEquals(3, dssDictionary.getCERTs().size());
		assertEquals(5, dssDictionary.getCRLs().size());

		// Same than for signature
		// <</Type /DSS
		// /Certs [20 0 R 26 0 R 30 0 R 35 0 R 39 0 R 40 0 R]
		// /CRLs [21 0 R 22 0 R 27 0 R 28 0 R 29 0 R 34 0 R 36 0 R 37 0 R 38 0 R]>>
		dssDictionary = iterator.next();
		
		assertNotNull(dssDictionary);
		assertEquals(6, dssDictionary.getCERTs().size());
		assertEquals(9, dssDictionary.getCRLs().size());
	}
	
	@Override
	protected void verifySourcesAndDiagnosticData(List<AdvancedSignature> advancedSignatures,
			DiagnosticData diagnosticData) {
		SignatureCertificateSource certificateSource = advancedSignatures.get(0).getCertificateSource();
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		FoundCertificatesProxy foundCertificates = signatureWrapper.foundCertificates();
		
		assertNotEquals(certificateSource.getDSSDictionaryCertValues().size(),
					foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size() +
					foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size());
		assertEquals(new HashSet<>(certificateSource.getDSSDictionaryCertValues()).size(), 
				foundCertificates.getRelatedCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size() +
				foundCertificates.getOrphanCertificatesByOrigin(CertificateOrigin.DSS_DICTIONARY).size());
	}
	
	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		assertFalse(diagnosticData.isTLevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
		assertTrue(diagnosticData.isALevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
	}

}
