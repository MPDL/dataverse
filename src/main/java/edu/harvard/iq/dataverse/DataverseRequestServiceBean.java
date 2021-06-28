package edu.harvard.iq.dataverse;

import javax.annotation.PostConstruct;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.google.api.Http;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The service bean to go to when one needs the current {@link DataverseRequest}.
 * @author michael
 */
@Service
//@RequestScoped
public class DataverseRequestServiceBean {
    
    @Inject
    DataverseSession dataverseSessionSvc;

    /*
    @Inject
    private HttpServletRequest request;
    
   private DataverseRequest dataverseRequest;
   */

    /*
    @PostConstruct
    protected void setup() {
        dataverseRequest = new DataverseRequest(dataverseSessionSvc.getUser(), request);
    }
    */
    public DataverseRequest getDataverseRequest() {

        HttpServletRequest request =
                ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes())
                        .getRequest();
        return new DataverseRequest(dataverseSessionSvc.getUser(), request);
    }
    
}
