package com.projecta.monsai.mondrian;

import com.projecta.monsai.security.CubeAccess;

import mondrian.olap.Access;
import mondrian.olap.Cube;
import mondrian.olap.Dimension;
import mondrian.olap.Hierarchy;
import mondrian.olap.Level;
import mondrian.olap.Member;
import mondrian.olap.NamedSet;
import mondrian.olap.OlapElement;
import mondrian.olap.Role;
import mondrian.olap.Schema;

/**
 * Custom implementation of mondrian.olap.Role that allows access to a specific
 * list of cubes
 */
public class CubeAccessRole implements Role {

    private CubeAccess cubeAccess;


    public CubeAccessRole(CubeAccess cubeAccess) {
        this.cubeAccess = cubeAccess;
    }


    @Override
    public boolean canAccess(OlapElement olapElement) {

        if (olapElement instanceof Cube) {
            return getAccess((Cube) olapElement) != Access.NONE;
        }
        return true;
    }

    @Override
    public Access getAccess(Cube cube) {
        return cubeAccess == null || cubeAccess.isCubeAllowed(cube.getName()) ? Access.ALL : Access.NONE;
    }

    @Override
    public Access getAccess(Schema schema) {
        return Access.CUSTOM;
    }

    @Override
    public Access getAccess(Dimension dimension) {
        return Access.ALL;
    }

    @Override
    public Access getAccess(Hierarchy hierarchy) {
        return Access.ALL;
    }

    @Override
    public Access getAccess(Level level) {
        return Access.ALL;
    }

    @Override
    public Access getAccess(Member member) {
        return Access.ALL;
    }

    @Override
    public Access getAccess(NamedSet set) {
        return Access.ALL;
    }

    @Override
    public HierarchyAccess getAccessDetails(final Hierarchy hierarchy) {

        return new HierarchyAccess() {

            @Override
            public Access getAccess(Member member) {
                return Access.ALL;
            }

            @Override
            public int getTopLevelDepth() {
                return 0;
            }

            @Override
            public int getBottomLevelDepth() {
                Level[] levels = hierarchy.getLevels();
                return levels[levels.length - 1].getDepth();
            }

            @Override
            public RollupPolicy getRollupPolicy() {
                return RollupPolicy.FULL;
            }

            @Override
            public boolean hasInaccessibleDescendants(Member member) {
                return false;
            }

        };
    }

}
