/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;
import static org.junit.Assert.*;

import org.etools.j1939tools.modules.DateTimeModule;
import org.etools.j1939tools.modules.TestDateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GhgActiveTechnologyGroupTest {

    private GhgActiveTechnologyGroup instance1;
    private GhgActiveTechnologyGroup instance2;

    @Before
    public void setUp() {
        instance1 = new GhgActiveTechnologyGroup("Some Label", 0xFA, 0x88);
        instance2 = new GhgActiveTechnologyGroup("Some Other Label", 0xFF, 0xFF);
        DateTimeModule.setInstance(new TestDateTimeModule());
    }

    @After
    public void tearDown() {
        DateTimeModule.setInstance(null);
    }

    @Test
    public void getTechIndex() {
        assertEquals("Some Label", instance1.getTechIndex());
    }

    @Test
    public void setTechIndex() {
        instance1.setTechIndex("Good times");
        assertEquals("Good times", instance1.getTechIndex());
    }

    @Test
    public void getTechTime() {
        assertEquals(255, instance2.getTechTime());
    }

    @Test
    public void setTechTime() {
        instance2.setTechTime(0x0D);
        assertEquals(13, instance2.getTechTime());
    }

    @Test
    public void getTechVehDistance() {
        assertEquals(136, instance1.getTechVehDistance());
    }

    @Test
    public void setTechVehDistance() {
        instance1.setTechVehDistance(0xAD);
        assertEquals(173, instance1.getTechVehDistance());
    }

    @Test
    public void testToString() {
        String expectedInstance1 = "GhgActiveTechnologyGroup {" + NL;
        expectedInstance1 += "  Index = Some Label" + NL;
        expectedInstance1 += "  Time = 250" + NL;
        expectedInstance1 += "  Vehicle Distance = 136" + NL;
        expectedInstance1 += "}" + NL;
        assertEquals(expectedInstance1, instance1.toString());
        String expectedInstance2 = "GhgActiveTechnologyGroup {" + NL;
        expectedInstance2 += "  Index = Some Other Label" + NL;
        expectedInstance2 += "  Time = 255" + NL;
        expectedInstance2 += "  Vehicle Distance = 255" + NL;
        expectedInstance2 += "}" + NL;
        assertEquals(expectedInstance2, instance2.toString());
    }
}