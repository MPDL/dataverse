package edu.harvard.iq.dataverse.api;

//import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("api/v1")
public class ApiConfiguration extends ResourceConfig {
   
   public ApiConfiguration() {
       packages("edu.harvard.iq.dataverse.api");
       packages("edu.harvard.iq.dataverse.mydata");
       register(MultiPartFeature.class);
       //register(JsonProcessingFeature.class);
   }
}
/*
public class ApiConfiguration extends ResourceConfi {
}
*/