package edu.harvard.iq.dataverse.spring;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebApplicationInitializer 
  implements org.springframework.web.WebApplicationInitializer {
 
    @Override
    public void onStartup(ServletContext servletContext) 
      throws ServletException {
 
        AnnotationConfigWebApplicationContext context 
          = new AnnotationConfigWebApplicationContext();
 
        servletContext.addListener(new ContextLoaderListener(context));
        servletContext.setInitParameter(
          "contextConfigLocation", "edu.harvard.iq.dataverse");
    }
}
