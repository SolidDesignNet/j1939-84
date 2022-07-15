/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;
import static org.junit.Assert.*;

import java.util.List;
import org.etools.j1939tools.j1939.J1939DaRepository;
import org.etools.j1939tools.j1939.model.Spn;
import org.etools.j1939tools.j1939.model.SpnDataParser;
import org.etools.j1939tools.j1939.model.SpnDefinition;
import org.etools.j1939tools.modules.DateTimeModule;
import org.etools.j1939tools.modules.TestDateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GhgActiveTechnologyGroupTest {

    private GhgActiveTechnologyGroup instance1;
    private GhgActiveTechnologyGroup instance2;

    private char groupLetter;

    @Before
    public void setUp() {
        Slot slot = J1939DaRepository.findSlot(64255, 12697);
        var pgnDefinition = J1939DaRepository.getInstance().findPgnDefinition(64255);
        byte[] data = {0x06, 0x7D, 0x60, 0x10, 0x00, (byte)0xC0, (byte)0xBC, 0x05, 0x00};
        var spn = SupportedSPN.parseSPN(data);;
        SpnDataParser.parse(data)
        instance1 = new GhgActiveTechnologyGroup(0xAD, 0xFA, 0x88);
        instance2 = new GhgActiveTechnologyGroup(0xF0, 0xFF, 0xFF);
        DateTimeModule.setInstance(new TestDateTimeModule());
    }

    @After
    public void tearDown() {
        DateTimeModule.setInstance(null);
    }

    @Test
    public void getTechIndex() {
        assertEquals(55, instance1.getIndex());
    }

    @Test
    public void setTechIndex() {
        instance1.setIndex(0x98);
        assertEquals(20, instance1.getIndex());
    }

    @Test
    public void getTechTime() {
        assertEquals(255, instance2.getTime());
    }

    @Test
    public void setTechTime() {
        instance2.setTime(0x0D);
        assertEquals(13, instance2.getTime());
    }

    @Test
    public void getTechVehDistance() {
        assertEquals(136, instance1.getVehDistance());
    }

    @Test
    public void setTechVehDistance() {
        instance1.setVehDistance(0xAD);
        assertEquals(173, instance1.getVehDistance());
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