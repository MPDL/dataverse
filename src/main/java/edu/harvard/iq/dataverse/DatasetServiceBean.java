package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDatasetStorageSizeCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.*;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import edu.harvard.iq.dataverse.workflows.WorkflowComment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.RandomStringUtils;
import org.ocpsoft.common.util.Strings;

/**
 *
 * @author skraffmiller
 */


@Stateless
@Named
public class DatasetServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;

    @EJB
    DOIEZIdServiceBean doiEZIdServiceBean;

    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    DatasetVersionServiceBean versionService;
    
    @EJB
    DvObjectServiceBean dvObjectService;
    
    @EJB
    AuthenticationServiceBean authentication;
    
    @EJB
    DataFileServiceBean fileService; 
    
    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    OAIRecordServiceBean recordService;
    
    @EJB
    EjbDataverseEngine commandEngine;
    
    @EJB
    SystemConfig systemConfig;

    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }
    
    public List<Dataset> findByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, false);
    }
    
    public List<Dataset> findPublishedByOwnerId(Long ownerId) {
        return findByOwnerId(ownerId, true);
    }    

    private List<Dataset> findByOwnerId(Long ownerId, boolean onlyPublished) {
        List<Dataset> retList = new ArrayList<>();
        TypedQuery<Dataset>  query = em.createNamedQuery("Dataset.findByOwnerId", Dataset.class);
        query.setParameter("ownerId", ownerId);
        if (!onlyPublished) {
            return query.getResultList();
        } else {
            for (Dataset ds : query.getResultList()) {
                if (ds.isReleased() && !ds.isDeaccessioned()) {
                    retList.add(ds);
                }
            }
            return retList;
        }
    }
    
    public List<Long> findIdsByOwnerId(Long ownerId) {
        return findIdsByOwnerId(ownerId, false);
    }
    
    private List<Long> findIdsByOwnerId(Long ownerId, boolean onlyPublished) {
        List<Long> retList = new ArrayList<>();
        if (!onlyPublished) {
            return em.createNamedQuery("Dataset.findIdByOwnerId")
                    .setParameter("ownerId", ownerId)
                    .getResultList();
        } else {
            List<Dataset> results = em.createNamedQuery("Dataset.findByOwnerId")
                    .setParameter("ownerId", ownerId).getResultList();
            for (Dataset ds : results) {
                if (ds.isReleased() && !ds.isDeaccessioned()) {
                    retList.add(ds.getId());
                }
            }
            return retList;
        }
    }

    public List<Dataset> findByCreatorId(Long creatorId) {
        return em.createNamedQuery("Dataset.findByCreatorId").setParameter("creatorId", creatorId).getResultList();
    }

    public List<Dataset> findByReleaseUserId(Long releaseUserId) {
        return em.createNamedQuery("Dataset.findByReleaseUserId").setParameter("releaseUserId", releaseUserId).getResultList();
    }

    public List<Dataset> filterByPidQuery(String filterQuery) {
        // finds only exact matches
        Dataset ds = findByGlobalId(filterQuery);
        List<Dataset> ret = new ArrayList<>();
        if (ds != null) ret.add(ds);

        
        /*
        List<Dataset> ret = em.createNamedQuery("Dataset.filterByPid", Dataset.class)
            .setParameter("affiliation", "%" + filterQuery.toLowerCase() + "%").getResultList();
        //logger.info("created native query: select o from Dataverse o where o.alias LIKE '" + filterQuery + "%' order by o.alias");
        logger.info("created named query");
        */
        if (ret != null) {
            logger.info("results list: "+ret.size()+" results.");
        }
        return ret;
    }
    
    public List<Dataset> findAll() {
        return em.createQuery("select object(o) from Dataset as o order by o.id", Dataset.class).getResultList();
    }
      
    public List<Long> findIdStale() {
        return em.createNamedQuery("Dataset.findIdStale").getResultList();
    }
 
     public List<Long> findIdStalePermission() {
        return em.createNamedQuery("Dataset.findIdStalePermission").getResultList();
    }
  
    public List<Long> findAllLocalDatasetIds() {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id", Long.class).getResultList();
    }
    
    public List<Long> findAllUnindexed() {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.indexTime IS null ORDER BY o.id DESC", Long.class).getResultList();
    }

    //Used in datasets listcurationstatus API
    public List<Dataset> findAllUnpublished() {
        return em.createQuery("SELECT object(o) FROM Dataset o, DvObject d WHERE d.id=o.id and d.publicationDate IS null ORDER BY o.id ASC", Dataset.class).getResultList();
    }

    /**
     * For docs, see the equivalent method on the DataverseServiceBean.
     * @param numPartitions
     * @param partitionId
     * @param skipIndexed
     * @return a list of datasets
     * @see DataverseServiceBean#findAllOrSubset(long, long, boolean)
     */     
    public List<Long> findAllOrSubset(long numPartitions, long partitionId, boolean skipIndexed) {
        if (numPartitions < 1) {
            long saneNumPartitions = 1;
            numPartitions = saneNumPartitions;
        }
        String skipClause = skipIndexed ? "AND o.indexTime is null " : "";
        TypedQuery<Long> typedQuery = em.createQuery("SELECT o.id FROM Dataset o WHERE MOD( o.id, :numPartitions) = :partitionId " +
                skipClause +
                "ORDER BY o.id", Long.class);
        typedQuery.setParameter("numPartitions", numPartitions);
        typedQuery.setParameter("partitionId", partitionId);
        return typedQuery.getResultList();
    }
    
    /**
     * Merges the passed dataset to the persistence context.
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    public Dataset merge( Dataset ds ) {
        return em.merge(ds);
    }
    
    public Dataset findByGlobalId(String globalId) {
        Dataset retVal = (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset");
        if (retVal != null){
            return retVal;
        } else {
            //try to find with alternative PID
            return (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset", true);
        }        
    }
    
    /**
     * Instantiate dataset, and its components (DatasetVersions and FileMetadatas)
     * this method is used for object validation; if there are any invalid values
     * in the dataset components, a ConstraintViolationException will be thrown,
     * which can be further parsed to detect the specific offending values.
     * @param id the id of the dataset
     * @throws javax.validation.ConstraintViolationException 
     */
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void instantiateDatasetInNewTransaction(Long id, boolean includeVariables) {
        Dataset dataset = find(id);
        for (DatasetVersion version : dataset.getVersions()) {
            for (FileMetadata fileMetadata : version.getFileMetadatas()) {
                // todo: make this optional!
                if (includeVariables) {
                    if (fileMetadata.getDataFile().isTabularData()) {
                        DataTable dataTable = fileMetadata.getDataFile().getDataTable();
                        for (DataVariable dataVariable : dataTable.getDataVariables()) {

                        }
                    }
                }
            }
        }
    }

    public String generateDatasetIdentifier(Dataset dataset, GlobalIdServiceBean idServiceBean) {
        String identifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "randomString");
        String shoulder = settingsService.getValueForKey(SettingsServiceBean.Key.Shoulder, "");
       
        switch (identifierType) {
            case "randomString":
                return generateIdentifierAsRandomString(dataset, idServiceBean, shoulder);
            case "storedProcGenerated":
                return generateIdentifierFromStoredProcedure(dataset, idServiceBean, shoulder);
            default:
                /* Should we throw an exception instead?? -- L.A. 4.6.2 */
                return generateIdentifierAsRandomString(dataset, idServiceBean, shoulder);
        }
    }
    
    private String generateIdentifierAsRandomString(Dataset dataset, GlobalIdServiceBean idServiceBean, String shoulder) {
        String identifier = null;
        do {
            identifier = shoulder + RandomStringUtils.randomAlphanumeric(6).toUpperCase();  
        } while (!isIdentifierLocallyUnique(identifier, dataset));
        
        return identifier;
    }

    private String generateIdentifierFromStoredProcedure(Dataset dataset, GlobalIdServiceBean idServiceBean, String shoulder) {
        
        String identifier; 
        do {
            StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery("Dataset.generateIdentifierFromStoredProcedure");
            query.execute();
            String identifierFromStoredProcedure = (String) query.getOutputParameterValue(1);
            // some diagnostics here maybe - is it possible to determine that it's failing 
            // because the stored procedure hasn't been created in the database?
            if (identifierFromStoredProcedure == null) {
                return null; 
            }
            identifier = shoulder + identifierFromStoredProcedure;
        } while (!isIdentifierLocallyUnique(identifier, dataset));
        
        return identifier;
    }

    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network) also check for duplicate
     * in EZID if needed
     * @param userIdentifier
     * @param dataset
     * @param persistentIdSvc
     * @return {@code true} if the identifier is unique, {@code false} otherwise.
     */
    public boolean isIdentifierUnique(String userIdentifier, Dataset dataset, GlobalIdServiceBean persistentIdSvc) {
        if ( ! isIdentifierLocallyUnique(userIdentifier, dataset) ) return false; // duplication found in local database
        
        // not in local DB, look in the persistent identifier service
        try {
            return ! persistentIdSvc.alreadyExists(dataset);
        } catch (Exception e){
            //we can live with failure - means identifier not found remotely
        }

        return true;
    }
    
    public boolean isIdentifierLocallyUnique(Dataset dataset) {
        return isIdentifierLocallyUnique(dataset.getIdentifier(), dataset);
    }
    
    public boolean isIdentifierLocallyUnique(String identifier, Dataset dataset) {
        return em.createNamedQuery("Dataset.findByIdentifierAuthorityProtocol")
            .setParameter("identifier", identifier)
            .setParameter("authority", dataset.getAuthority())
            .setParameter("protocol", dataset.getProtocol())
            .getResultList().isEmpty();
    }
    
    public Long getMaximumExistingDatafileIdentifier(Dataset dataset) {
        //Cannot rely on the largest table id having the greatest identifier counter
        long zeroFiles = new Long(0);
        Long retVal = zeroFiles;
        Long testVal;
        List<Object> idResults;
        Long dsId = dataset.getId();
        if (dsId != null) {
            try {
                idResults = em.createNamedQuery("Dataset.findIdentifierByOwnerId")
                                .setParameter("ownerId", dsId).getResultList();
            } catch (NoResultException ex) {
                logger.log(Level.FINE, "No files found in dataset id {0}. Returning a count of zero.", dsId);
                return zeroFiles;
            }
            if (idResults != null) {
                for (Object raw: idResults){
                    String identifier = (String) raw;
                    identifier =  identifier.substring(identifier.lastIndexOf("/") + 1);
                    testVal = new Long(identifier) ;
                    if (testVal > retVal){
                        retVal = testVal;
                    }               
                }
            }
        }
        return retVal;
    }

    public DatasetVersion storeVersion( DatasetVersion dsv ) {
        em.persist(dsv);
        return dsv;
    }
      

    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {

        TypedQuery<DatasetVersionUser> query = em.createNamedQuery("DatasetVersionUser.findByVersionIdAndUserId", DatasetVersionUser.class);
        query.setParameter("versionId", version.getId());
        String identifier = user.getIdentifier();
        identifier = identifier.startsWith("@") ? identifier.substring(1) : identifier;
        AuthenticatedUser au = authentication.getAuthenticatedUser(identifier);
        query.setParameter("userId", au.getId());
        try {
            return query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }

    public boolean checkDatasetLock(Long datasetId) {
        TypedQuery<DatasetLock> lockCounter = em.createNamedQuery("DatasetLock.getLocksByDatasetId", DatasetLock.class);
        lockCounter.setParameter("datasetId", datasetId);
        lockCounter.setMaxResults(1);
        List<DatasetLock> lock = lockCounter.getResultList();
        return lock.size()>0;
    }
    
    public List<DatasetLock> getDatasetLocksByUser( AuthenticatedUser user) {

        TypedQuery<DatasetLock> query = em.createNamedQuery("DatasetLock.getLocksByAuthenticatedUserId", DatasetLock.class);
        query.setParameter("authenticatedUserId", user.getId());
        try {
            return query.getResultList();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DatasetLock addDatasetLock(Dataset dataset, DatasetLock lock) {
        lock.setDataset(dataset);
        dataset.addLock(lock);
        lock.setStartTime( new Date() );
        em.persist(lock);
        //em.merge(dataset); 
        return lock;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) /*?*/
    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, Long userId, String info) {

        Dataset dataset = em.find(Dataset.class, datasetId);

        AuthenticatedUser user = null;
        if (userId != null) {
            user = em.find(AuthenticatedUser.class, userId);
        }

        // Check if the dataset is already locked for this reason:
        // (to prevent multiple, duplicate locks on the dataset!)
        DatasetLock lock = dataset.getLockFor(reason); 
        if (lock != null) {
            return lock;
        }
        
        // Create new:
        lock = new DatasetLock(reason, user);
        lock.setDataset(dataset);
        lock.setInfo(info);
        lock.setStartTime(new Date());

        if (userId != null) {
            lock.setUser(user);
            if (user.getDatasetLocks() == null) {
                user.setDatasetLocks(new ArrayList<>());
            }
            user.getDatasetLocks().add(lock);
        }

        return addDatasetLock(dataset, lock);
    }

    /**
     * Removes all {@link DatasetLock}s for the dataset whose id is passed and reason
     * is {@code aReason}.
     * @param dataset the dataset whose locks (for {@code aReason}) will be removed.
     * @param aReason The reason of the locks that will be removed.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) {
        if ( dataset != null ) {
            new HashSet<>(dataset.getLocks()).stream()
                    .filter( l -> l.getReason() == aReason )
                    .forEach( lock -> {
                        lock = em.merge(lock);
                        dataset.removeLock(lock);

                        AuthenticatedUser user = lock.getUser();
                        user.getDatasetLocks().remove(lock);

                        em.remove(lock);
                    });
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDatasetLock(DatasetLock datasetLock) {
        em.merge(datasetLock);
    }
    
    /*
    getTitleFromLatestVersion methods use native query to return a dataset title
    
        There are two versions:
     1) The version with datasetId param only will return the title regardless of version state
     2)The version with the param 'includeDraft' boolean  will return the most recently published title if the param is set to false
    If no Title found return empty string - protects against calling with
    include draft = false with no published version
    */
    
    public String getTitleFromLatestVersion(Long datasetId){
        return getTitleFromLatestVersion(datasetId, true);
    }
    
    public String getTitleFromLatestVersion(Long datasetId, boolean includeDraft){

        String whereDraft = "";
        //This clause will exclude draft versions from the select
        if (!includeDraft) {
            whereDraft = " and v.versionstate !='DRAFT' ";
        }
        
        try {
            return (String) em.createNativeQuery("select dfv.value  from dataset d "
                + " join datasetversion v on d.id = v.dataset_id "
                + " join datasetfield df on v.id = df.datasetversion_id "
                + " join datasetfieldvalue dfv on df.id = dfv.datasetfield_id "
                + " join datasetfieldtype dft on df.datasetfieldtype_id  = dft.id "
                + " where dft.name = '" + DatasetFieldConstant.title + "' and  v.dataset_id =" + datasetId
                + whereDraft
                + " order by v.versionnumber desc, v.minorVersionNumber desc limit 1 "
                + ";").getSingleResult();

        } catch (Exception ex) {
            logger.log(Level.INFO, "exception trying to get title from latest version: {0}", ex);
            return "";
        }

    }
    
    public Dataset getDatasetByHarvestInfo(Dataverse dataverse, String harvestIdentifier) {
        String queryStr = "SELECT d FROM Dataset d, DvObject o WHERE d.id = o.id AND o.owner.id = " + dataverse.getId() + " and d.harvestIdentifier = '" + harvestIdentifier + "'";
        Query query = em.createQuery(queryStr);
        List resultList = query.getResultList();
        Dataset dataset = null;
        if (resultList.size() > 1) {
            throw new EJBException("More than one dataset found in the dataverse (id= " + dataverse.getId() + "), with harvestIdentifier= " + harvestIdentifier);
        }
        if (resultList.size() == 1) {
            dataset = (Dataset) resultList.get(0);
        }
        return dataset;

    }
    
    public Long getDatasetVersionCardImage(Long versionId, User user) {
        if (versionId == null) {
            return null;
        }
        
        
        
        return null;
    }
    
    /**
     * Used to identify and properly display Harvested objects on the dataverse page.
     * 
     * @param datasetIds
     * @return 
     */
    public Map<Long, String> getArchiveDescriptionsForHarvestedDatasets(Set<Long> datasetIds){
        if (datasetIds == null || datasetIds.size() < 1) {
            return null;
        }
        
        String datasetIdStr = Strings.join(datasetIds, ", ");
        
        String qstr = "SELECT d.id, h.archiveDescription FROM harvestingClient h, dataset d WHERE d.harvestingClient_id = h.id AND d.id IN (" + datasetIdStr + ")";
        List<Object[]> searchResults;
        
        try {
            searchResults = em.createNativeQuery(qstr).getResultList();
        } catch (Exception ex) {
            searchResults = null;
        }
        
        if (searchResults == null) {
            return null;
        }
        
        Map<Long, String> ret = new HashMap<>();
        
        for (Object[] result : searchResults) {
            Long dsId;
            if (result[0] != null) {
                try {
                    dsId = (Long)result[0];
                } catch (Exception ex) {
                    dsId = null;
                }
                if (dsId == null) {
                    continue;
                }
                
                ret.put(dsId, (String)result[1]);
            }
        }
        
        return ret;        
    }
    
    
    
    public boolean isDatasetCardImageAvailable(DatasetVersion datasetVersion, User user) {        
        if (datasetVersion == null) {
            return false; 
        }
                
        // First, check if this dataset has a designated thumbnail image: 
        
        if (datasetVersion.getDataset() != null) {
            DataFile dataFile = datasetVersion.getDataset().getThumbnailFile();
            if (dataFile != null) {
                return ImageThumbConverter.isThumbnailAvailable(dataFile, 48);
            }
        }
        
        // If not, we'll try to use one of the files in this dataset version:
        // (the first file with an available thumbnail, really)
        
        List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();

        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            
            // TODO: use permissionsWrapper here - ? 
            // (we are looking up these download permissions on individual files, 
            // true, and those are unique... but the wrapper may be able to save 
            // us some queries when it determines the download permission on the
            // dataset as a whole? -- L.A. 4.2.1
            
            if (fileService.isThumbnailAvailable(dataFile) && permissionService.userOn(user, dataFile).has(Permission.DownloadFile)) { //, user)) {
                return true;
            }
 
        }
        
        return false;
    }
    
    
    // reExportAll *forces* a reexport on all published datasets; whether they 
    // have the "last export" time stamp set or not. 
    @Asynchronous 
    public void reExportAllAsync() {
        exportAllDatasets(true);
    }
    
    public void reExportAll() {
        exportAllDatasets(true);
    }
    
    
    // exportAll() will try to export the yet unexported datasets (it will honor
    // and trust the "last export" time stamp).
    
    @Asynchronous
    public void exportAllAsync() {
        exportAllDatasets(false);
    }
    
    public void exportAll() {
        exportAllDatasets(false);
    }
    
    public void exportAllDatasets(boolean forceReExport) {
        Integer countAll = 0;
        Integer countSuccess = 0;
        Integer countError = 0;
        String logTimestamp = logFormatter.format(new Date());
        Logger exportLogger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.client.DatasetServiceBean." + "ExportAll" + logTimestamp);
        String logFileName = "../logs" + File.separator + "export_" + logTimestamp + ".log";
        FileHandler fileHandler;
        boolean fileHandlerSuceeded;
        try {
            fileHandler = new FileHandler(logFileName);
            exportLogger.setUseParentHandlers(false);
            fileHandlerSuceeded = true;
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DatasetServiceBean.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        if (fileHandlerSuceeded) {
            exportLogger.addHandler(fileHandler);
        } else {
            exportLogger = logger;
        }

        exportLogger.info("Starting an export all job");

        for (Long datasetId : findAllLocalDatasetIds()) {
            // Potentially, there's a godzillion datasets in this Dataverse. 
            // This is why we go through the list of ids here, and instantiate 
            // only one dataset at a time. 
            Dataset dataset = this.find(datasetId);
            if (dataset != null) {
                // Accurate "is published?" test - ?
                // Answer: Yes, it is! We can't trust dataset.isReleased() alone; because it is a dvobject method 
                // that returns (publicationDate != null). And "publicationDate" is essentially
                // "the first publication date"; that stays the same as versions get 
                // published and/or deaccessioned. But in combination with !isDeaccessioned() 
                // it is indeed an accurate test.
                if (dataset.isReleased() && dataset.getReleasedVersion() != null && !dataset.isDeaccessioned()) {

                    // can't trust dataset.getPublicationDate(), no. 
                    Date publicationDate = dataset.getReleasedVersion().getReleaseTime(); // we know this dataset has a non-null released version! Maybe not - SEK 8/19 (We do now! :)
                    if (forceReExport || (publicationDate != null
                            && (dataset.getLastExportTime() == null
                            || dataset.getLastExportTime().before(publicationDate)))) {
                        countAll++;
                        try {
                            recordService.exportAllFormatsInNewTransaction(dataset);
                            exportLogger.info("Success exporting dataset: " + dataset.getDisplayName() + " " + dataset.getGlobalIdString());
                            countSuccess++;
                        } catch (Exception ex) {
                            exportLogger.info("Error exporting dataset: " + dataset.getDisplayName() + " " + dataset.getGlobalIdString() + "; " + ex.getMessage());
                            countError++;
                        }
                    }
                }
            }
        }
        exportLogger.info("Datasets processed: " + countAll.toString());
        exportLogger.info("Datasets exported successfully: " + countSuccess.toString());
        exportLogger.info("Datasets failures: " + countError.toString());
        exportLogger.info("Finished export-all job.");
        
        if (fileHandlerSuceeded) {
            fileHandler.close();
        }

    }
    
    //get a string to add to save success message
    //depends on dataset state and user privleges
    public String getReminderString(Dataset dataset, boolean canPublishDataset) {

        String reminderString;

        if(!dataset.isReleased() ){
            //messages for draft state.
            if (canPublishDataset){
                reminderString = BundleUtil.getStringFromBundle("dataset.message.publish.remind.draft");
            } else {
                reminderString = BundleUtil.getStringFromBundle("dataset.message.submit.remind.draft");
            }            
        } else{
            //messages for new version - post-publish
            if (canPublishDataset){
                reminderString = BundleUtil.getStringFromBundle("dataset.message.publish.remind.version");
            } else {
                reminderString = BundleUtil.getStringFromBundle("dataset.message.submit.remind.version");
            }           
        }             

        if (reminderString != null) {
            return reminderString;
        } else {
            logger.warning("Unable to get reminder string from bundle. Returning empty string.");
            return "";
        }
    }
    
    public void updateLastExportTimeStamp(Long datasetId) {
        Date now = new Date();
        em.createNativeQuery("UPDATE Dataset SET lastExportTime='"+now.toString()+"' WHERE id="+datasetId).executeUpdate();
    }

    public Dataset setNonDatasetFileAsThumbnail(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            logger.fine("In setNonDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        if (inputStream == null) {
            logger.fine("In setNonDatasetFileAsThumbnail but inputStream is null! Returning null.");
            return null;
        }
        dataset = DatasetUtil.persistDatasetLogoToStorageAndCreateThumbnails(dataset, inputStream);
        dataset.setThumbnailFile(null);
        return merge(dataset);
    }

    public Dataset setDatasetFileAsThumbnail(Dataset dataset, DataFile datasetFileThumbnailToSwitchTo) {
        if (dataset == null) {
            logger.fine("In setDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        if (datasetFileThumbnailToSwitchTo == null) {
            logger.fine("In setDatasetFileAsThumbnail but dataset is null! Returning null.");
            return null;
        }
        DatasetUtil.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset removeDatasetThumbnail(Dataset dataset) {
        if (dataset == null) {
            logger.fine("In removeDatasetThumbnail but dataset is null! Returning null.");
            return null;
        }
        DatasetUtil.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        return merge(dataset);
    }
    
    // persist assigned thumbnail in a single one-field-update query:
    // (the point is to avoid doing an em.merge() on an entire dataset object...)
    public void assignDatasetThumbnailByNativeQuery(Long datasetId, Long dataFileId) {
        try {
            em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" + dataFileId + " WHERE id=" + datasetId).executeUpdate();
        } catch (Exception ex) {
            // it's ok to just ignore... 
        }
    }
    
    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile dataFile) {
        try {
            em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" + dataFile.getId() + " WHERE id=" + dataset.getId()).executeUpdate();
        } catch (Exception ex) {
            // it's ok to just ignore... 
        }
    }

    public WorkflowComment addWorkflowComment(WorkflowComment workflowComment) {
        em.persist(workflowComment);
        return workflowComment;
    }
    
    public void markWorkflowCommentAsRead(WorkflowComment workflowComment) {
        workflowComment.setToBeShown(false);
        em.merge(workflowComment);
    }
    
    
    /**
     * This method used to throw CommandException, which was pretty pointless 
     * seeing how it's called asynchronously. As of v5.0 any CommanExceptiom 
     * thrown by the FinalizeDatasetPublicationCommand below will be caught 
     * and we'll log it as a warning - which is the best we can do at this point.
     * Any failure notifications to users should be sent from inside the command.
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void callFinalizePublishCommandAsynchronously(Long datasetId, CommandContext ctxt, DataverseRequest request, boolean isPidPrePublished) {

        // Since we are calling the next command asynchronously anyway - sleep here 
        // for a few seconds, just in case, to make sure the database update of 
        // the dataset initiated by the PublishDatasetCommand has finished, 
        // to avoid any concurrency/optimistic lock issues. 
        // Aug. 2020/v5.0: It MAY be working consistently without any 
        // sleep here, after the call the method has been moved to the onSuccess()
        // portion of the PublishDatasetCommand. I'm going to leave the 1 second
        // sleep below, for just in case reasons: -- L.A.
        try {
            Thread.sleep(1000);
        } catch (Exception ex) {
            logger.warning("Failed to sleep for a second.");
        }
        logger.fine("Running FinalizeDatasetPublicationCommand, asynchronously");
        Dataset theDataset = find(datasetId);
        try {
            commandEngine.submit(new FinalizeDatasetPublicationCommand(theDataset, request, isPidPrePublished));
        } catch (CommandException cex) {
            logger.warning("CommandException caught when executing the asynchronous portion of the Dataset Publication Command.");
        }
    }
    
    /*
     Experimental asynchronous method for requesting persistent identifiers for 
     datafiles. We decided not to run this method on upload/create (so files 
     will not have persistent ids while in draft; when the draft is published, 
     we will force obtaining persistent ids for all the files in the version. 
     
     If we go back to trying to register global ids on create, care will need to 
     be taken to make sure the asynchronous changes below are not conflicting with 
     the changes from file ingest (which may be happening in parallel, also 
     asynchronously). We would also need to lock the dataset (similarly to how 
     tabular ingest logs the dataset), to prevent the user from publishing the
     version before all the identifiers get assigned - otherwise more conflicts 
     are likely. (It sounds like it would make sense to treat these two tasks -
     persistent identifiers for files and ingest - as one post-upload job, so that 
     they can be run in sequence). -- L.A. Mar. 2018
    */
    @Asynchronous
    public void obtainPersistentIdentifiersForDatafiles(Dataset dataset) {
        GlobalIdServiceBean idServiceBean = GlobalIdServiceBean.getBean(dataset.getProtocol(), commandEngine.getContext());

        //If the Id type is sequential and Dependent then write file idenitifiers outside the command
        String datasetIdentifier = dataset.getIdentifier();
        Long maxIdentifier = null;

        if (systemConfig.isDataFilePIDSequentialDependent()) {
            maxIdentifier = getMaximumExistingDatafileIdentifier(dataset);
        }

        for (DataFile datafile : dataset.getFiles()) {
            logger.info("Obtaining persistent id for datafile id=" + datafile.getId());

            if (datafile.getIdentifier() == null || datafile.getIdentifier().isEmpty()) {

                logger.info("Obtaining persistent id for datafile id=" + datafile.getId());

                if (maxIdentifier != null) {
                    maxIdentifier++;
                    datafile.setIdentifier(datasetIdentifier + "/" + maxIdentifier.toString());
                } else {
                    datafile.setIdentifier(fileService.generateDataFileIdentifier(datafile, idServiceBean));
                }

                if (datafile.getProtocol() == null) {
                    datafile.setProtocol(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, ""));
                }
                if (datafile.getAuthority() == null) {
                    datafile.setAuthority(settingsService.getValueForKey(SettingsServiceBean.Key.Authority, ""));
                }

                logger.info("identifier: " + datafile.getIdentifier());

                String doiRetString;

                try {
                    logger.log(Level.FINE, "creating identifier");
                    doiRetString = idServiceBean.createIdentifier(datafile);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Exception while creating Identifier: " + e.getMessage(), e);
                    doiRetString = "";
                }

                // Check return value to make sure registration succeeded
                if (!idServiceBean.registerWhenPublished() && doiRetString.contains(datafile.getIdentifier())) {
                    datafile.setIdentifierRegistered(true);
                    datafile.setGlobalIdCreateTime(new Date());
                }
                
                DataFile merged = em.merge(datafile);
                merged = null; 
            }

        }
    }
    
    public long findStorageSize(Dataset dataset) throws IOException {
        return findStorageSize(dataset, false, GetDatasetStorageSizeCommand.Mode.STORAGE, null);
    }
    
    
    public long findStorageSize(Dataset dataset, boolean countCachedExtras) throws IOException {
        return findStorageSize(dataset, countCachedExtras, GetDatasetStorageSizeCommand.Mode.STORAGE, null);
    }
  
    /**
     * Returns the total byte size of the files in this dataset 
     * 
     * @param dataset
     * @param countCachedExtras boolean indicating if the cached disposable extras should also be counted
     * @param mode String indicating whether we are getting the result for storage (entire dataset) or download version based
     * @param version optional param for dataset version
     * @return total size 
     * @throws IOException if it can't access the objects via StorageIO 
     * (in practice, this can only happen when called with countCachedExtras=true; when run in the 
     * default mode, the method doesn't need to access the storage system, as the 
     * sizes of the main files are recorded in the database)
     */
    public long findStorageSize(Dataset dataset, boolean countCachedExtras, GetDatasetStorageSizeCommand.Mode mode, DatasetVersion version) throws IOException {
        long total = 0L; 
        
        if (dataset.isHarvested()) {
            return 0L;
        }

        List<DataFile> filesToTally = new ArrayList();
        
        if (version == null || (mode != null &&  mode.equals("storage"))){
            filesToTally = dataset.getFiles();
        } else {
            List <FileMetadata>  fmds = version.getFileMetadatas();
            for (FileMetadata fmd : fmds){
                    filesToTally.add(fmd.getDataFile());
            }           
        }
    
        
        //CACHED EXTRAS FOR DOWNLOAD?
        
        
        for (DataFile datafile : filesToTally) {
                total += datafile.getFilesize();

                if (!countCachedExtras) {
                    if (datafile.isTabularData()) {
                        // count the size of the stored original, in addition to the main tab-delimited file:
                        Long originalFileSize = datafile.getDataTable().getOriginalFileSize();
                        if (originalFileSize != null) {
                            total += originalFileSize;
                        }
                    }
                } else {
                    StorageIO<DataFile> storageIO = datafile.getStorageIO();
                    for (String cachedFileTag : storageIO.listAuxObjects()) {
                        total += storageIO.getAuxObjectSize(cachedFileTag);
                    }
                }
            }
        
        // and finally,
        if (countCachedExtras) {
            // count the sizes of the files cached for the dataset itself
            // (i.e., the metadata exports):
            StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);
            
            for (String[] exportProvider : ExportService.getInstance().getExportersLabels()) {
                String exportLabel = "export_" + exportProvider[1] + ".cached";
                try {
                    total += datasetSIO.getAuxObjectSize(exportLabel);
                } catch (IOException ioex) {
                    // safe to ignore; object not cached
                }
            }
        }
        
        return total; 
    }
    
    /**
     * An optimized method for deleting a harvested dataset. 
     * 
     * @param dataset
     * @param request DataverseRequest (for initializing the DestroyDatasetCommand)
     * @param hdLogger logger object (in practice, this will be a separate log file created for a specific harvesting job)
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteHarvestedDataset(Dataset dataset, DataverseRequest request, Logger hdLogger) {
        // Purge all the SOLR documents associated with this client from the 
        // index server: 
        indexService.deleteHarvestedDocuments(dataset);
        
        try {
            // files from harvested datasets are removed unceremoniously, 
            // directly in the database. no need to bother calling the 
            // DeleteFileCommand on them.
            for (DataFile harvestedFile : dataset.getFiles()) {
                DataFile merged = em.merge(harvestedFile);
                em.remove(merged);
                harvestedFile = null; 
            }
            dataset.setFiles(null);
            Dataset merged = em.merge(dataset);
            commandEngine.submit(new DestroyDatasetCommand(merged, request));
            hdLogger.info("Successfully destroyed the dataset");
        } catch (Exception ex) {
            hdLogger.warning("Failed to destroy the dataset");
        } 
    }


    public boolean isMinorUpdate(DatasetVersion datasetVersion) {
        if (this.dataset.getLatestVersion().isWorkingCopy()) {
            if (this.dataset.getVersions().size() > 1 && this.dataset.getVersions().get(1) != null) {
                if (this.dataset.getVersions().get(1).isDeaccessioned()) {
                    return false;
                }
            }
        }
        if (this.getDataset().getReleasedVersion() != null) {
            if (this.getFileMetadatas().size() != this.getDataset().getReleasedVersion().getFileMetadatas().size()){
                return false;
            } else {
                List <DataFile> current = new ArrayList<>();
                List <DataFile> previous = new ArrayList<>();
                for (FileMetadata fmdc : this.getFileMetadatas()){
                    current.add(fmdc.getDataFile());
                }
                for (FileMetadata fmdc : this.getDataset().getReleasedVersion().getFileMetadatas()){
                    previous.add(fmdc.getDataFile());
                }
                for (DataFile fmd: current){
                    previous.remove(fmd);
                }
                return previous.isEmpty();
            }
        }
        return true;
    }

    public boolean isHasPackageFile(){
        if (this.fileMetadatas.isEmpty()){
            return false;
        }
        if(this.fileMetadatas.size() > 1){
            return false;
        }
        return this.fileMetadatas.get(0).getDataFile().getContentType().equals(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
    }

    public boolean isHasNonPackageFile(){
        if (this.fileMetadatas.isEmpty()){
            return false;
        }
        // The presence of any non-package file means that HTTP Upload was used (no mixing allowed) so we just check the first file.
        return !this.fileMetadatas.get(0).getDataFile().getContentType().equals(DataFileServiceBean.MIME_TYPE_PACKAGE_FILE);
    }

    public DatasetVersion cloneDatasetVersion(DatasetVersion oldVersion){
        DatasetVersion dsv = new DatasetVersion();
        dsv.setVersionState(this.getPriorVersionState());
        dsv.setFileMetadatas(new ArrayList<>());

        if (this.getUNF() != null){
            dsv.setUNF(this.getUNF());
        }

        if (this.getDatasetFields() != null && !this.getDatasetFields().isEmpty()) {
            dsv.setDatasetFields(dsv.copyDatasetFields(this.getDatasetFields()));
        }

        if (this.getTermsOfUseAndAccess()!= null){
            dsv.setTermsOfUseAndAccess(this.getTermsOfUseAndAccess().copyTermsOfUseAndAccess());
        } else {
            TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
            terms.setDatasetVersion(dsv);
            terms.setLicense(TermsOfUseAndAccess.License.CC0);
            dsv.setTermsOfUseAndAccess(terms);
        }

        for (FileMetadata fm : this.getFileMetadatas()) {
            FileMetadata newFm = new FileMetadata();
            // TODO:
            // the "category" will be removed, shortly.
            // (replaced by multiple, tag-like categories of
            // type DataFileCategory) -- L.A. beta 10
            //newFm.setCategory(fm.getCategory());
            // yep, these are the new categories:
            newFm.setCategories(fm.getCategories());
            newFm.setDescription(fm.getDescription());
            newFm.setLabel(fm.getLabel());
            newFm.setDirectoryLabel(fm.getDirectoryLabel());
            newFm.setRestricted(fm.isRestricted());
            newFm.setDataFile(fm.getDataFile());
            newFm.setDatasetVersion(dsv);
            newFm.setProvFreeForm(fm.getProvFreeForm());

            dsv.getFileMetadatas().add(newFm);
        }




        dsv.setDataset(this.getDataset());
        return dsv;

    }

    // TODO: Consider moving this comment into the Exporter code.
    // The export subsystem assumes there is only
    // one metadata export in a given format per dataset (it uses the current
    // released (published) version. This JSON fragment is generated for a
    // specific released version - and we can have multiple released versions.
    // So something will need to be modified to accommodate this. -- L.A.
    /**
     * We call the export format "Schema.org JSON-LD" and extensive Javadoc can
     * be found in {@link SchemaDotOrgExporter}.
     */
    public String getJsonLd() {
        // We show published datasets only for "datePublished" field below.
        if (!this.isPublished()) {
            return "";
        }

        if (jsonLd != null) {
            return jsonLd;
        }
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("@context", "http://schema.org");
        job.add("@type", "Dataset");
        // Note that whenever you use "@id" you should also use "identifier" and vice versa.
        job.add("@id", this.getDataset().getPersistentURL());
        job.add("identifier", this.getDataset().getPersistentURL());
        job.add("name", this.getTitle());
        JsonArrayBuilder authors = Json.createArrayBuilder();
        for (DatasetAuthor datasetAuthor : this.getDatasetAuthors()) {
            JsonObjectBuilder author = Json.createObjectBuilder();
            String name = datasetAuthor.getName().getDisplayValue();
            DatasetField authorAffiliation = datasetAuthor.getAffiliation();
            String affiliation = null;
            if (authorAffiliation != null) {
                affiliation = datasetAuthor.getAffiliation().getDisplayValue();
            }
            // We are aware of "givenName" and "familyName" but instead of a person it might be an organization such as "Gallup Organization".
            //author.add("@type", "Person");
            author.add("name", name);
            // We are aware that the following error is thrown by https://search.google.com/structured-data/testing-tool
            // "The property affiliation is not recognized by Google for an object of type Thing."
            // Someone at Google has said this is ok.
            // This logic could be moved into the `if (authorAffiliation != null)` block above.
            if (!StringUtil.isEmpty(affiliation)) {
                author.add("affiliation", affiliation);
            }
            String identifierAsUrl = datasetAuthor.getIdentifierAsUrl();
            if (identifierAsUrl != null) {
                // It would be valid to provide an array of identifiers for authors but we have decided to only provide one.
                author.add("@id", identifierAsUrl);
                author.add("identifier", identifierAsUrl);
            }
            authors.add(author);
        }
        JsonArray authorsArray = authors.build();
        /**
         * "creator" is being added along side "author" (below) as an
         * experiment. We think Google Dataset Search might like "creator"
         * better".
         */
        job.add("creator", authorsArray);
        /**
         * "author" is still here for backward compatibility. Depending on how
         * the "creator" experiment above goes, we may deprecate it in the
         * future.
         */
        job.add("author", authorsArray);
        /**
         * We are aware that there is a "datePublished" field but it means "Date
         * of first broadcast/publication." This only makes sense for a 1.0
         * version.
         *
         * TODO: Should we remove the comment above about a 1.0 version? We
         * included this "datePublished" field in Dataverse 4.8.4.
         */
        String datePublished = this.getDataset().getPublicationDateFormattedYYYYMMDD();
        if (datePublished != null) {
            job.add("datePublished", datePublished);
        }

        /**
         * "dateModified" is more appropriate for a version: "The date on which
         * the CreativeWork was most recently modified or when the item's entry
         * was modified within a DataFeed."
         */
        job.add("dateModified", this.getPublicationDateAsString());
        job.add("version", this.getVersionNumber().toString());

        JsonArrayBuilder descriptionsArray = Json.createArrayBuilder();
        List<String> descriptions = this.getDescriptionsPlainText();
        for (String description : descriptions) {
            descriptionsArray.add(description);
        }
        /**
         * In Dataverse 4.8.4 "description" was a single string but now it's an
         * array.
         */
        job.add("description", descriptionsArray);

        /**
         * "keywords" - contains subject(s), datasetkeyword(s) and topicclassification(s)
         * metadata fields for the version. -- L.A.
         * (see #2243 for details/discussion/feedback from Google)
         */
        JsonArrayBuilder keywords = Json.createArrayBuilder();

        for (String subject : this.getDatasetSubjects()) {
            keywords.add(subject);
        }

        for (String topic : this.getTopicClassifications()) {
            keywords.add(topic);
        }

        for (String keyword : this.getKeywords()) {
            keywords.add(keyword);
        }

        job.add("keywords", keywords);

        /**
         * citation: (multiple) related publication citation and URLs, if
         * present.
         *
         * In Dataverse 4.8.4 "citation" was an array of strings but now it's an
         * array of objects.
         */
        List<DatasetRelPublication> relatedPublications = getRelatedPublications();
        if (!relatedPublications.isEmpty()) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for (DatasetRelPublication relatedPub : relatedPublications) {
                boolean addToArray = false;
                String pubCitation = relatedPub.getText();
                String pubUrl = relatedPub.getUrl();
                if (pubCitation != null || pubUrl != null) {
                    addToArray = true;
                }
                JsonObjectBuilder citationEntry = Json.createObjectBuilder();
                citationEntry.add("@type", "CreativeWork");
                if (pubCitation != null) {
                    citationEntry.add("text", pubCitation);
                }
                if (pubUrl != null) {
                    citationEntry.add("@id", pubUrl);
                    citationEntry.add("identifier", pubUrl);
                }
                if (addToArray) {
                    jsonArrayBuilder.add(citationEntry);
                }
            }
            JsonArray jsonArray = jsonArrayBuilder.build();
            if (!jsonArray.isEmpty()) {
                job.add("citation", jsonArray);
            }
        }

        /**
         * temporalCoverage:
         * (if available)
         */

        List<String> timePeriodsCovered = this.getTimePeriodsCovered();
        if (timePeriodsCovered.size() > 0) {
            JsonArrayBuilder temporalCoverage = Json.createArrayBuilder();
            for (String timePeriod : timePeriodsCovered) {
                temporalCoverage.add(timePeriod);
            }
            job.add("temporalCoverage", temporalCoverage);
        }

        /**
         * https://schema.org/version/3.4/ says, "Note that schema.org release
         * numbers are not generally included when you use schema.org. In
         * contexts (e.g. related standards work) when a particular release
         * needs to be cited, this document provides the appropriate URL."
         *
         * For the reason above we decided to take out schemaVersion but we're
         * leaving this Javadoc in here to remind us that we made this decision.
         * We used to include "https://schema.org/version/3.3" in the output for
         * "schemaVersion".
         */
        TermsOfUseAndAccess terms = this.getTermsOfUseAndAccess();
        if (terms != null) {
            JsonObjectBuilder license = Json.createObjectBuilder().add("@type", "Dataset");

            if (TermsOfUseAndAccess.License.CC0.equals(terms.getLicense())) {
                license.add("text", "CC0").add("url", TermsOfUseAndAccess.CC0_URI);
            } else {
                String termsOfUse = terms.getTermsOfUse();
                // Terms of use can be null if you create the dataset with JSON.
                if (termsOfUse != null) {
                    license.add("text", termsOfUse);
                }
            }

            job.add("license",license);
        }

        job.add("includedInDataCatalog", Json.createObjectBuilder()
                .add("@type", "DataCatalog")
                .add("name", BrandingUtil.getRootDataverseCollectionName())
                .add("url", SystemConfig.getDataverseSiteUrlStatic())
        );

        String installationBrandName = BrandingUtil.getInstallationBrandName();
        /**
         * Both "publisher" and "provider" are included but they have the same
         * values. Some services seem to prefer one over the other.
         */
        job.add("publisher", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );
        job.add("provider", Json.createObjectBuilder()
                .add("@type", "Organization")
                .add("name", installationBrandName)
        );

        List<String> funderNames = getFunders();
        if (!funderNames.isEmpty()) {
            JsonArrayBuilder funderArray = Json.createArrayBuilder();
            for (String funderName : funderNames) {
                JsonObjectBuilder funder = NullSafeJsonBuilder.jsonObjectBuilder();
                funder.add("@type", "Organization");
                funder.add("name", funderName);
                funderArray.add(funder);
            }
            job.add("funder", funderArray);
        }

        boolean commaSeparated = true;
        List<String> spatialCoverages = getSpatialCoverages(commaSeparated);
        if (!spatialCoverages.isEmpty()) {
            JsonArrayBuilder spatialArray = Json.createArrayBuilder();
            for (String spatialCoverage : spatialCoverages) {
                spatialArray.add(spatialCoverage);
            }
            job.add("spatialCoverage", spatialArray);
        }

        List<FileMetadata> fileMetadatasSorted = getFileMetadatasSorted();
        if (fileMetadatasSorted != null && !fileMetadatasSorted.isEmpty()) {
            JsonArrayBuilder fileArray = Json.createArrayBuilder();
            String dataverseSiteUrl = SystemConfig.getDataverseSiteUrlStatic();
            for (FileMetadata fileMetadata : fileMetadatasSorted) {
                JsonObjectBuilder fileObject = NullSafeJsonBuilder.jsonObjectBuilder();
                String filePidUrlAsString = null;
                URL filePidUrl = fileMetadata.getDataFile().getGlobalId().toURL();
                if (filePidUrl != null) {
                    filePidUrlAsString = filePidUrl.toString();
                }
                fileObject.add("@type", "DataDownload");
                fileObject.add("name", fileMetadata.getLabel());
                fileObject.add("fileFormat", fileMetadata.getDataFile().getContentType());
                fileObject.add("contentSize", fileMetadata.getDataFile().getFilesize());
                fileObject.add("description", fileMetadata.getDescription());
                fileObject.add("@id", filePidUrlAsString);
                fileObject.add("identifier", filePidUrlAsString);
                String hideFilesBoolean = System.getProperty(SystemConfig.FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS);
                if (hideFilesBoolean != null && hideFilesBoolean.equals("true")) {
                    // no-op
                } else {
                    if (FileUtil.isPubliclyDownloadable(fileMetadata)) {
                        String nullDownloadType = null;
                        fileObject.add("contentUrl", dataverseSiteUrl + FileUtil.getFileDownloadUrlPath(nullDownloadType, fileMetadata.getDataFile().getId(), false, fileMetadata.getId()));
                    }
                }
                fileArray.add(fileObject);
            }
            job.add("distribution", fileArray);
        }
        jsonLd = job.build().toString();

        //Most fields above should be stripped/sanitized but, since this is output in the dataset page as header metadata, do a final sanitize step to make sure
        jsonLd = MarkupChecker.stripAllTags(jsonLd);

        return jsonLd;
    }



    public Set<ConstraintViolation> validate(DatasetVersion datasetVersion) {
        Set<ConstraintViolation> returnSet = new HashSet<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        for (DatasetField dsf : this.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                dsf.setValidationMessage(constraintViolation.getMessage());
                returnSet.add(constraintViolation);
                break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            for (DatasetFieldValue dsfv : dsf.getDatasetFieldValues()) {
                dsfv.setValidationMessage(null); // clear out any existing validation message
                Set<ConstraintViolation<DatasetFieldValue>> constraintViolations2 = validator.validate(dsfv);
                for (ConstraintViolation<DatasetFieldValue> constraintViolation : constraintViolations2) {
                    dsfv.setValidationMessage(constraintViolation.getMessage());
                    returnSet.add(constraintViolation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation
                }
            }
        }
        List<FileMetadata> dsvfileMetadatas = this.getFileMetadatas();
        if (dsvfileMetadatas != null) {
            for (FileMetadata fileMetadata : dsvfileMetadatas) {
                Set<ConstraintViolation<FileMetadata>> constraintViolations = validator.validate(fileMetadata);
                if (constraintViolations.size() > 0) {
                    // currently only support one message
                    ConstraintViolation<FileMetadata> violation = constraintViolations.iterator().next();
                    /**
                     * @todo How can we expose this more detailed message
                     * containing the invalid value to the user?
                     */
                    String message = "Constraint violation found in FileMetadata. "
                            + violation.getMessage() + " "
                            + "The invalid value is \"" + violation.getInvalidValue().toString() + "\".";
                    logger.info(message);
                    returnSet.add(violation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation
                }
            }
        }

        return returnSet;
    }

    public String getRemoteArchiveURL(Dataset dataset) {
        if (isHarvested()) {
            if (HarvestingClient.HARVEST_STYLE_DATAVERSE.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/dataset.xhtml?persistentId=" + getGlobalIdString();
            } else if (HarvestingClient.HARVEST_STYLE_VDC.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String rootArchiveUrl = this.getHarvestedFrom().getHarvestingUrl();
                int c = rootArchiveUrl.indexOf("/OAIHandler");
                if (c > 0) {
                    rootArchiveUrl = rootArchiveUrl.substring(0, c);
                    return rootArchiveUrl + "/faces/study/StudyPage.xhtml?globalId=" + getGlobalIdString();
                }
            } else if (HarvestingClient.HARVEST_STYLE_ICPSR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // For the ICPSR, it turns out that the best thing to do is to
                // rely on the DOI to send the user to the right landing page for
                // the study:
                //String icpsrId = identifier;
                //return this.getOwner().getHarvestingClient().getArchiveUrl() + "/icpsrweb/ICPSR/studies/"+icpsrId+"?q="+icpsrId+"&amp;searchSource=icpsr-landing";
                return "http://doi.org/" + this.getAuthority() + "/" + this.getIdentifier();
            } else if (HarvestingClient.HARVEST_STYLE_NESSTAR.equals(this.getHarvestedFrom().getHarvestStyle())) {
                String nServerURL = this.getHarvestedFrom().getArchiveUrl();
                // chop any trailing slashes in the server URL - or they will result
                // in multiple slashes in the final URL pointing to the study
                // on server of origin; Nesstar doesn't like it, apparently.
                nServerURL = nServerURL.replaceAll("/*$", "");

                String nServerURLencoded = nServerURL;

                nServerURLencoded = nServerURLencoded.replace(":", "%3A").replace("/", "%2F");
                //SEK 09/13/18
                String NesstarWebviewPage = nServerURL
                        + "/webview/?mode=documentation&submode=abstract&studydoc="
                        + nServerURLencoded + "%2Fobj%2FfStudy%2F"
                        + this.getIdentifier()
                        + "&top=yes";

                return NesstarWebviewPage;
            } else if (HarvestingClient.HARVEST_STYLE_ROPER.equals(this.getHarvestedFrom().getHarvestStyle())) {
                return this.getHarvestedFrom().getArchiveUrl() + "/CFIDE/cf/action/catalog/abstract.cfm?archno=" + this.getIdentifier();
            } else if (HarvestingClient.HARVEST_STYLE_HGL.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // a bit of a hack, true.
                // HGL documents, when turned into Dataverse studies/datasets
                // all 1 datafile; the location ("storage identifier") of the file
                // is the URL pointing back to the HGL GUI viewer. This is what
                // we will display for the dataset URL.  -- L.A.
                // TODO: create a 4.+ ticket for a cleaner solution.
                List<DataFile> dataFiles = this.getFiles();
                if (dataFiles != null && dataFiles.size() == 1) {
                    if (dataFiles.get(0) != null) {
                        String hglUrl = dataFiles.get(0).getStorageIdentifier();
                        if (hglUrl != null && hglUrl.matches("^http.*")) {
                            return hglUrl;
                        }
                    }
                }
                return this.getHarvestedFrom().getArchiveUrl();
            } else if (HarvestingClient.HARVEST_STYLE_DEFAULT.equals(this.getHarvestedFrom().getHarvestStyle())) {
                // This is a generic OAI archive.
                // The metadata we harvested for this dataset is most likely a
                // simple DC record that does not contain a URL pointing back at
                // the specific location on the source archive. But, it probably
                // has a global identifier, a DOI or a Handle - so we should be
                // able to redirect to the proper global resolver.
                // But since this is a harvested dataset, we will assume that
                // there is a possibility tha this object does NOT have all the
                // valid persistent identifier components.

                if (StringUtil.nonEmpty(this.getProtocol())
                        && StringUtil.nonEmpty(this.getAuthority())
                        && StringUtil.nonEmpty(this.getIdentifier())) {
                    return this.getPersistentURL();
                }

                // All we can do is redirect them to the top-level URL we have
                // on file for this remote archive:
                return this.getHarvestedFrom().getArchiveUrl();
            } else {
                // This is really not supposed to happen - this is a harvested
                // dataset for which we don't have ANY information on the nature
                // of the archive we got it from. So all we can do is redirect
                // the user to the top-level URL we have on file for this remote
                // archive:
                return this.getHarvestedFrom().getArchiveUrl();
            }
        }

        return null;
    }

}
