/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;
import static org.junit.Assert.*;

import org.etools.j1939tools.bus.Packet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GhgPacketTest {

    private GhgPacket instance1;

    private GhgPacket instance2;

    @Before
    public void setUp() throws Exception {
        instance1 = new GhgPacket(Packet.create(0xFAFF,
                                                    0x00,
                                                    // @formatter:off
                                                    0x06, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00, //
                                                    0x06, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00,
                                                    0x00, 0x00, 0x00, 0x00, 0x00));
        // @formatter:on

        instance2 = new GhgPacket(Packet.create(0xFB01,
                                        0x00,
                                        // @formatter:off
                                        0x06, 0x7D, 0x60, 0x10, 0x00, 0xC0, 0xBC, 0x05, 0x00,
                                        0x04, 0xCE, 0x31, 0x02, 0x00, 0x02, 0x49, 0x1D, 0x00,
                                        0x02, 0x00, 0xE0, 0x79, 0x00, 0x00, 0x00, 0x00, 0x00,
                                        0xF9, 0x86, 0xAD, 0x00, 0x00, 0xA8, 0xD2, 0x02, 0x00,
                                        0xF7, 0x4B, 0xC3, 0x00, 0x00, 0x90, 0xFB, 0x00, 0x00,
                                        0xF5, 0xD0, 0xB3, 0x00, 0x00, 0x38, 0xED, 0x02, 0x00));

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void geActiveTechnologyGroups() {
    }

    @Test
    public void testToString(){
        String expectedString1 = "Green House Gas Stored 100 Hour Active Technology Tracking from Engine #1 (0): " + NL;
        expectedString1 += "  SPN 12697, GHG Tracking Stored 100 Hour Active Technology Index: Predictive Cruise Control" + NL;
        expectedString1 += "  SPN 12698, GHG Tracking Stored 100 Hour Active Technology Time: 0.000 s" + NL;
        expectedString1 += "  SPN 12699, GHG Tracking Stored 100 Hour Active Technology Vehicle Distance: 0.000 km" + NL;
//        assertEquals(expectedString1, instance1.toString());

        String expectedString2 = "Green House Gas Lifetime Active Technology Tracking from Engine #1 (0): " + NL;
        expectedString2 += "  SPN 12691, GHG Tracking Lifetime Active Technology Index: Predictive Cruise Control" + NL;
        expectedString2 += "  SPN 12692, GHG Tracking Lifetime Active Technology Time: 1073277.000 s" + NL;
        expectedString2 += "  SPN 12693, GHG Tracking Lifetime Active Technology Vehicle Distance: 16106148320.000 m" + NL;
        assertEquals(expectedString2, instance2.toString());
    }

    @Test
    public void testGhgGroups(){
        assertEquals(6, instance1.getActiveTechnologyGroups().size());
        assertEquals(3, instance2.getActiveTechnologyGroups());
    }
}