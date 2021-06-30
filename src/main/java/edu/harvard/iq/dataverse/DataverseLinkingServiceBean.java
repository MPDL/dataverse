/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author skraffmiller
 */
@Service
@Transactional
public class DataverseLinkingServiceBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(DataverseLinkingServiceBean.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;
    
    @Autowired
    DataverseServiceBean dataverseService;
    
    
    public List<Dataverse> findLinkedDataverses(Long linkingDataverseId) {
        List<Dataverse> retList = new ArrayList<>();
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findByLinkingDataverseId", DataverseLinkingDataverse.class)
            .setParameter("linkingDataverseId", linkingDataverseId);
        for (DataverseLinkingDataverse dataverseLinkingDataverse : typedQuery.getResultList()) {
            retList.add(dataverseLinkingDataverse.getDataverse());
        }
        return retList;
    }

    public List<Dataverse> findLinkingDataverses(Long dataverseId) {
        List<Dataverse> retList = new ArrayList<>();
        TypedQuery<DataverseLinkingDataverse> typedQuery = em.createNamedQuery("DataverseLinkingDataverse.findByDataverseId", DataverseLinkingDataverse.class)
            .setParameter("dataverseId", dataverseId);
        for (DataverseLinkingDataverse dataverseLinkingDataverse : typedQuery.getResultList()) {
            retList.add(dataverseLinkingDataverse.getLinkingDataverse());
        }
        return retList;
    }
    
    public void save(DataverseLinkingDataverse dataverseLinkingDataverse) {
        if (dataverseLinkingDataverse.getId() == null) {
            em.persist(dataverseLinkingDataverse);
        } else {
            em.merge(dataverseLinkingDataverse);
        }
    }
    
    public DataverseLinkingDataverse findDataverseLinkingDataverse(Long dataverseId, Long linkingDataverseId) {
        try {
            return em.createNamedQuery("DataverseLinkingDataverse.findByDataverseIdAndLinkingDataverseId", DataverseLinkingDataverse.class)
                .setParameter("dataverseId", dataverseId)
                .setParameter("linkingDataverseId", linkingDataverseId)
                .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            logger.fine("No DataverseLinkingDataverse found for dataverseId " + dataverseId + " and linkedDataverseId " + linkingDataverseId);        
            return null;
        }
    }

    public boolean alreadyLinked(Dataverse definitionPoint, Dataverse dataverseToLinkTo) {
        return findDataverseLinkingDataverse(dataverseToLinkTo.getId(), definitionPoint.getId()) != null;
    }
}
