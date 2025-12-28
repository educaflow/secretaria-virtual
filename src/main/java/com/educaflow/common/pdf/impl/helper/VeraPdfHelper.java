package com.educaflow.common.pdf.impl.helper;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.Map.Entry;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.flavours.PDFFlavours;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.RuleId;





/**
 *
 * @author logongas
 */
public class VeraPdfHelper {
    
    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    public static Map<PDFAFlavour, List<String>> getPDFAFlavourValid(byte[] datos) {
        Map<PDFAFlavour, List<String>> resultado = new HashMap<>();

        try (PDFAParser parser = Foundries.defaultInstance().createParser(new ByteArrayInputStream(datos))) {
            List<PDFAFlavour> detectedFlavours = getFlavoursPdfAPdfUA(parser.getFlavours());
            PDFAValidator validator = Foundries.defaultInstance().createValidator(detectedFlavours);


            List<ValidationResult> results = validator.validateAll(parser);
            for (ValidationResult result : results) {

                resultado.put(result.getPDFAFlavour(), getMensajesFallos(result));
            }

            return resultado;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<String> getMensajesFallos(ValidationResult result) {
        List<String> mensajesFallos = new ArrayList<>();

        if (result.isCompliant() == true) {
            return null;
        }

        for (TestAssertion testAssertion : result.getTestAssertions()) {
            mensajesFallos.add("Assert--> " + testAssertion.getStatus() + ":" + testAssertion.getMessage());
        }
        for (Entry<RuleId, Integer> failedCheck : result.getFailedChecks().entrySet()) {
            RuleId ruleId = failedCheck.getKey();
            int count = failedCheck.getValue();
            Rule rule = result.getValidationProfile().getRuleByRuleId(ruleId);
            String descripcion = (rule != null) ? rule.getDescription() : ruleId.toString();

            mensajesFallos.add("Check--> " + descripcion + " (fallos: " + count + ")");
        }

        List<String> mensajesFallosSinDuplicados=new ArrayList<>(new LinkedHashSet<>(mensajesFallos));

        return mensajesFallosSinDuplicados;
    }

    private static List<PDFAFlavour> getFlavoursPdfAPdfUA(List<PDFAFlavour> detectedFlavours) {
        List<PDFAFlavour> flavours = new LinkedList<>();
        for (PDFAFlavour flavour : detectedFlavours) {
            if (PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_A) || PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_UA)) {
                flavours.add(flavour);
            }
        }

        return flavours;
    }
}
