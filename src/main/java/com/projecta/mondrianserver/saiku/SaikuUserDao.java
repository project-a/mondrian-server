package com.projecta.mondrianserver.saiku;

import java.util.Collection;

import org.saiku.UserDAO;
import org.saiku.database.dto.SaikuUser;

import com.google.common.collect.ImmutableList;

/**
 * Dummy implementation of UserDAO
 */
public class SaikuUserDao implements UserDAO {

    public static final SaikuUser ADMIN_USER = new SaikuUser();

    static {
        ADMIN_USER.setId(1);
        ADMIN_USER.setUsername("admin");
        ADMIN_USER.setPassword("admin");
        ADMIN_USER.setEmail("");
        ADMIN_USER.setRoles(new String[] {"ROLE_USER", "ROLE_ADMIN"});
    }

    @Override
    public SaikuUser insert(SaikuUser user) {
        return user;
    }

    @Override
    public void insertRole(SaikuUser user) {
    }

    @Override
    public void deleteUser(SaikuUser user) {
    }

    @Override
    public void deleteRole(SaikuUser user) {
    }

    @Override
    public String[] getRoles(SaikuUser user) {
        return ADMIN_USER.getRoles();
    }

    @Override
    public SaikuUser findByUserId(int userId) {
        return ADMIN_USER;
    }

    @Override
    public Collection findAllUsers() {
        return ImmutableList.of(ADMIN_USER);
    }

    @Override
    public void deleteUser(String username) {
    }

    @Override
    public SaikuUser updateUser(SaikuUser user, boolean updatepassword) {
        return user;
    }

    @Override
    public void updateRoles(SaikuUser user) {
    }

}
