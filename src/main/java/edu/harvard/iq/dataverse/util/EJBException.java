package edu.harvard.iq.dataverse.util;

public class EJBException extends RuntimeException {

	public EJBException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public EJBException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public EJBException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public EJBException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public EJBException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
	
	public Throwable getCausedByException()
	{
		return this.getCause();
	}

}
