package edu.harvard.iq.dataverse.actionlogging;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service bean that persists {@link ActionLogRecord}s to the DB.
 * @author michael
 */
@Service
public class ActionLogServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    /**
     * Log the record. Set default values.
     * @param rec 
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log( ActionLogRecord rec ) {
        if ( rec.getEndTime() == null ) {
            rec.setEndTime( new Date() );
        }
        if ( rec.getActionResult() == null 
                && rec.getActionType() != ActionLogRecord.ActionType.Command ) {
            rec.setActionResult(ActionLogRecord.Result.OK);
        }
        em.persist(rec);
    }

    //Switches all actions from one identifier to another identifier, via native query
    //This is needed for when we change a userIdentifier or merge one account into another
    public void changeUserIdentifierInHistory(String oldIdentifier, String newIdentifier) {
        em.createNativeQuery(
                "UPDATE actionlogrecord "
                        + "SET useridentifier='"+newIdentifier+"', "
                        + "info='orig from "+oldIdentifier+" | ' || info "
                        + "WHERE useridentifier='"+oldIdentifier+"'"
        ).executeUpdate();
    }
   

    
}
