package org.uniprot.api.aa.controller;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.aa.AARestApplication;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@ContextConfiguration(
        classes = {DataStoreTestConfig.class, AARestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ArbaController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ArbaGetByIdControllerIT.ArbaGetByIdParameterResolver.class,
            ArbaGetByIdControllerIT.ArbaGetByIdContentTypeParamResolver.class
        })
public class ArbaGetByIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String PATH = "/arba/{arbaid}";

    @Autowired private ArbaQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.ARBA;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.arba;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return this.repository;
    }

    @Override
    protected void saveEntry() {}

    @Override
    protected String getIdRequestPath() {
        return PATH;
    }

    static class ArbaGetByIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        protected GetIdParameter validIdParameter() {
            return null;
        }

        @Override
        protected GetIdParameter invalidIdParameter() {
            return null;
        }

        @Override
        protected GetIdParameter nonExistentIdParameter() {
            return null;
        }

        @Override
        protected GetIdParameter withFilterFieldsParameter() {
            return null;
        }

        @Override
        protected GetIdParameter withInvalidFilterParameter() {
            return null;
        }
    }

    static class ArbaGetByIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        protected GetIdContentTypeParam idSuccessContentTypesParam() {
            return null;
        }

        @Override
        protected GetIdContentTypeParam idBadRequestContentTypesParam() {
            return null;
        }
    }
}
