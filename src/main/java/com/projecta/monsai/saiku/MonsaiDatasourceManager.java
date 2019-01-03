package com.projecta.monsai.saiku;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.saiku.database.dto.MondrianSchema;
import org.saiku.datasources.connection.RepositoryFile;
import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.datasources.datasource.SaikuDatasource.Type;
import org.saiku.repository.AclEntry;
import org.saiku.repository.AclMethod;
import org.saiku.repository.IRepositoryObject;
import org.saiku.repository.RepositoryFileObject;
import org.saiku.repository.RepositoryFolderObject;
import org.saiku.service.datasource.IDatasourceManager;
import org.saiku.service.importer.JujuSource;
import org.saiku.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.projecta.monsai.config.Config;

/**
 * Stripped-down implementation of IDatasourceManager that avoids the use of any
 * external repository and stores saved queries in the configured directory
 */
public class MonsaiDatasourceManager implements IDatasourceManager {

    @Autowired private Config config;

    private String storageDir;

    private static final List<AclMethod> ACCESS_READ = ImmutableList.of(AclMethod.READ);
    private static final List<AclMethod> ACCESS_ALL  = ImmutableList.of(AclMethod.READ, AclMethod.WRITE);


    @PostConstruct
    public void init() throws Exception {
        storageDir = config.getProperty("saikuStorageDir", ".");
        new File(storageDir).mkdirs();
    }


    @Override
    public void load() {
    }

    @Override
    public void unload() {
    }

    @Override
    public SaikuDatasource addDatasource(SaikuDatasource datasource) throws Exception {
        return datasource;
    }


    @Override
    public SaikuDatasource setDatasource(SaikuDatasource datasource) throws Exception {
        return datasource;
    }


    @Override
    public List<SaikuDatasource> addDatasources(List<SaikuDatasource> datasources) {
        return datasources;
    }

    @Override
    public boolean removeDatasource(String datasourceName) {
        return false;
    }

    @Override
    public boolean removeSchema(String schemaName) {
        return false;
    }


    /**
     * Returns only the preconfigured data source
     */
    @Override
    public Map<String, SaikuDatasource> getDatasources() {

        String dataSourceName = config.getProperty("dataSourceName", "Cubes");
        SaikuDatasource datasource = new SaikuDatasource(dataSourceName, Type.OLAP, new Properties());
        return ImmutableMap.of(dataSourceName, datasource);
    }


    @Override
    public SaikuDatasource getDatasource(String datasourceName) {
        return getDatasources().get(datasourceName);
    }


    @Override
    public SaikuDatasource getDatasource(String datasourceName, boolean refresh) {
        return getDatasources().get(datasourceName);
    }


    @Override
    public void addSchema(String file, String path, String name) throws Exception {
    }


    @Override
    public List<MondrianSchema> getMondrianSchema() {
        return Collections.emptyList();
    }

    @Override
    public MondrianSchema getMondrianSchema(String catalog) {
        return null;
    }

    @Override
    public RepositoryFile getFile(String file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFileData(String file, String username, List<String> roles) {
        return readFile(file);
    }


    @Override
    public String getInternalFileData(String file) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getBinaryInternalFileData(String file) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String saveFile(String path, Object content, String user, List<String> roles) {
        saveFile(path, content);
        return "Save Okay";
    }

    @Override
    public String removeFile(String path, String user, List<String> roles) {
        boolean deleted = deleteFile(path);
        return deleted ? "Remove Okay" : "";
    }

    @Override
    public String moveFile(String source, String target, String user, List<String> roles) {
        moveFile(source, target);
        return "Move Okay";
    }

    @Override
    public String saveInternalFile(String path, Object content, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String saveBinaryInternalFile(String path, InputStream content, String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeInternalFile(String filePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IRepositoryObject> getFiles(List<String> types, String username, List<String> roles) {
        return listFiles("", types);
    }

    @Override
    public List<IRepositoryObject> getFiles(List<String> types, String username, List<String> roles, String path) {
        return listFiles(path, types);
    }

    @Override
    public void createUser(String user) {
    }

    @Override
    public void deleteFolder(String folder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AclEntry getACL(String object, String username, List<String> roles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setACL(String object, String acl, String username, List<String> roles) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserService(UserService userService) {
    }

    @Override
    public List<MondrianSchema> getInternalFilesOfFileType(String type) {
        return null;
    }

    @Override
    public void createFileMixin(String type) throws RepositoryException {
    }

    @Override
    public byte[] exportRepository() {
        return null;
    }

    @Override
    public void restoreRepository(byte[] data) {
    }

    @Override
    public boolean hasHomeDirectory(String name) {
        return true;
    }

    @Override
    public void restoreLegacyFiles(byte[] data) {
    }

    @Override
    public String getFoodmartschema() {
        return null;
    }

    @Override
    public void setFoodmartschema(String schema) {
    }

    @Override
    public void setFoodmartdir(String dir) {
    }

    @Override
    public String getFoodmartdir() {
        return null;
    }

    @Override
    public String getDatadir() {
        return null;
    }

    @Override
    public void setDatadir(String dir) {
    }

    @Override
    public void setFoodmarturl(String foodmarturl) {
    }

    @Override
    public String getFoodmarturl() {
        return null;
    }

    @Override
    public String getEarthquakeUrl() {
        return null;
    }

    @Override
    public String getEarthquakeDir() {
        return null;
    }

    @Override
    public String getEarthquakeSchema() {
        return null;
    }

    @Override
    public void setEarthquakeUrl(String earthquakeUrl) {
    }

    @Override
    public void setEarthquakeDir(String earthquakeDir) {
    }

    @Override
    public void setEarthquakeSchema(String earthquakeSchema) {
    }

    @Override
    public void setExternalPropertiesFile(String file) {
    }

    @Override
    public String[] getAvailablePropertiesKeys() {
        return null;
    }

    @Override
    public List<JujuSource> getJujuDatasources() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }


    /**
     * Retrieves all files in the given directory
     */
    public List<IRepositoryObject> listFiles(String path, List<String> fileTypes) {

        List<IRepositoryObject> files = new ArrayList<>();
        String normalizedPath = normalizePath(path);

        // add synthetic root folder
        if (normalizedPath.isEmpty()) {
            files.add(new RepositoryFolderObject("/", "#", "/", ACCESS_READ, Collections.emptyList()));
        }

        // retrieve directory contents
        for (File file : new File(storageDir, normalizedPath).listFiles()) {
            if (file.isHidden()) {
                continue;
            }

            String filePath = normalizedPath + "/" + file.getName();

            if (file.isDirectory()) {
                List<IRepositoryObject> directoryContents = listFiles(filePath, fileTypes);

                files.add(new RepositoryFolderObject(file.getName(),
                        "#" + filePath, filePath, ACCESS_ALL, directoryContents));
            }
            else {
                String extension = FilenameUtils.getExtension(file.getName());
                if (fileTypes.contains(extension)) {
                    files.add(new RepositoryFileObject(file.getName(), "#" + filePath, extension, filePath, ACCESS_ALL));
                }
            }
        }

        // sort
        Collections.sort(files, new Comparator<IRepositoryObject>() {

            @Override
            public int compare(IRepositoryObject o1, IRepositoryObject o2) {
                if (o1.getType().equals(IRepositoryObject.Type.FOLDER) && o2.getType().equals(IRepositoryObject.Type.FILE))
                    return -1;
                if (o1.getType().equals(IRepositoryObject.Type.FILE) && o2.getType().equals(IRepositoryObject.Type.FOLDER))
                    return 1;
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());

            }

        });

        return files;
    }


    private String normalizePath(String path) {
        path = StringUtils.replace(StringUtils.defaultString(path), "\\", "/");
        path = StringUtils.replace(path, "..", "");
        return StringUtils.stripStart(path, "/.");
    }


    /**
     * Creates a directory or a file
     */
    private void saveFile(String path, Object content) {

         String normalizedPath = normalizePath(path);
         File file = new File(storageDir, normalizedPath);

         if (normalizedPath.contains("/")) {
             file.getParentFile().mkdirs();
         }
         if (content == null) {
             file.mkdir();
         }
         else {
             try {
                FileUtils.writeStringToFile(file, (String) content, StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
         }
    }

    /**
     * Reads a file into a string
     */
    private String readFile(String path) {

        try {
            File file = new File(storageDir, normalizePath(path));
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Deletes the given file
     */
    private boolean deleteFile(String path) {

        File file = new File(storageDir, normalizePath(path));
        return file.delete();
    }


    /**
     * Moves the given source file to a different directory
     */
    private void moveFile(String source, String target) {

        File sourceFile = new File(storageDir, normalizePath(source));
        File targetDir = new File(storageDir, normalizePath(target));
        File targetFile = new File(targetDir, sourceFile.getName());

        sourceFile.renameTo(targetFile);
    }


}

