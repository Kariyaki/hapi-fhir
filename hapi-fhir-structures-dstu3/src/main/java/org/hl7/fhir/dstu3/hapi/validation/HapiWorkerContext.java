package org.hl7.fhir.dstu3.hapi.validation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.hl7.fhir.dstu3.formats.IParser;
import org.hl7.fhir.dstu3.formats.ParserType;
import org.hl7.fhir.dstu3.hapi.validation.IValidationSupport.CodeValidationResult;
import org.hl7.fhir.dstu3.model.BaseConformance;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ConceptMap;
import org.hl7.fhir.dstu3.model.ExpansionProfile;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptReferenceComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpander;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpanderFactory;
import org.hl7.fhir.dstu3.terminologies.ValueSetExpanderSimple;
import org.hl7.fhir.dstu3.utils.INarrativeGenerator;
import org.hl7.fhir.dstu3.utils.IWorkerContext;
import org.hl7.fhir.dstu3.validation.IResourceValidator;
import org.hl7.fhir.exceptions.TerminologyServiceException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.CoverageIgnore;

public final class HapiWorkerContext implements IWorkerContext, ValueSetExpander, ValueSetExpanderFactory {
	private final FhirContext myCtx;
	private Map<String, Resource> myFetchedResourceCache = new HashMap<String, Resource>();
	private IValidationSupport myValidationSupport;
	private ExpansionProfile myExpansionProfile;

	public HapiWorkerContext(FhirContext theCtx, IValidationSupport theValidationSupport) {
		Validate.notNull(theCtx, "theCtx must not be null");
		Validate.notNull(theValidationSupport, "theValidationSupport must not be null");
		myCtx = theCtx;
		myValidationSupport = theValidationSupport;
	}

	@Override
	public List<StructureDefinition> allStructures() {
		return myValidationSupport.fetchAllStructureDefinitions(myCtx);
	}

	@Override
	public CodeSystem fetchCodeSystem(String theSystem) {
		if (myValidationSupport == null) {
			return null;
		} else {
			return myValidationSupport.fetchCodeSystem(myCtx, theSystem);
		}
	}

	@Override
	public <T extends Resource> T fetchResource(Class<T> theClass, String theUri) {
		if (myValidationSupport == null) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			T retVal = (T) myFetchedResourceCache.get(theUri);
			if (retVal == null) {
				retVal = myValidationSupport.fetchResource(myCtx, theClass, theUri);
				if (retVal != null) {
					myFetchedResourceCache.put(theUri, retVal);
				}
			}
			return retVal;
		}
	}

	@Override
	public List<ConceptMap> findMapsForSource(String theUrl) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAbbreviation(String theName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueSetExpander getExpander() {
		ValueSetExpanderSimple retVal = new ValueSetExpanderSimple(this, this);
		retVal.setMaxExpansionSize(Integer.MAX_VALUE);
		return retVal;
	}

	@Override
	public INarrativeGenerator getNarrativeGenerator(String thePrefix, String theBasePath) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser getParser(ParserType theType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser getParser(String theType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getResourceNames() {
		List<String> result = new ArrayList<String>();
		for (ResourceType next : ResourceType.values()) {
			result.add(next.name());
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public <T extends Resource> boolean hasResource(Class<T> theClass_, String theUri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser newJsonParser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IResourceValidator newValidator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IParser newXmlParser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String oid2Uri(String theCode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsSystem(String theSystem) {
		if (myValidationSupport == null) {
			return false;
		} else {
			return myValidationSupport.isCodeSystemSupported(myCtx, theSystem);
		}
	}

	@Override
	public Set<String> typeTails() {
		return new HashSet<String>(Arrays.asList("Integer", "UnsignedInt", "PositiveInt", "Decimal", "DateTime", "Date", "Time", "Instant", "String", "Uri", "Oid", "Uuid", "Id", "Boolean", "Code",
				"Markdown", "Base64Binary", "Coding", "CodeableConcept", "Attachment", "Identifier", "Quantity", "SampledData", "Range", "Period", "Ratio", "HumanName", "Address", "ContactPoint",
				"Timing", "Reference", "Annotation", "Signature", "Meta"));
	}

	@Override
	public ValidationResult validateCode(CodeableConcept theCode, ValueSet theVs) {
		for (Coding next : theCode.getCoding()) {
			ValidationResult retVal = validateCode(next, theVs);
			if (retVal != null && retVal.isOk()) {
				return retVal;
			}
		}

		return new ValidationResult(null, null);
	}

	@Override
	public ValidationResult validateCode(Coding theCode, ValueSet theVs) {
		String system = theCode.getSystem();
		String code = theCode.getCode();
		String display = theCode.getDisplay();
		return validateCode(system, code, display, theVs);
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay) {
		CodeValidationResult result = myValidationSupport.validateCode(myCtx, theSystem, theCode, theDisplay);
		if (result == null) {
			return null;
		}
		return new ValidationResult(result.getSeverity(), result.getMessage(), result.asConceptDefinition());
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay, ConceptSetComponent theVsi) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValidationResult validateCode(String theSystem, String theCode, String theDisplay, ValueSet theVs) {

		boolean caseSensitive = true;
		CodeSystem system = fetchCodeSystem(theSystem);
		if (system != null) {
			if (system.hasCaseSensitive()) {
				caseSensitive = system.getCaseSensitive();
			}
		}

		String wantCode = theCode;
		if (!caseSensitive) {
			wantCode = wantCode.toUpperCase();
		}

		ValueSetExpansionOutcome expandedValueSet = null;

		/*
		 * The following valueset is a special case, since the BCP codesystem is very difficult to expand
		 */
		if (theVs != null && "http://hl7.org/fhir/ValueSet/languages".equals(theVs.getId())) {
			ValueSet expansion = new ValueSet();
			for (ConceptSetComponent nextInclude : theVs.getCompose().getInclude()) {
				for (ConceptReferenceComponent nextConcept : nextInclude.getConcept()) {
					expansion.getExpansion().addContains().setCode(nextConcept.getCode()).setDisplay(nextConcept.getDisplay());
				}
			}
			expandedValueSet= new ValueSetExpansionOutcome(expansion);
		}

		// if (theVs.getCompose().hasExclude() == false) {
		// if (theVs.getCompose().hasImport() == false) {
		// if (theVs.getCompose().hasInclude()) {
		// for (ConceptSetComponent nextInclude : theVs.getCompose().getInclude()) {
		// if (nextInclude.hasFilter() == false) {
		// if (nextInclude.hasConcept() == false) {
		// if (nextInclude.hasSystem()) {
		//
		// }
		// }
		// }
		// }
		// }
		// }
		// }

		if (expandedValueSet == null) {
			expandedValueSet = expand(theVs, null);
		}
		
		for (ValueSetExpansionContainsComponent next : expandedValueSet.getValueset().getExpansion().getContains()) {
			String nextCode = next.getCode();
			if (!caseSensitive) {
				nextCode = nextCode.toUpperCase();
			}

			if (nextCode.equals(wantCode)) {
				if (theSystem == null || next.getSystem().equals(theSystem)) {
					ConceptDefinitionComponent definition = new ConceptDefinitionComponent();
					definition.setCode(next.getCode());
					definition.setDisplay(next.getDisplay());
					ValidationResult retVal = new ValidationResult(definition);
					return retVal;
				}
			}
		}

		// for (UriType nextComposeImport : theVs.getCompose().getImport()) {
		// if (isNotBlank(nextComposeImport.getValue())) {
		// aaa
		// }
		// }
		// for (ConceptSetComponent nextComposeConceptSet : theVs.getCompose().getInclude()) {
		// if (theSystem == null || StringUtils.equals(theSystem, nextComposeConceptSet.getSystem())) {
		// if (nextComposeConceptSet.getConcept().isEmpty()) {
		// ValidationResult retVal = validateCode(nextComposeConceptSet.getSystem(), theCode, theDisplay);
		// if (retVal != null && retVal.isOk()) {
		// return retVal;
		// }
		// } else {
		// for (ConceptReferenceComponent nextComposeCode : nextComposeConceptSet.getConcept()) {
		// ConceptDefinitionComponent conceptDef = new ConceptDefinitionComponent();
		// conceptDef.setCode(nextComposeCode.getCode());
		// conceptDef.setDisplay(nextComposeCode.getDisplay());
		// ValidationResult retVal = validateCodeSystem(theCode, conceptDef);
		// if (retVal != null && retVal.isOk()) {
		// return retVal;
		// }
		// }
		// }
		// }
		// }
		return new ValidationResult(IssueSeverity.ERROR, "Unknown code[" + theCode + "] in system[" + theSystem + "]");
	}

	@Override
	@CoverageIgnore
	public List<BaseConformance> allConformanceResources() {
		throw new UnsupportedOperationException();
	}

	@Override
	@CoverageIgnore
	public boolean hasCache() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueSetExpansionOutcome expand(ValueSet theSource, ExpansionProfile theProfile) {
		ValueSetExpansionOutcome vso;
		try {
			vso = getExpander().expand(theSource, theProfile);
		} catch (InvalidRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalErrorException(e);
		}
		if (vso.getError() != null) {
			throw new InvalidRequestException(vso.getError());
		} else {
			return vso;
		}
	}

	@Override
	public ExpansionProfile getExpansionProfile() {
		return myExpansionProfile;
	}

	@Override
	public void setExpansionProfile(ExpansionProfile theExpProfile) {
		myExpansionProfile = theExpProfile;
	}

	@Override
	public ValueSetExpansionOutcome expandVS(ValueSet theSource, boolean theCacheOk, boolean theHeiarchical) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ValueSetExpansionComponent expandVS(ConceptSetComponent theInc, boolean theHeiarchical) throws TerminologyServiceException {
		return myValidationSupport.expandValueSet(myCtx, theInc);
	}

	@Override
	public void setLogger(ILoggingService theLogger) {
		throw new UnsupportedOperationException();
	}

}