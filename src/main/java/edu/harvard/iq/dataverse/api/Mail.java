package edu.harvard.iq.dataverse.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import edu.harvard.iq.dataverse.MailServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;

/**
 *
 * @author xyang
 * @author Leonid Andreev
 */
@Path("mail")
public class Mail extends AbstractApiBean {
    
    @Autowired
    MailServiceBean mailService;
    
    @GET
    @Path("notifications")
    public Response sendMail() {
        ActionLogRecord alr = new ActionLogRecord(ActionLogRecord.ActionType.Admin, "sendMail");
       // mailService.bulkSendNotifications();
        actionLogSvc.log(alr);
        return ok("bulk send notification is deprecated");
    }
    
}
