package edu.harvard.iq.dataverse.branding;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

    /**
     * This is a small helper bean 
     * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
     * dataverse service into the BrandingUtil once it's ready.
     */
    @Component
    public class BrandingUtilHelper {

        @Autowired
        DataverseServiceBean dataverseSvc;
        @Autowired SettingsServiceBean settingsSvc;
        
        @PostConstruct
        public void injectService() {
            BrandingUtil.injectServices(dataverseSvc, settingsSvc);
        }
    }