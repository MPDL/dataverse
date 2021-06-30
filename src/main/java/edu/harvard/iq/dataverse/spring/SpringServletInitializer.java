package edu.harvard.iq.dataverse.spring;

import edu.harvard.iq.dataverse.CitationServlet;
import edu.harvard.iq.dataverse.HomepageServlet;
import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import edu.harvard.iq.dataverse.api.ApiRouter;
import edu.harvard.iq.dataverse.api.datadeposit.*;
import edu.harvard.iq.dataverse.harvest.server.web.servlet.OAIServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.faces.webapp.FacesServlet;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.EnumSet;

@Configuration
public class SpringServletInitializer
  implements ServletContextInitializer {

    @Bean
    public ApiRouter apiRouter(){
        return new ApiRouter();
    }

    @Bean
    public ApiBlockingFilter apiBlockingFilter(){
        return new ApiBlockingFilter();
    }

    @Bean
    public FilterRegistrationBean<ApiRouter> apiRouterFilterRegistration(){
        FilterRegistrationBean<ApiRouter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(apiRouter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ApiBlockingFilter> apiBlockingFilterRegistration(){
        FilterRegistrationBean<ApiBlockingFilter> registrationBean
                = new FilterRegistrationBean<>();

        registrationBean.setFilter(apiBlockingFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

        return registrationBean;
    }

    @Bean
    OAIServlet oaiServlet()
    {
        return new OAIServlet();
    }

    @Bean
    CitationServlet citationServlet()
    {
        return new CitationServlet();
    }

    @Bean
    HomepageServlet homepageServlet()
    {
        return new HomepageServlet();
    }


    @Override
    public void onStartup(ServletContext servletContext) 
      throws ServletException {
        /*
        AnnotationConfigWebApplicationContext context 
          = new AnnotationConfigWebApplicationContext();
 
        servletContext.addListener(new ContextLoaderListener(context));

         */

        servletContext.setInitParameter(
          "contextConfigLocation", "edu.harvard.iq.dataverse");

        /*
        servletContext.addFilter("Router", apiRouterFilter())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD),false, "/api/*");
        servletContext.addFilter("Blocker", apiBlockingFilter())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD),false, "/api/*");
        */
        ServletRegistration.Dynamic reg = servletContext.addServlet("Faces Servlet", FacesServlet.class);
                reg.addMapping("/faces/*", "*.faces", "*.jsf", "*.xhtml");
                reg.setLoadOnStartup(1);
        //servletContext.addServlet("Push Servlet", PushServlet.class).addMapping("/primepush/*");
        servletContext.addServlet("OAI Servlet", oaiServlet()).addMapping("/oai");
        servletContext.addServlet("Citation Servlet",citationServlet()).addMapping("/citation");
        servletContext.addServlet("Homepage Servlet", homepageServlet()).addMapping("/Homepage");

        servletContext.setInitParameter("org.jboss.weld.context.conversation.lazy", "false");
        servletContext.setInitParameter("javax.faces.PROJECT_STAGE", "Production");
        servletContext.setInitParameter("primefaces.THEME", "bootstrap");
        servletContext.setInitParameter("javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL", "true");
        servletContext.setInitParameter("javax.faces.FACELETS_SKIP_COMMENTS", "true");
        servletContext.setInitParameter("javax.faces.FACELETS_BUFFER_SIZE", "102400");
        servletContext.setInitParameter("javax.faces.FACELETS_REFRESH_PERIOD", "-1");
        servletContext.setInitParameter("com.sun.faces.forceLoadConfiguration", Boolean.TRUE.toString());
        servletContext.setInitParameter("config-impl", "edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl");
        servletContext.setInitParameter("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2ServiceDocumentServlet", "edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl");
        servletContext.setInitParameter("config-impl", "edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl");


        /*
        servletContext.addServlet("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2ServiceDocumentServlet", SWORDv2ServiceDocumentServlet.class).addMapping("/dvn/api/data-deposit/v1/swordv2/service-document/*","/dvn/api/data-deposit/v1.1/swordv2/service-document/*");


        servletContext.setInitParameter("collection-deposit-impl", "edu.harvard.iq.dataverse.api.datadeposit.CollectionDepositManagerImpl");
        servletContext.setInitParameter("collection-list-impl", "edu.harvard.iq.dataverse.api.datadeposit.CollectionListManagerImpl");
        servletContext.addServlet("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2CollectionServlet", SWORDv2CollectionServlet.class).addMapping("/dvn/api/data-deposit/v1/swordv2/collection/*","/dvn/api/data-deposit/v1.1/swordv2/collection/*");


        servletContext.setInitParameter("media-resource-impl", "edu.harvard.iq.dataverse.api.datadeposit.MediaResourceManagerImpl");
        servletContext.addServlet("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2MediaResourceServlet", SWORDv2MediaResourceServlet.class).addMapping("/dvn/api/data-deposit/v1/swordv2/edit-media/*","/dvn/api/data-deposit/v1.1/swordv2/edit-media/*");

        servletContext.setInitParameter("statement-impl", "edu.harvard.iq.dataverse.api.datadeposit.StatementManagerImpl");
        servletContext.addServlet("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2StatementServlet", SWORDv2StatementServlet.class).addMapping("/dvn/api/data-deposit/v1/swordv2/statement/*","/dvn/api/data-deposit/v1.1/swordv2/statement/*");

        servletContext.setInitParameter("container-impl", "edu.harvard.iq.dataverse.api.datadeposit.ContainerManagerImpl");
        servletContext.addServlet("edu.harvard.iq.dataverse.api.datadeposit.SWORDv2ContainerServlet", SWORDv2ContainerServlet.class).addMapping("/dvn/api/data-deposit/v1/swordv2/edit/*","/dvn/api/data-deposit/v1.1/swordv2/edit/*");

        */

    }
}
