/*
 * Copyright 2021 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part02;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM27AllPendingDTCsPacket;
import org.etools.j1939tools.j1939.packets.DM29DtcCounts;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * The unit test for {@link Part02Step12Controller}
 */
@RunWith(MockitoJUnitRunner.class)
public class Part02Step12ControllerTest extends AbstractControllerTest {
    private static final int PART = 2;
    private static final int STEP = 12;

    @Mock
    private BannerModule bannerModule;

    private DataRepository dataRepository;

    @Mock
    private CommunicationsModule communicationsModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    private Part02Step12Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    private static DM27AllPendingDTCsPacket dm27(int source) {
        return new DM27AllPendingDTCsPacket(Packet.create(DM27AllPendingDTCsPacket.PGN, source, 0, 0, 0, 0, 0, 0));
    }

    @Before
    public void setUp() {
        dataRepository = DataRepository.newInstance();
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

        instance = new Part02Step12Controller(
                                              executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              dataRepository,
                                              DateTimeModule.getInstance(),
                                              communicationsModule);

        setup(instance,
              listener,
              j1939,
              executor,
              reportFileModule,
              engineSpeedModule,
              vehicleInformationModule,
              communicationsModule);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(executor,
                                 engineSpeedModule,
                                 bannerModule,
                                 vehicleInformationModule,
                                 communicationsModule,
                                 mockListener);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Part " + PART + " Step " + STEP, instance.getDisplayName());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(STEP, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

    @Test
    public void testNoErrors() {

        DM29DtcCounts packet1 = DM29DtcCounts.create(1, 0, 0, 0, 0, 0, 0);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(1);
        obdModuleInformation.set(dm27(1), 1);
        dataRepository.putObdModule(obdModuleInformation);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet1));
        when(communicationsModule.requestDM29(any(), eq(0x01))).thenReturn(BusResult.of(packet1));

        runTest();

        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(0x01));

        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());
    }

    @Test
    public void testFailureForSupportDM27WithNonZeros() {
        // Module 2 will support DM27 but return bad values
        OBDModuleInformation module2 = new OBDModuleInformation(2);
        module2.set(dm27(2), 1);
        dataRepository.putObdModule(module2);
        DM29DtcCounts packet2 = DM29DtcCounts.create(0x02, 0, 0x00, 0x00, 0x04, 0x00, 0xFF);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet2));
        when(communicationsModule.requestDM29(any(), eq(2))).thenReturn(BusResult.of(packet2));

        runTest();

        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(2));

        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.2.a - Turbocharger (2) did not report pending/all pending/MIL on/previous MIL on/permanent = 0/0/0/0/0");
    }

    @Test
    public void testFailureForNoSupportAndNonZero() {
        // Module 3 will not support DM27 but return bad values
        dataRepository.putObdModule(new OBDModuleInformation(3));
        DM29DtcCounts packet3 = DM29DtcCounts.create(0x03, 0, 0x00, 0, 0x00, 0x00, 0x00);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet3));
        when(communicationsModule.requestDM29(any(), eq(3))).thenReturn(BusResult.of(packet3));

        runTest();

        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(3));

        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.2.b - Transmission #1 (3) did not report pending/all pending/MIL on/previous MIL on/permanent = 0/0xFF/0/0/0");
    }

    @Test
    public void testFailureForNonOBDModule() {
        dataRepository.putObdModule(new OBDModuleInformation(3));
        DM29DtcCounts packet3 = DM29DtcCounts.create(0x03, 0, 0x00, 0xFF, 0x00, 0x00, 0x00);

        when(communicationsModule.requestDM29(any(), eq(3))).thenReturn(BusResult.of(packet3));

        // Module 6 will be a non-obd module with bad values
        DM29DtcCounts packet6 = DM29DtcCounts.create(0x06, 0, 0, 0xFF, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet3, packet6));

        runTest();

        verify(communicationsModule).requestDM29(any(), eq(3));
        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.2.b - Shift Console - Secondary (6) did not report pending/all pending/MIL on/previous MIL on/permanent = 0/0xFF/0/0/0");

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.2.c - A non-OBD ECU Shift Console - Secondary (6) reported pending, MIL-on, previously MIL-on or permanent DTC count greater than 0");
    }

    @Test
    public void testFailureForDifference() {

        // Module 7 will return different values global/ds
        dataRepository.putObdModule(new OBDModuleInformation(7));
        DM29DtcCounts packet71 = DM29DtcCounts.create(0x07, 0, 0, 0xFF, 0, 0, 0);
        DM29DtcCounts packet72 = DM29DtcCounts.create(0x07, 0, 1, 0xFF, 0, 0, 0);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet71));
        when(communicationsModule.requestDM29(any(), eq(7))).thenReturn(BusResult.of(packet72));

        runTest();

        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(7));

        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.4.a - Difference compared to data received during global request from Power TakeOff - (Main or Rear) (7)");
    }

    @Test
    public void testFailureForNoNACK() {

        // Module 0 will support DM27 and have no errors
        OBDModuleInformation module0 = new OBDModuleInformation(0);
        module0.set(dm27(0), 1);
        dataRepository.putObdModule(module0);
        DM29DtcCounts packet0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 0);

        // Module 4 will not respond at all
        dataRepository.putObdModule(new OBDModuleInformation(4));

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(packet0));

        when(communicationsModule.requestDM29(any(), eq(0))).thenReturn(BusResult.of(packet0));
        when(communicationsModule.requestDM29(any(), eq(4))).thenReturn(new BusResult<>(true));

        runTest();

        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(0));
        verify(communicationsModule).requestDM29(any(), eq(4));

        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.4.b - OBD ECU Transmission #2 (4) did not provide a response to Global query and did not provide a NACK for the DS query");
    }

    @Test
    public void testEmptyPacketFailure() {
        dataRepository.putObdModule(new OBDModuleInformation(1));

        when(communicationsModule.requestDM29(any())).thenReturn(new RequestResult<>(true));
        when(communicationsModule.requestDM29(any(), eq(0x01))).thenReturn(new BusResult<>(true));

        runTest();

        verify(communicationsModule).setJ1939(j1939);
        verify(communicationsModule).requestDM29(any());
        verify(communicationsModule).requestDM29(any(), eq(0x01));

        verify(mockListener).addOutcome(PART, STEP, FAIL, "6.2.12.2.d - No OBD ECU provided DM29");
        verify(mockListener).addOutcome(PART,
                                        STEP,
                                        FAIL,
                                        "6.2.12.4.b - OBD ECU Engine #2 (1) did not provide a response to Global query and did not provide a NACK for the DS query");

        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());
    }

}
