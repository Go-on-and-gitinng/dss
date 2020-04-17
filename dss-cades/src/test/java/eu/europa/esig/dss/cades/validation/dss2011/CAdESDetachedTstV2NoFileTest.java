package eu.europa.esig.dss.cades.validation.dss2011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import eu.europa.esig.dss.cades.validation.AbstractCAdESTestValidation;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.ArchiveTimestampType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;

public class CAdESDetachedTstV2NoFileTest extends AbstractCAdESTestValidation {

	@Override
	protected DSSDocument getSignedDocument() {
		return new FileDocument("src/test/resources/validation/dss-2011/cades-tstv2-detached.p7s");
	}
	
	@Override
	protected void checkBLevelValid(DiagnosticData diagnosticData) {
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertFalse(signature.isSignatureIntact());
		assertFalse(signature.isSignatureValid());
		assertFalse(signature.isBLevelTechnicallyValid());
		
		List<XmlDigestMatcher> digestMatchers = signature.getDigestMatchers();
		assertEquals(1, digestMatchers.size());
		for (XmlDigestMatcher digestMatcher : digestMatchers) {
			assertFalse(digestMatcher.isDataFound());
			assertFalse(digestMatcher.isDataIntact());
		}
	}
	
	@Override
	protected void checkSignatureLevel(DiagnosticData diagnosticData) {
		assertTrue(diagnosticData.isTLevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
		assertFalse(diagnosticData.isALevelTechnicallyValid(diagnosticData.getFirstSignatureId()));
	}
	
	@Override
	protected void checkTimestamps(DiagnosticData diagnosticData) {
		int v2ArchiveTsts = 0;
		for (TimestampWrapper timestamp : diagnosticData.getTimestampList()) {
			if (ArchiveTimestampType.CAdES_V2.equals(timestamp.getArchiveTimestampType())) {
				assertFalse(timestamp.isMessageImprintDataFound());
				assertFalse(timestamp.isMessageImprintDataIntact());
				++v2ArchiveTsts;
			}
		}
		assertEquals(1, v2ArchiveTsts);
	}
	
	@Override
	protected void checkSigningCertificateValue(DiagnosticData diagnosticData) {
		SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		assertTrue(signatureWrapper.isAttributePresent());
		assertTrue(signatureWrapper.isDigestValuePresent());
		assertTrue(signatureWrapper.isDigestValueMatch());
	}

}
