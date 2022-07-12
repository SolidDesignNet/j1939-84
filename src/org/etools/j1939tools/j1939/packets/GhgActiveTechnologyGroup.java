/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;

public class GhgActiveTechnologyGroup {

    private String techIndex;

    private int techTime;

    private int techVehDistance;

    public GhgActiveTechnologyGroup(String techIndex, int techTime, int techVehDistance) {
        this.techIndex = techIndex;
        this.techTime = techTime;
        this.techVehDistance = techVehDistance;
    }

    public String getTechIndex() {
        return techIndex;
    }

    public void setTechIndex(String techIndex) {
        this.techIndex = techIndex;
    }

    public int getTechTime() {
        return techTime;
    }

    public void setTechTime(int techTime) {
        this.techTime = techTime;
    }

    public int getTechVehDistance() {
        return techVehDistance;
    }

    public void setTechVehDistance(int techVehDistance) {
        this.techVehDistance = techVehDistance;
    }

    @Override
    public String toString() {
        return "GhgActiveTechnologyGroup {" + NL +
                "  Index = " + techIndex + NL +
                "  Time = " + techTime + NL +
                "  Vehicle Distance = " + techVehDistance + NL +
                "}" + NL;
    }
}
