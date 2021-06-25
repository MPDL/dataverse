
package edu.harvard.iq.dataverse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;

/**
 *
 * @author skraffmi
 * Inner class that does the actual execute action on a command
 * Transaction attribute is required so that failures here cause a rollback
 * the outer engine has a transaction attribute of "SUPPORTED" 
 * so that if there are failure in the onComplete method of the command
 * the transaction will not be rolled back
 * 
 */
@Component
public class EjbDataverseEngineInner {

	/*
    @Resource
    EJBContext ejbCtxt;
*/
    @Transactional(propagation = Propagation.REQUIRED)
    public <R> R submit(Command<R> aCommand, CommandContext ctxt) throws CommandException {
        R retVal = null;
        try {
            retVal = aCommand.execute(ctxt);
        } catch (CommandException e) {
            try {
            	TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
                //ejbCtxt.setRollbackOnly();
            } catch (IllegalStateException q) {
                //If we're not in a transaction nothing to do here
            }
            throw e;
        }
        return retVal;
    }
}
