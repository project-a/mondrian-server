package com.projecta.monsai.security;

import java.util.Set;

/**
 * Bean class for cube access permissions
 */
public class CubeAccess {

    private Boolean allowed;
    private Set<String> cubes;

    public static final String REQUEST_ATTR = "cubeAccess";


    public boolean isAllowed() {
        return allowed != null && allowed;
    }

    public boolean isCubeAllowed(String cubeName) {
        return isAllowed() && cubes != null && cubes.contains(cubeName);
    }


    public Boolean getAllowed() {
        return allowed;
    }
    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
    public Set<String> getCubes() {
        return cubes;
    }
    public void setCubes(Set<String> cubes) {
        this.cubes = cubes;
    }

}
