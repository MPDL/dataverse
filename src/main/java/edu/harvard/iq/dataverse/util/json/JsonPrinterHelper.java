package edu.harvard.iq.dataverse.util.json;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * This is a small helper bean 
 * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
 * settings service into the OREMap once it's ready.
 */
@Component
public class JsonPrinterHelper {
    @Autowired
    SettingsServiceBean settingsSvc;
    
    @PostConstruct
    public void injectService() {
        JsonPrinter.injectSettingsService(settingsSvc);
    }
}
