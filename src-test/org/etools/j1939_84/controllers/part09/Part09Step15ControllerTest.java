/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part09;

import static net.soliddesign.j1939tools.j1939.packets.LampStatus.OFF;
import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executor;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.TestDateTimeModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.soliddesign.j1939tools.bus.RequestResult;
import net.soliddesign.j1939tools.j1939.J1939;
import net.soliddesign.j1939tools.j1939.packets.DM27AllPendingDTCsPacket;
import net.soliddesign.j1939tools.j1939.packets.DM28PermanentEmissionDTCPacket;
import net.soliddesign.j1939tools.j1939.packets.DM29DtcCounts;
import net.soliddesign.j1939tools.j1939.packets.DiagnosticTroubleCode;
import net.soliddesign.j1939tools.modules.CommunicationsModule;
import net.soliddesign.j1939tools.modules.DateTimeModule;

;

@RunWith(MockitoJUnitRunner.class)
public class Part09Step15ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 9;
    private static final int STEP_NUMBER = 15;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private CommunicationsModule communicationsModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    @Mock
    private J1939 j1939;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    private TestResultsListener listener;

    private DataRepository dataRepository;

    private StepController instance;

    @Before
    public void setUp() throws Exception {
        dataRepository = DataRepository.newInstance();
        listener = new TestResultsListener(mockListener);

        instance = new Part09Step15Controller(executor,
                                              bannerModule,
                                              new TestDateTimeModule(),
                                              dataRepository,
                                              engineSpeedModule,
                                              vehicleInformationModule,
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
    public void tearDown() throws Exception {
        DateTimeModule.setInstance(null);
        verifyNoMoreInteractions(executor,
                                 bannerModule,
                                 engineSpeedModule,
                                 vehicleInformationModule,
                                 communicationsModule,
                                 mockListener);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals("Part " + PART_NUMBER + " Step " + STEP_NUMBER, instance.getDisplayName());
    }

    @Test
    public void testGetPartNumber() {
        assertEquals(PART_NUMBER, instance.getPartNumber());
    }

    @Test
    public void testGetStepNumber() {
        assertEquals(STEP_NUMBER, instance.getStepNumber());
    }

    @Test
    public void testGetTotalSteps() {
        assertEquals(0, instance.getTotalSteps());
    }

    @Test
    public void testHappyPathNoFailures() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 1);

        OBDModuleInformation obdModuleInformation1 = new OBDModuleInformation(1);
        dataRepository.putObdModule(obdModuleInformation1);
        var dm29_1 = DM29DtcCounts.create(1, 0, 0, 0xFF, 0, 0, 0);

        var dm29_2 = DM29DtcCounts.create(2, 0, 0, 0, 0, 0, 0);
        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0, dm29_1, dm29_2));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testNoResponse() {
        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of());

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.b - No ECU reported > 0 for permanent DTC");
    }

    @Test
    public void testFailureForPendingNoZero() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 1, 0, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.a - Engine #1 (0) reported > 0 for emission-related pending");
    }

    @Test
    public void testFailureForMILNonZero() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 1, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.a - Engine #1 (0) reported > 0 for MIL-on");
    }

    @Test
    public void testFailureForPreviousMILNonZero() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 1, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.a - Engine #1 (0) reported > 0 for previous MIL on");
    }

    @Test
    public void testFailureForNoPermanent() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 0);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.b - No ECU reported > 0 for permanent DTC");
    }

    @Test
    public void testFailureForDTCDifference() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc1 = DiagnosticTroubleCode.create(123, 1, 1, 1);
        var dtc2 = DiagnosticTroubleCode.create(456, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc1, dtc2), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.c - Engine #1 (0) reported different number for permanent DTC than what it reported in DM28");
    }

    @Test
    public void testFailureForSupportDM27AllPendingWrong() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 1, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.d - Engine #1 (0) reported > 0 for all pending DTCs");
    }

    @Test
    public void testFailureForDoesNotSupportDM27AllPendingWrong() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.15.2.e - Engine #1 (0) did not report all pending DTCs = 0xFF");
    }

    @Test
    public void testWarningForMoreThanOneDTC() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc1 = DiagnosticTroubleCode.create(123, 1, 1, 1);
        var dtc2 = DiagnosticTroubleCode.create(4456, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc1, dtc2), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 2);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.9.15.3.a - Engine #1 (0) reported > 1 for permanent DTC");
    }

    @Test
    public void testWarningForMoreThanOneDTCModule() {
        OBDModuleInformation obdModuleInformation0 = new OBDModuleInformation(0);
        var dtc = DiagnosticTroubleCode.create(123, 1, 1, 1);
        obdModuleInformation0.set(DM28PermanentEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 9);
        obdModuleInformation0.set(DM27AllPendingDTCsPacket.create(0, OFF, OFF, OFF, OFF), 9);
        dataRepository.putObdModule(obdModuleInformation0);
        var dm29_0 = DM29DtcCounts.create(0, 0, 0, 0, 0, 0, 1);

        OBDModuleInformation obdModuleInformation1 = new OBDModuleInformation(1);
        var dtc1 = DiagnosticTroubleCode.create(456, 1, 1, 1);
        obdModuleInformation1.set(DM28PermanentEmissionDTCPacket.create(1, OFF, OFF, OFF, OFF, dtc1), 9);
        dataRepository.putObdModule(obdModuleInformation1);
        var dm29_1 = DM29DtcCounts.create(1, 0, 0, 0xFF, 0, 0, 1);

        when(communicationsModule.requestDM29(any())).thenReturn(RequestResult.of(dm29_0, dm29_1));

        runTest();

        verify(communicationsModule).requestDM29(any());

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.9.15.3.b - More than one ECU reported > 0 for permanent DTC");
    }

}
