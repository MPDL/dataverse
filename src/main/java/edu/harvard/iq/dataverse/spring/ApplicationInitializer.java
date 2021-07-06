package edu.harvard.iq.dataverse.spring;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;

public class ApplicationInitializer implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        /*
        Environment env = event.getApplicationContext().getEnvironment();

        System.setProperty("dataverse.files.directory", env.getProperty("dataverse.files.directory"));
        System.setProperty("dataverse.files.file.type", env.getProperty("dataverse.files.file.type"));
        System.setProperty(" dataverse.files.file.label", env.getProperty(" dataverse.files.file.label"));
        System.setProperty("dataverse.files.file.directory", env.getProperty("dataverse.files.file.directory"));

        System.setProperty("dataverse.dropbox.key", env.getProperty("dataverse.dropbox.key"));
        System.setProperty("dataverse.files.hide-schema-dot-org-download-urls", env.getProperty("dataverse.files.hide-schema-dot-org-download-urls"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));
        System.setProperty("dataverse.siteUrl", env.getProperty("dataverse.siteUrl"));
        System.setProperty("dataverse.fqdn", env.getProperty("dataverse.fqdn"));
        System.setProperty("dataverse.handlenet.admprivphrase", env.getProperty("dataverse.handlenet.admprivphrase"));
        System.setProperty("dataverse.files.dcm-s3-bucket-name", env.getProperty("dataverse.files.dcm-s3-bucket-name"));
        System.setProperty("dataverse.files.s3.bucket-name", env.getProperty("dataverse.files.s3.bucket-name"));
        System.setProperty("dataverse.lang.directory", env.getProperty("dataverse.lang.directory"));

        System.setProperty("dataverse.file." + storageDriver + ".type", env.getProperty("dataverse.file." + storageDriver + ".type"));
        dataverse.files." + driverId + ".type

        System.setProperty("dataverse.test.baseurl", env.getProperty("dataverse.test.baseurl"));
        System.setProperty("checksumType", env.getProperty("checksumType"));
        System.setProperty("checksumManifest", env.getProperty("checksumManifest"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));
        System.setProperty("dataverse.handlenet.admcredfile", env.getProperty("dataverse.handlenet.admcredfile"));

*/



    }
}
