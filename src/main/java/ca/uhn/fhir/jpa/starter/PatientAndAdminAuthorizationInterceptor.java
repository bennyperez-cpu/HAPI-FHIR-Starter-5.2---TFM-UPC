package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.IdType;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

@SuppressWarnings("ConstantConditions")
public class PatientAndAdminAuthorizationInterceptor extends AuthorizationInterceptor {

    private String getRequestorType (RequestDetails theRequestDetails) {
        String id = theRequestDetails.getId().toString();
        String patientId = id.substring(id.indexOf('/') + 1);// Paciente del cual se consulta
        String authHeader = theRequestDetails.getHeader("Authorization");
        if (("Bearer " + patientId).equals(authHeader)) {
            return "Self";
        }
        Client client = ClientBuilder.newClient();
        Response response = client.target("http://localhost:8080/hapi_fhir_jpaserver_starter_war/fhir/")
                .path("Patient").path(patientId).request(MediaType.APPLICATION_JSON).header("Authorization", "Bearer 39ff939jgg").get();
        JsonNode node = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            node = mapper.readTree(response.readEntity(String.class));
            String patientPractitioner = node.get("generalPractitioner").get(0).get("identifier").get("id").asText();
            if (("Bearer " + patientPractitioner).equals(authHeader)) {
                return "PatientPractitioner";
            }
            try {
                String managingOrganization = node.get("managingOrganization").get("identifier").get("id").asText();
                if (("Bearer " + managingOrganization).equals(authHeader)) {
                    return "PatientOrganization";
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return "Other";
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        // Process authorization header - The following is a fake
        // implementation. Obviously we'd want something more real
        // for a production scenario.
        //
        // In this basic example we have two hardcoded bearer tokens,
        // one which is for a user that has access to one patient, and
        // another that has full access.
        String authHeader = theRequestDetails.getHeader("Authorization");
        boolean userIsAdmin = false;
        boolean userIsPractitioner = false;
        boolean userIsOrganization = false;

        String requestorType = null;

        IdType userIdPatientId = null;
        IdType userId = null;
        if ("Bearer 39ff939jgg".equals(authHeader)) {
            // This user has access to everything
            userIsAdmin = true;
        }
        else {
            requestorType = getRequestorType(theRequestDetails);
        }

        if(userIsAdmin || requestorType.equals("Self")){
            return new RuleBuilder()
                    .allowAll()
                    .build();
        }

        if(requestorType.equals("PatientPractitioner")){
            return new RuleBuilder()
                    .allow().read().resourcesOfType("Patient").inCompartment("Patient", new IdType("Patient/A")).andThen()
                    .allow().read().resourcesOfType("Observation").inCompartment("Patient", new IdType("Patient/A")).andThen()
                    .denyAll().andThen()
                    .build();
        }

        if(requestorType.equals("PatientOrganization")){
            return new RuleBuilder()
                    .allowAll()
                    .build();
        }


        // By default, deny everything. This should never get hit, but it's
        // good to be defensive
        return new RuleBuilder()
                .denyAll()
                .build();
    }
}