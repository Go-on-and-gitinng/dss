package eu.europa.esig.dss.pades.timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFRevision;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFSignatureDictionary;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.PAdESTimestampParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pades.validation.suite.AbstractPAdESTestValidation;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.validationreport.jaxb.ValidationStatusType;

public abstract class AbstractPDFTimestampServiceTest extends AbstractPAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		PAdESService service = new PAdESService(getOfflineCertificateVerifier());
		service.setTspSource(getGoodTsa());

		PAdESTimestampParameters parameters = new PAdESTimestampParameters();

		DSSDocument document = new InMemoryDocument(getClass().getResourceAsStream("/sample.pdf"));
		DSSDocument timestamped = service.timestamp(document, parameters);
		
		return timestamped;
	}

	@Override
	protected void checkAdvancedSignatures(List<AdvancedSignature> signatures) {
		assertFalse(Utils.isCollectionNotEmpty(signatures));
	}

	@Override
	protected void checkNumberOfSignatures(DiagnosticData diagnosticData) {
		assertFalse(Utils.isCollectionNotEmpty(diagnosticData.getSignatures()));
		assertFalse(Utils.isCollectionNotEmpty(diagnosticData.getSignatureIdList()));
	}
	
	@Override
	protected void validateValidationStatus(ValidationStatusType signatureValidationStatus) {
		assertEquals(Indication.NO_SIGNATURE_FOUND, signatureValidationStatus.getMainIndication());
	}
	
	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		super.checkTimestamps(diagnosticData);
		
		assertEquals(1, diagnosticData.getTimestampList().size());
	}
	
	@Override
	protected void checkPdfRevision(DiagnosticData diagnosticData) {
		super.checkPdfRevision(diagnosticData);
		
		TimestampWrapper timestampWrapper = diagnosticData.getTimestampList().get(0);
		XmlPDFRevision pdfRevision = timestampWrapper.getPDFRevision();
		assertNotNull(pdfRevision);
		assertEquals(1, pdfRevision.getSignatureFieldName().size());
		
		XmlPDFSignatureDictionary pdfSignatureDictionary = pdfRevision.getPDFSignatureDictionary();
		assertNotNull(pdfSignatureDictionary);
		
		assertEquals("Adobe.PPKLite", pdfSignatureDictionary.getFilter());
		assertEquals("ETSI.RFC3161", pdfSignatureDictionary.getSubFilter());
	}

}
