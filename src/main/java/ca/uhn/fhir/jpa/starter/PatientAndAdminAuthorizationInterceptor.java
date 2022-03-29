package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;

import java.util.List;

@SuppressWarnings("ConstantConditions")
public class PatientAndAdminAuthorizationInterceptor extends AuthorizationInterceptor {


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
        IdType userIdPatientId = null;
        if ("Bearer 39ff939jgg".equals(authHeader)) {
            // This user has access to everything
            userIsAdmin = true;
        }
        else {
            String id = theRequestDetails.getId().toString();
            String patientId = id.substring(id.indexOf('/') + 1);
            FhirContext ctx = FhirContext.forR4();
            IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/fhir/");
            BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor("39ff939jgg");
            client.registerInterceptor(authInterceptor);
            Patient patient = client.read().resource(Patient.class).withId(patientId).execute();


            if (("Bearer " + patientId).equals(authHeader)) {
                // This user has access only to Patient/1 resources
                userIdPatientId = new IdType("Patient", patientId);
            } else {
                // Throw an HTTP 401
                throw new AuthenticationException( "Missing or invalid Authorization header value " + patient.getId());
            }
        }

        // If the user is a specific patient, we create the following rule chain:
        // Allow the user to read anything in their own patient compartment
        // Allow the user to write anything in their own patient compartment
        // If a client request doesn't pass either of the above, deny it
        if (userIdPatientId != null) {
            return new RuleBuilder()
                    .allow().read().allResources().inCompartment("Patient", userIdPatientId).andThen()
                    .allow().write().allResources().inCompartment("Patient", userIdPatientId).andThen()
                    .denyAll()
                    .build();
        }

        // If the user is an admin, allow everything
        if (userIsAdmin) {
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

/*
*     AuthorizationInterceptor authInterceptor = new AuthorizationInterceptor(){
      @Override
      public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        return new RuleBuilder()
          .allow().read().resourcesOfType("Patient").inCompartment("Patient", new IdType("Patient/A")).andThen()
          .allow().read().resourcesOfType("Observation").inCompartment("Patient", new IdType("Patient/A")).andThen()
          .denyAll().andThen()
          .build();
      }
    };
    registerInterceptor(authInterceptor);
*
*
* */