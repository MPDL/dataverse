ALTER TABLE termsofuseandaccess ADD COLUMN IF NOT EXISTS license_id BIGINT;

DO $$
BEGIN
  BEGIN
    ALTER TABLE termsofuseandaccess ADD CONSTRAINT fk_termsofuseandcesss_license_id foreign key (license_id) REFERENCES license(id);
  EXCEPTION
    WHEN duplicate_object THEN RAISE NOTICE 'Table constraint fk_termsofuseandcesss_license_id already exists';
  END;

  BEGIN
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('http://creativecommons.org/publicdomain/zero/1.0', 'CC0','You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.',true, true, '/resources/images/cc0.png');
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://creativecommons.org/licenses/by/4.0/', 'CCBY','Creative Commons: Attribution 4.0 International',true, false, '/resources/images/ccby.png');
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://creativecommons.org/licenses/by-sa/4.0/', 'CCBYSA','Creative Commons: Attribution-ShareAlike 4.0 International',true, false, '/resources/images/ccbysa.png');
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://opendatacommons.org/licenses/pddl/1-0/', 'PDDL','Open Data Commons: Public Domain Dedication and License v1.0',true, false, NULL);
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://opendatacommons.org/licenses/by/1-0/', 'ODCBY','Open Data Commons: Attribution License v1.0',true, false, NULL);
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://opendatacommons.org/licenses/odbl/1-0/', 'ODBL','Open Data Commons: Open Database License v1.0',true, false, NULL);
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://www.govdata.de/dl-de/by-2-0', 'DLGA','Data licence Germany - attribution - version 2.0',true, false, '/resources/images/dlg.png');
    INSERT INTO license (uri, name, shortdescription, active, isDefault, iconurl) VALUES
        ('https://www.govdata.de/dl-de/zero-2-0', 'DLGZ','Data licence Germany - Zero - version 2.0',true, false, '/resources/images/dlg.png');

  EXCEPTION
    WHEN unique_violation THEN RAISE NOTICE 'CC0 has already been added to the license table';
  END;

  BEGIN
    UPDATE termsofuseandaccess
    SET license_id = (SELECT license.id FROM license WHERE license.name = termsofuseandaccess.license)
    WHERE license != 'NONE'
      AND license_id IS NULL
      AND termsofuse IS null
      AND confidentialitydeclaration IS null
      AND specialpermissions IS null
      AND restrictions IS null
      AND citationrequirements IS null
      AND depositorrequirements IS null
      AND conditions IS null
      AND disclaimer IS null;

    UPDATE termsofuseandaccess
    SET termsofuse='This dataset is made available with limited information on how it can be used. You may wish to communicate with the Contact(s) specified before use.'
    WHERE license = 'NONE'
      AND license_id IS NULL
      AND termsofuse IS null
      AND NOT (confidentialitydeclaration IS null
        AND specialpermissions IS null
        AND restrictions IS null
        AND citationrequirements IS null
        AND depositorrequirements IS null
        AND conditions IS null
        AND disclaimer IS null);

    UPDATE termsofuseandaccess
    SET termsofuse='This dataset is made available without information on how it can be used. You should communicate with the Contact(s) specified before use.'
    WHERE license = 'NONE'
      AND license_id IS NULL
      AND termsofuse IS null
      AND confidentialitydeclaration IS null
      AND specialpermissions IS null
      AND restrictions IS null
      AND citationrequirements IS null
      AND depositorrequirements IS null
      AND conditions IS null
      AND disclaimer IS null;
  /*
      UPDATE termsofuseandaccess
        SET license_id = (SELECT license.id FROM license WHERE license.name = 'CC0')
        WHERE license = 'CC0'
          AND license_id IS NULL
          AND termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null;

      UPDATE termsofuseandaccess
        SET termsofuse=CONCAT('This dataset is made available under a Creative Commons CC0 license with the following additional/modified terms and conditions: ', termsofuse)
        WHERE license = 'CC0'
          AND license_id IS NULL
          AND NOT (termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null);

      UPDATE termsofuseandaccess
        SET termsofuse='This dataset is made available with limited information on how it can be used. You may wish to communicate with the Contact(s) specified before use.'
        WHERE license = 'NONE'
          AND license_id IS NULL
          AND termsofuse IS null
          AND NOT (confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null);

      UPDATE termsofuseandaccess
        SET termsofuse='This dataset is made available without information on how it can be used. You should communicate with the Contact(s) specified before use.'
        WHERE license = 'NONE'
          AND license_id IS NULL
          AND termsofuse IS null
          AND confidentialitydeclaration IS null
          AND specialpermissions IS null
          AND restrictions IS null
          AND citationrequirements IS null
          AND depositorrequirements IS null
          AND conditions IS null
          AND disclaimer IS null;
*/
  EXCEPTION
    WHEN undefined_column THEN RAISE NOTICE 'license is not in table - new instance';
  END;
  
  -- This section updates the db of those who ran the develop branch and does not make changes for new installs or upgrades from v5.9
  BEGIN
    UPDATE license SET shortdescription='Creative Commons CC0 1.0 Universal Public Domain Dedication.' WHERE name='CC0';
    UPDATE license SET iconurl='https://licensebuttons.net/p/zero/1.0/88x31.png' WHERE name='CC0';
    UPDATE license SET name='CC0 1.0' WHERE name='CC0';
    UPDATE license SET name='CC BY 4.0' WHERE name='CCBY';
    UPDATE license SET name='CC BY-SA 4.0' WHERE name='CCBYSA';
    UPDATE license SET name='PDDL 1.0' WHERE name='PDDL';
    UPDATE license SET name='ODC-By 1.0' WHERE name='ODCBY';
    UPDATE license SET name='ODbL v1.0' WHERE name='ODBL';
    UPDATE license SET name='DL-DE/BY-2-0' WHERE name='DLGA';
    UPDATE license SET name='DL-ZERO-DE/2.0' WHERE name='DLGZ';
  END;

END $$;
ALTER TABLE termsofuseandaccess DROP COLUMN IF EXISTS license;
