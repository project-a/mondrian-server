package com.projecta.mondrianserver.saiku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.saiku.UserDAO;
import org.saiku.database.dto.SaikuUser;
import org.saiku.service.ISessionService;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.datasource.IDatasourceManager;
import org.saiku.service.user.UserService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patched version of UserService that does not use a hardcoded reference to JdbcUserDAO
 */
public class SaikuUserService extends UserService {

    private UserDAO uDAO;

    private IDatasourceManager iDatasourceManager;
    private DatasourceService datasourceService;
    private ISessionService sessionService;
    private List<String> adminRoles;
    //private static final Logger log = LoggerFactory.getLogger(SaikuUserService.class);
	private static final Logger log = LogManager.getLogger(SaikuUserService.class);
    private static final long serialVersionUID = 1L;

    @Override
    public void setAdminRoles( List<String> adminRoles ) {
        this.adminRoles = adminRoles;
    }

    public void setJdbcUserDAO(UserDAO jdbcUserDAO) {
        this.uDAO = jdbcUserDAO;
    }

    @Override
    public void setiDatasourceManager(IDatasourceManager repo) {
        this.iDatasourceManager = repo;
    }


    @Override
    public void setSessionService(ISessionService sessionService){
        this.sessionService = sessionService;
    }

    @Override
    public DatasourceService getDatasourceService() {
        return datasourceService;
    }

    @Override
    public void setDatasourceService(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @Override
    public SaikuUser addUser(SaikuUser u) {
        uDAO.insert(u);
        uDAO.insertRole(u);
        iDatasourceManager.createUser(u.getUsername());
        return u;
    }

    @Override
    public boolean deleteUser(SaikuUser u) {
        uDAO.deleteUser(u);
        iDatasourceManager.deleteFolder("homes/home:" + u.getUsername());
        return true;
    }

    @Override
    public SaikuUser setUser(SaikuUser u) {
        return null;
    }

    @Override
    public List<SaikuUser> getUsers() {
        Collection users = uDAO.findAllUsers();
        List<SaikuUser> l = new ArrayList<>();
        for (Object user : users) {
            l.add((SaikuUser) user);

        }
        return l;
    }

    @Override
    public SaikuUser getUser(int id) {
        return uDAO.findByUserId(id);
    }

    @Override
    public String[] getRoles(SaikuUser user) {
        return uDAO.getRoles(user);
    }

    @Override
    public void addRole(SaikuUser u) {
        uDAO.insertRole(u);
    }

    @Override
    public void removeRole(SaikuUser u) {
        uDAO.deleteRole(u);
    }

    @Override
    public void removeUser(String username) {
        SaikuUser u = getUser(Integer.parseInt(username));

        uDAO.deleteUser(username);

        iDatasourceManager.deleteFolder("homes/" + u.getUsername());

    }

    @Override
    public SaikuUser updateUser(SaikuUser u, boolean updatepassword) {
        SaikuUser user = uDAO.updateUser(u, updatepassword);
        uDAO.updateRoles(u);

        return user;

    }

    @Override
    public boolean isAdmin() {
        List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");

        if(roles!=null) {
            return !Collections.disjoint(roles, adminRoles);
        }
        else{
            return true;
        }

    }

    @Override
    public void checkFolders(){

        String username = (String ) sessionService.getAllSessionObjects().get("username");

        boolean home = true;
        if(username != null) {
          home = datasourceService.hasHomeDirectory(username);
        }
        if(!home){
            datasourceService.createUserHome(username);
        }



    }

    @Override
    public List<String> getAdminRoles(){
        return adminRoles;
    }

    @Override
    public String getActiveUsername() {
        try {
            return (String) sessionService.getSession().get("username");
        } catch (Exception e) {
            log.error("Could not fetch username");
        }
        return null;
    }

    @Override
    public String getSessionId() {
        try {
            return (String) sessionService.getSession().get("sessionid");
        } catch (Exception e) {
            log.error("Could not get sessionid: "+e.getMessage());
        }
        return null;
    }
}
