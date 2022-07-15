/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;

import java.util.Arrays;
import org.etools.j1939tools.j1939.model.Spn;

public class GhgActiveTechnologyGroup {

    public static final long ERROR = Long.MIN_VALUE;
    public static final long NOT_AVAILABLE = Long.MAX_VALUE;

    private long index;

    private long time;

    private long vehDistance;

    private final int[] data;

    private final Spn spn;


    public GhgActiveTechnologyGroup(int[] bytes, Spn spn) {
        this.data = Arrays.copyOf(bytes, bytes.length);
        this.spn = spn;
        this.index = bytes[0];
        this.time = getScaledLongValue(bytes, 1);
        this.vehDistance = getScaledLongValue(bytes, 5);
    }

    private static long getScaledLongValue(int[] bytes, int index) {
        int upperByte = bytes[index + 3];
        switch (upperByte) {
        case 0xFF:
            return NOT_AVAILABLE;
        case 0xFE:
            return ERROR;
        default:
            return get32(bytes, index) & 0xFFFFFFFFL;
        }
    }
    private static long get32(int[] bytes, int i) {
        return ((long) (bytes[i + 3] & 0xFF) << 24) | ((bytes[i + 2] & 0xFF) << 16) | ((bytes[i + 1] & 0xFF) << 8)
                | (bytes[i] & 0xFF);
    }

    public GhgActiveTechnologyGroup(int index, int time, int vehDistance, Spn spn) {
        this.spn = spn;
        this.index = index;
        this.time = time;
        this.vehDistance = vehDistance;
        data = new int[0];
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long techIndex) {
        this.index = techIndex;
    }

    public long getTime() {
        return time;
    }

    public void setTime(int techTime) {
        this.time = techTime;
    }

    public long getVehDistance() {
        return vehDistance;
    }

    public void setVehDistance(int techVehDistance) {
        this.vehDistance = techVehDistance;
    }

    @Override
    public String toString() {
        return "GhgActiveTechnologyGroup {" + NL +
                "  Index = " + index + NL +
                "  Time = " + time + NL +
                "  Vehicle Distance = " + vehDistance + NL +
                "}" + NL;
    }
}
