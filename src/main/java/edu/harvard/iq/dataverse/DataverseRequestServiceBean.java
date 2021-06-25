package edu.harvard.iq.dataverse;

import javax.annotation.PostConstruct;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

/**
 * The service bean to go to when one needs the current {@link DataverseRequest}.
 * @author michael
 */
@Named
@RequestScoped
public class DataverseRequestServiceBean {
    
    @Inject
    DataverseSession dataverseSessionSvc;
    
    @Inject
    private HttpServletRequest request;
    
   private DataverseRequest dataverseRequest;
    
    @PostConstruct
    protected void setup() {
        dataverseRequest = new DataverseRequest(dataverseSessionSvc.getUser(), request);
    }
    
    public DataverseRequest getDataverseRequest() {
        return dataverseRequest;
    }
    
}
