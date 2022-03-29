package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.servlet.ServletException;
import java.util.List;

@Import(AppProperties.class)
public class JpaRestfulServer extends BaseJpaRestfulServer {

  @Autowired
  AppProperties appProperties;

  private static final long serialVersionUID = 1L;

  public JpaRestfulServer() {
    super();
  }

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    // Add your own customization here
    PatientAndAdminAuthorizationInterceptor authInterceptor = new PatientAndAdminAuthorizationInterceptor();
    registerInterceptor(authInterceptor);

   /* AuthorizationInterceptor authInterceptor = new AuthorizationInterceptor(){
      @Override
      public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        return new RuleBuilder()
                .allow().read().resourcesOfType("Patient").inCompartment("Patient", new IdType("Patient/1603")).andThen()
                .allow().read().resourcesOfType("Observation").inCompartment("Patient", new IdType("Patient/1603")).andThen()
                .denyAll().andThen()
                .build();
      }
    };
    registerInterceptor(authInterceptor);
*/

  }

}
