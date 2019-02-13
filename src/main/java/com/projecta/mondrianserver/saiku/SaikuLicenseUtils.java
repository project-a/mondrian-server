package com.projecta.mondrianserver.saiku;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.saiku.service.datasource.IDatasourceManager;
import org.saiku.service.license.ILicenseUtils;

import com.google.common.collect.ImmutableMap;

import bi.meteorite.license.LicenseException;
import bi.meteorite.license.SaikuLicense;

/**
 * Dummy implementation of ILicenseUtils
 */
public class SaikuLicenseUtils implements ILicenseUtils {

    @Override
    public IDatasourceManager getRepositoryDatasourceManager() {
        return null;
    }

    @Override
    public void setRepositoryDatasourceManager(IDatasourceManager repositoryDatasourceManager) {
    }

    @Override
    public void setLicense(SaikuLicense lic) throws IOException {
    }

    @Override
    public void setLicense(String lic) {
    }

    @Override
    public Object getLicense() throws IOException, ClassNotFoundException, RepositoryException {
        return ImmutableMap.of("licenseType", "");
    }

    @Override
    public SaikuLicense getLicenseNo64() throws IOException, ClassNotFoundException, RepositoryException {
        return new SaikuLicense();
    }

    @Override
    public void validateLicense() throws LicenseException, RepositoryException, IOException, ClassNotFoundException {
    }

    @Override
    public void setAdminuser(String adminuser) {
    }

    @Override
    public String getAdminuser() {
        return "admin";
    }

}
