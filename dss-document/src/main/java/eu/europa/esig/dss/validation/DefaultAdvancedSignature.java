/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.alert.SilentOnStatusAlert;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.DigestDocument;
import eu.europa.esig.dss.model.identifier.EntityIdentifier;
import eu.europa.esig.dss.model.identifier.TokenIdentifier;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.x509.CertificateIdentifier;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.ListCertificateSource;
import eu.europa.esig.dss.spi.x509.revocation.Revocation;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLToken;
import eu.europa.esig.dss.spi.x509.revocation.crl.OfflineCRLSource;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OfflineOCSPSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.scope.SignatureScope;
import eu.europa.esig.dss.validation.scope.SignatureScopeFinder;
import eu.europa.esig.dss.validation.timestamp.TimestampSource;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;

public abstract class DefaultAdvancedSignature implements AdvancedSignature {

	private static final long serialVersionUID = 6452189007886779360L;

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAdvancedSignature.class);

	/**
	 * In the case of a non AdES signature the signing certificate is not mandatory within the signature and can be provided by the driving application.
	 */
	protected CertificateToken providedSigningCertificateToken;

	/**
	 * In case of a detached signature this is the signed document.
	 */
	protected List<DSSDocument> detachedContents;

	/**
	 * In case of a ASiC signature this is the archive or manifest content.
	 */
	private List<DSSDocument> containerContents;
	
	/**
	 * In case of a ASiC-E signature this is the list of found manifest files.
	 */
	protected List<ManifestFile> manifestFiles;

	/**
	 * This variable contains a list of reference validations (reference tag for
	 * XAdES or message-digest for CAdES)
	 */
	protected List<ReferenceValidation> referenceValidations;

	/**
	 * This variable contains the result of the signature mathematical validation. It is initialised when the method
	 * {@code checkSignatureIntegrity} is called.
	 */
	protected SignatureCryptographicVerification signatureCryptographicVerification;

	protected String structureValidation;

	private CertificateVerifier offlineCertificateVerifier;

	// Cached {@code SignatureCertificateSource}
	protected SignatureCertificateSource offlineCertificateSource;

	// Cached {@code OfflineCRLSource}
	protected OfflineCRLSource signatureCRLSource;

	// Cached {@code OfflineOCSPSource}
	protected OfflineOCSPSource signatureOCSPSource;

	// Cached {@code TimestampSource}
	protected TimestampSource signatureTimestampSource;

	private AdvancedSignature masterSignature;

	protected SignaturePolicy signaturePolicy;

	private List<SignatureScope> signatureScopes;

	private String signatureFilename;
	
	/*
	 * Unique signature identifier
	 */
	protected SignatureIdentifier signatureIdentifier;
	
	/**
	 * Build and defines {@code signatureIdentifier} value
	 */
	protected abstract SignatureIdentifier buildSignatureIdentifier();

	@Override
	public String getSignatureFilename() {
		return signatureFilename;
	}

	@Override
	public void setSignatureFilename(String signatureFilename) {
		this.signatureFilename = signatureFilename;
	}

	@Override
	public List<DSSDocument> getDetachedContents() {
		return detachedContents;
	}

	@Override
	public void setDetachedContents(final List<DSSDocument> detachedContents) {
		this.detachedContents = detachedContents;
	}
	
	@Override
	public List<DSSDocument> getContainerContents() {
		return containerContents;
	}
	
	@Override
	public void setContainerContents(List<DSSDocument> containerContents) {
		this.containerContents = containerContents;
	}
	
	@Override
	public void setManifestFiles(List<ManifestFile> manifestFiles) {
		this.manifestFiles = manifestFiles;
	}
	
	@Override
	public SignatureIdentifier getDSSId() {
		if (signatureIdentifier == null) {
			signatureIdentifier = buildSignatureIdentifier();
		}
		return signatureIdentifier;
	}
	
	@Override
	public String getId() {
		return getDSSId().asXmlId();
	}

	@Override
	public List<DSSDocument> getManifestedDocuments() {
		if (Utils.isCollectionEmpty(manifestFiles) || Utils.isCollectionEmpty(containerContents)) {
			return Collections.emptyList();
		}
		List<DSSDocument> foundManifestedDocuments = new ArrayList<>();
		for (ManifestFile manifestFile : manifestFiles) {
			if (Utils.areStringsEqual(manifestFile.getSignatureFilename(), signatureFilename)) {
				for (DSSDocument document : containerContents) {
					for (ManifestEntry entry : manifestFile.getEntries()) {
						if (Utils.areStringsEqual(entry.getFileName(), document.getName())) {
							foundManifestedDocuments.add(document);
						}
					}
				}
				break;
			}
		}
		return foundManifestedDocuments;
	}
	
	/**
	 * @return the upper level for which data have been found. Doesn't mean any validity of the data found. Null if
	 *         unknown.
	 */
	@Override
	public SignatureLevel getDataFoundUpToLevel() {
		final SignatureLevel[] signatureLevels = getSignatureLevels();
		final SignatureLevel dataFoundUpToProfile = getDataFoundUpToProfile(signatureLevels);
		return dataFoundUpToProfile;
	}
	
	@Override
	public ListCertificateSource getCompleteCertificateSource() {
		ListCertificateSource certificateSource = new ListCertificateSource(getCertificateSource());
		certificateSource.addAll(getTimestampSource().getTimestampCertificateSources());
		return certificateSource;
	}
	
	public ListCertificateSource getCertificateSourcesExceptLastArchiveTimestamp() {
		ListCertificateSource certificateSource = new ListCertificateSource(getCertificateSource());
		certificateSource.addAll(getTimestampSource().getTimestampCertificateSourcesExceptLastArchiveTimestamp());
		return certificateSource;
	}

	@Override
	public ListRevocationSource getCompleteCRLSource() {
		ListRevocationSource crlSource = new ListRevocationSource(getCRLSource());
		crlSource.addAll(getTimestampSource().getTimestampCRLSources());
		return crlSource;
	}

	@Override
	public ListRevocationSource getCompleteOCSPSource() {
		ListRevocationSource ocspSource = new ListRevocationSource(getOCSPSource());
		ocspSource.addAll(getTimestampSource().getTimestampOCSPSources());
		return ocspSource;
	}

	/**
	 * This method returns the {@code SignatureLevel} which was reached.
	 *
	 * @param signatureLevels
	 *            the array of the all levels associated with the given signature type
	 * @return {@code SignatureLevel}
	 */
	private SignatureLevel getDataFoundUpToProfile(final SignatureLevel... signatureLevels) {

		for (int ii = signatureLevels.length - 1; ii >= 0; ii--) {

			final SignatureLevel signatureLevel = signatureLevels[ii];
			if (isDataForSignatureLevelPresent(signatureLevel)) {
				return signatureLevel;
			}
		}
		return null;
	}
	
	/**
	 * ETSI TS 101 733 V2.2.1 (2013-04)
	 * 5.6.3 Signature Verification Process
	 * ...the public key from the first certificate identified in the sequence
	 * of certificate identifiers from SigningCertificate shall be the key used
	 * to verify the digital signature.
	 *
	 * @return
	 */
	@Override
	public CandidatesForSigningCertificate getCandidatesForSigningCertificate() {
		return getCertificateSource().getCandidatesForSigningCertificate(providedSigningCertificateToken);
	}

	/**
	 * This method prepares an offline CertificateVerifier. The instance is used to
	 * know if all required revocation data are present
	 * 
	 * @param certificateVerifier the configured CertificateVerifier with all
	 *                            external sources
	 */
	public void prepareOfflineCertificateVerifier(final CertificateVerifier certificateVerifier) {
		// AIA is disabled
		offlineCertificateVerifier = new CommonCertificateVerifier(true);
		if (certificateVerifier != null) {
			offlineCertificateVerifier.setAdjunctCertSource(certificateVerifier.getAdjunctCertSource());
			List<CertificateSource> trustedCertSources = certificateVerifier.getTrustedCertSources();
			for (CertificateSource certificateSource : trustedCertSources) {
				offlineCertificateVerifier.setTrustedCertSource(certificateSource);
			}
		}
		// disable alerting
		offlineCertificateVerifier.setAlertOnInvalidTimestamp(new SilentOnStatusAlert());
		offlineCertificateVerifier.setAlertOnMissingRevocationData(new SilentOnStatusAlert());
		offlineCertificateVerifier.setAlertOnNoRevocationAfterBestSignatureTime(new SilentOnStatusAlert());
		offlineCertificateVerifier.setAlertOnRevokedCertificate(new SilentOnStatusAlert());
		offlineCertificateVerifier.setAlertOnUncoveredPOE(new SilentOnStatusAlert());
	}

	/**
	 * This method validates the signing certificate and all timestamps.
	 *
	 * @return signature validation context containing all certificates and
	 *         revocation data used during the validation process.
	 */
	public ValidationContext getSignatureValidationContext(final CertificateVerifier certificateVerifier) {

		final ValidationContext validationContext = new SignatureValidationContext();
		certificateVerifier.setSignatureCRLSource(getCompleteCRLSource());
		certificateVerifier.setSignatureOCSPSource(getCompleteOCSPSource());
		certificateVerifier.setSignatureCertificateSource(getCompleteCertificateSource());
		
		validationContext.initialize(certificateVerifier);

		if (providedSigningCertificateToken != null) {
			validationContext.addCertificateTokenForVerification(providedSigningCertificateToken);
		}
		final List<CertificateToken> certificates = getCertificates();
		for (final CertificateToken certificate : certificates) {
			validationContext.addCertificateTokenForVerification(certificate);
		}
		prepareTimestamps(validationContext);
		validationContext.validate();

		validationContext.checkAllTimestampsValid();
		validationContext.checkAllRequiredRevocationDataPresent();
		validationContext.checkAllPOECoveredByRevocationData();
		validationContext.checkAllCertificatesValid();

		CertificateToken signingCertificateToken = getSigningCertificateToken();
		validationContext.checkAtLeastOneRevocationDataPresentAfterBestSignatureTime(signingCertificateToken);

		return validationContext;
	}

	/**
	 * Returns an unmodifiable list of all certificate tokens encapsulated in the
	 * signature
	 * 
	 * @see eu.europa.esig.dss.validation.AdvancedSignature#getCertificates()
	 */
	@Override
	public List<CertificateToken> getCertificates() {
		return getCertificateSource().getCertificates();
	}
	
	/**
	 * This method returns all validation data to be included into the signature
	 * @param validationContext {@link ValidationContext} contained all extracted data during the validation
	 * @return {@link ValidationDataForInclusion} all validation data to be included to the signature excluding duplicates and cross-certificates
	 */
	public ValidationDataForInclusion getValidationDataForInclusion(final ValidationContext validationContext) {
		Set<CertificateToken> certificatesForInclusion = getCertificatesForInclusion(validationContext);
		List<CRLToken> crlsForInclusion = getCRLsForInclusion(validationContext.getProcessedRevocations(), certificatesForInclusion);
		List<OCSPToken> ocspsForInclusion = getOCSPsForInclusion(validationContext.getProcessedRevocations(), certificatesForInclusion);
		
		return new ValidationDataForInclusion(certificatesForInclusion, crlsForInclusion, ocspsForInclusion);
	}

	/**
	 * This method returns all certificates used during the validation process. If a certificate's public key is
	 * already present within the signature then it is ignored.
	 *
	 * @param validationContext
	 *            validation context containing all information about the validation process of the signing certificate
	 *            and time-stamps
	 * @return set of certificates which public keys not yet present within the signature
	 */
	private Set<CertificateToken> getCertificatesForInclusion(final ValidationContext validationContext) {
		final Set<CertificateToken> certificatesForInclusion = getCompleteCertificateSource().getAllCertificateTokens();

		// avoid adding of cross-certificates to the list
		final List<EntityIdentifier> publicKeys = getEntityIdentifierList(certificatesForInclusion);
		for (final CertificateToken certificateToken : validationContext.getProcessedCertificates()) {
			if (!publicKeys.contains(certificateToken.getEntityKey())) {
				certificatesForInclusion.add(certificateToken);
			} else {
				LOG.debug("Certificate Token with Id : [{}] has not been added for inclusion. "
						+ "The same public key is already present!", certificateToken.getDSSIdAsString());
			}
		}
		return certificatesForInclusion;
	}

	private List<EntityIdentifier> getEntityIdentifierList(Collection<CertificateToken> certificateTokens) {
		final List<EntityIdentifier> entityIdentifiers = new ArrayList<>();
		for (CertificateToken certificateToken : certificateTokens) {
			entityIdentifiers.add(certificateToken.getEntityKey());
		}
		return entityIdentifiers;
	}

	/**
	 * This method returns CRLs that will be included in the LT profile.
	 *
	 * @param processedRevocations
	 *            {@link RevocationToken} contains all the revocation data retrieved during the validation process
	 * @param certificatesToBeIncluded
	 *            {@link CertificateToken} contains all the certificate tokens to be included to the signature
	 * @return list of {@link CRLToken}s to be included to the signature
	 */
	private List<CRLToken> getCRLsForInclusion(
			final Set<RevocationToken<Revocation>> processedRevocations,
			final Set<CertificateToken> certificatesToBeIncluded) {

		final List<CRLToken> crlTokens = new ArrayList<>();
		final List<TokenIdentifier> revocationIds = new ArrayList<>();

		for (final RevocationToken revocationToken : processedRevocations) {
			if (!revocationIds.contains(revocationToken.getDSSId()) && isAtLeastOneCertificateCovered(revocationToken, certificatesToBeIncluded)) {
				revocationIds.add(revocationToken.getDSSId());
				if (revocationToken instanceof CRLToken) {
					final CRLToken crlToken = (CRLToken) revocationToken;
					crlTokens.add(crlToken);
				}
			}
		}
		return crlTokens;
	}

	/**
	 * This method returns OCSPs that will be included in the LT profile.
	 *
	 * @param processedRevocations
	 *            {@link RevocationToken} contains all the revocation data retrieved during the validation process
	 * @param certificatesToBeIncluded
	 *            {@link CertificateToken} contains all the certificate tokens to be included to the signature
	 * @return list of {@link OCSPToken}s to be included to the signature
	 */
	private List<OCSPToken> getOCSPsForInclusion(
			final Set<RevocationToken<Revocation>> processedRevocations, 
			final Set<CertificateToken> certificatesToBeIncluded) {
		
		final List<OCSPToken> ocspTokens = new ArrayList<>();
		final List<TokenIdentifier> revocationIds = new ArrayList<>();
		
		for (final RevocationToken revocationToken : processedRevocations) {
			if (!revocationIds.contains(revocationToken.getDSSId()) && isAtLeastOneCertificateCovered(revocationToken, certificatesToBeIncluded)) {
				revocationIds.add(revocationToken.getDSSId());
				if (revocationToken instanceof OCSPToken) {
					final OCSPToken ocspToken = (OCSPToken) revocationToken;
					ocspTokens.add(ocspToken);
				}
			}
		}
		return ocspTokens;
	}
	
	/**
	 * The method allows to avoid adding of revocation data for certificates that had been removed from the inclusion
	 */
	private boolean isAtLeastOneCertificateCovered(RevocationToken revocationToken, 
			final Collection<CertificateToken> certificateTokens) {
		String relatedCertificateID = revocationToken.getRelatedCertificateID();
		if (Utils.isStringNotEmpty(relatedCertificateID)) {
			for (CertificateToken certificateToken : certificateTokens) {
				if (certificateToken.getDSSIdAsString().equals(relatedCertificateID)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setMasterSignature(final AdvancedSignature masterSignature) {
		this.masterSignature = masterSignature;
	}

	@Override
	public AdvancedSignature getMasterSignature() {
		return masterSignature;
	}
	
	@Override
	public boolean isCounterSignature() {
		return masterSignature != null;
	}

	@Override
	public SignatureCryptographicVerification getSignatureCryptographicVerification() {
		if (signatureCryptographicVerification == null) {
			checkSignatureIntegrity();
		}
		return signatureCryptographicVerification;
	}

	public static class ValidationDataForInclusion {

		public final Set<CertificateToken> certificateTokens;
		public final List<CRLToken> crlTokens;
		public final List<OCSPToken> ocspTokens;

		public ValidationDataForInclusion(final Set<CertificateToken> certificateTokens, 
				final List<CRLToken> crlTokens, final List<OCSPToken> ocspTokens) {
			this.certificateTokens = certificateTokens;
			this.crlTokens = crlTokens;
			this.ocspTokens = ocspTokens;
		}
		
	}
	
	@Override
	public List<SignerRole> getSignerRoles() {
		List<SignerRole> signerRoles = new ArrayList<>();
		List<SignerRole> claimedSignerRoles = getClaimedSignerRoles();
		if (Utils.isCollectionNotEmpty(claimedSignerRoles)) {
			signerRoles.addAll(claimedSignerRoles);
		}
		List<SignerRole> certifiedSignerRoles = getCertifiedSignerRoles();
		if (Utils.isCollectionNotEmpty(certifiedSignerRoles)) {
			signerRoles.addAll(certifiedSignerRoles);
		}
		return signerRoles;
	}

	@Override
	public CertificateToken getProvidedSigningCertificateToken() {
		return providedSigningCertificateToken;
	}

	@Override
	public void setProvidedSigningCertificateToken(final CertificateToken certificateToken) {
		this.providedSigningCertificateToken = certificateToken;
	}

	@Override
	public CertificateToken getSigningCertificateToken() {
		// This ensures that the variable candidatesForSigningCertificate has been initialized
		CandidatesForSigningCertificate candidatesForSigningCertificate = getCertificateSource()
				.getCandidatesForSigningCertificate(providedSigningCertificateToken);
		// This ensures that the variable signatureCryptographicVerification has been initialized
		signatureCryptographicVerification = getSignatureCryptographicVerification();
		final CertificateValidity theCertificateValidity = candidatesForSigningCertificate.getTheCertificateValidity();
		if (theCertificateValidity != null) {
			if (theCertificateValidity.isValid()) {
				final CertificateToken signingCertificateToken = theCertificateValidity.getCertificateToken();
				return signingCertificateToken;
			}
		}
		final CertificateValidity theBestCandidate = candidatesForSigningCertificate.getTheBestCandidate();
		return theBestCandidate == null ? null : theBestCandidate.getCertificateToken();
	}

	/**
	 * This method adds to the {@code ValidationContext} all timestamps to be validated.
	 *
	 * @param validationContext
	 *            {@code ValidationContext} to which the timestamps must be added
	 */
	@Override
	public void prepareTimestamps(final ValidationContext validationContext) {

		/*
		 * This validates the signature timestamp tokens present in the signature.
		 */
		for (final TimestampToken timestampToken : getContentTimestamps()) {
			validationContext.addTimestampTokenForVerification(timestampToken);
		}

		/*
		 * This validates the signature timestamp tokens present in the signature.
		 */
		for (final TimestampToken timestampToken : getSignatureTimestamps()) {
			validationContext.addTimestampTokenForVerification(timestampToken);
		}

		/*
		 * This validates the SigAndRefs timestamp tokens present in the signature.
		 */
		for (final TimestampToken timestampToken : getTimestampsX1()) {
			validationContext.addTimestampTokenForVerification(timestampToken);
		}

		/*
		 * This validates the RefsOnly timestamp tokens present in the signature.
		 */
		for (final TimestampToken timestampToken : getTimestampsX2()) {
			validationContext.addTimestampTokenForVerification(timestampToken);
		}

		/*
		 * This validates the archive timestamp tokens present in the signature.
		 */
		for (final TimestampToken timestampToken : getArchiveTimestamps()) {
			validationContext.addTimestampTokenForVerification(timestampToken);
		}
	}

	@Override
	public void validateStructure() {
	}

	@Override
	public String getStructureValidationResult() {
		return structureValidation;
	}

	@Override
	public SignaturePolicy getPolicyId() {
		return signaturePolicy;
	}
	
	@Override
	public void findSignatureScope(SignatureScopeFinder signatureScopeFinder) {
		signatureScopes = signatureScopeFinder.findSignatureScope(this);
	}

	@Override
	public List<SignatureScope> getSignatureScopes() {
		return signatureScopes;
	}
	
	@Override
	public List<TimestampToken> getContentTimestamps() {
		return getTimestampSource().getContentTimestamps();
	}

	@Override
	public List<TimestampToken> getSignatureTimestamps() {
		return getTimestampSource().getSignatureTimestamps();
	}

	@Override
	public List<TimestampToken> getTimestampsX1() {
		return getTimestampSource().getTimestampsX1();
	}

	@Override
	public List<TimestampToken> getTimestampsX2() {
		return getTimestampSource().getTimestampsX2();
	}

	@Override
	public List<TimestampToken> getArchiveTimestamps() {
		return getTimestampSource().getArchiveTimestamps();
	}

	@Override
	public List<TimestampToken> getDocumentTimestamps() {
		return getTimestampSource().getDocumentTimestamps();
	}
	
	@Override
	public List<TimestampToken> getAllTimestamps() {
		return getTimestampSource().getAllTimestamps();
	}

	@Override
	public void addExternalTimestamp(TimestampToken timestamp) {
		if (!timestamp.isProcessed()) {
			throw new DSSException("Timestamp token must be validated first !");
		}

		if (!timestamp.getTimeStampType().isArchivalTimestamp()) {
			throw new DSSException("Only archival timestamp is allowed !");
		}

		getTimestampSource().addExternalTimestamp(timestamp);
	}

	/* Defines the level T */
	public boolean hasTProfile() {
		return Utils.isCollectionNotEmpty(getSignatureTimestamps());
	}

	/* Defines the level LT */
	public boolean hasLTProfile() {
		ListCertificateSource certificateSources = getCertificateSourcesExceptLastArchiveTimestamp();
		boolean certificateFound = certificateSources.getNumberOfCertificates() > 0;
		boolean allSelfSigned = certificateFound && certificateSources.isAllSelfSigned();

		boolean emptyCRLs = getCompleteCRLSource().getAllRevocationBinaries().isEmpty();
		boolean emptyOCSPs = getCompleteOCSPSource().getAllRevocationBinaries().isEmpty();
		boolean emptyRevocation = emptyCRLs && emptyOCSPs;

		boolean minimalLTrequirement = !allSelfSigned && !emptyRevocation;
		if (minimalLTrequirement) {
			// check presence of all revocation data
			return isAllRevocationDataPresent(certificateSources);
		}
		return minimalLTrequirement;
	}

	private boolean isAllRevocationDataPresent(ListCertificateSource certificateSources) {
		SignatureValidationContext validationContext = new SignatureValidationContext();
		offlineCertificateVerifier.setSignatureCRLSource(getCompleteCRLSource());
		offlineCertificateVerifier.setSignatureOCSPSource(getCompleteOCSPSource());
		offlineCertificateVerifier.setSignatureCertificateSource(getCompleteCertificateSource());
		validationContext.initialize(offlineCertificateVerifier);
		if (providedSigningCertificateToken != null) {
			validationContext.addCertificateTokenForVerification(providedSigningCertificateToken);
		}
		for (final CertificateToken certificate : certificateSources.getAllCertificateTokens()) {
			validationContext.addCertificateTokenForVerification(certificate);
		}
		validationContext.validate();
		return validationContext.checkAllRequiredRevocationDataPresent();
	}
	
	@Override
	public boolean areAllSelfSignedCertificates() {
		ListCertificateSource certificateSources = getCompleteCertificateSource();
		boolean certificateFound = certificateSources.getNumberOfCertificates() > 0;
		return certificateFound && certificateSources.isAllSelfSigned();
	}

	/* Defines the level LTA */
	public boolean hasLTAProfile() {
		return Utils.isCollectionNotEmpty(getArchiveTimestamps());
	}
	
	@Override
	public boolean isDocHashOnlyValidation() {
		if (Utils.isCollectionNotEmpty(detachedContents)) {
			for (DSSDocument dssDocument : detachedContents) {
				if (!(dssDocument instanceof DigestDocument)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isHashOnlyValidation() {
		// TODO: not implemented yet
		return false;
	}
	
	@Override
	public byte[] getMessageDigestValue() {
		// Not applicable by default (CAdES/PAdES only)
		return null;
	}
	
	@Override
	public Set<CertificateIdentifier> getSignerInformationStoreInfos() {
		// Not applicable by default (CAdES/PAdES only)
		return Collections.emptySet();
	}
	
	@Override
	public PdfRevision getPdfRevision() {
		// Not applicable by default (PDF only)
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || !(obj instanceof DefaultAdvancedSignature)) {
			return false;
		}
		DefaultAdvancedSignature das = (DefaultAdvancedSignature) obj;
		return getDSSId().equals(das.getDSSId());
	}

	@Override
	public int hashCode() {
		return getDSSId().hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("%s Signature with Id : %s", getSignatureForm(), getId());
	}

}
