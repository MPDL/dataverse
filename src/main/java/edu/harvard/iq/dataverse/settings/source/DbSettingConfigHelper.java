package edu.harvard.iq.dataverse.settings.source;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * This is a small helper bean for the MPCONFIG DbSettingConfigSource.
 * As it is a singleton and built at application start (=deployment), it will inject the (stateless)
 * settings service into the MPCONFIG POJO once it's ready.
 *
 * MPCONFIG requires it's sources to be POJOs. No direct dependency injection possible.
 */
@Component
public class DbSettingConfigHelper {
    @Autowired
    SettingsServiceBean settingsSvc;
    
    @PostConstruct
    public void injectService() {
        DbSettingConfigSource.injectSettingsService(settingsSvc);
    }
}
