/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part09;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939tools.j1939.packets.AcknowledgmentPacket.Response.NACK;
import static org.etools.j1939tools.j1939.packets.LampStatus.OFF;
import static org.etools.j1939tools.j1939.packets.LampStatus.ON;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939tools.j1939.packets.DM2PreviouslyActiveDTC;
import org.etools.j1939tools.j1939.packets.DiagnosticTroubleCode;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Part09Step22ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 9;
    private static final int STEP_NUMBER = 22;

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

        instance = new Part09Step22Controller(executor,
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
    public void testEmptyGlobalPacketsFailure() {

        dataRepository.putObdModule(new OBDModuleInformation(0));
        dataRepository.putObdModule(new OBDModuleInformation(1));

        var dm2 = DM2PreviouslyActiveDTC.create(0, OFF, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, List.of(), List.of()));

        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(BusResult.of(dm2));

        var nack = AcknowledgmentPacket.create(1, NACK);
        when(communicationsModule.requestDM2(any(), eq(1))).thenReturn(BusResult.of(nack));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));
        verify(communicationsModule).requestDM2(any(), eq(1));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.22.4.b - OBD ECU Engine #1 (0) did not provide a response to Global query and did not provide a NACK for the DS query");

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testNoFailures() {

        dataRepository.putObdModule(new OBDModuleInformation(0));
        dataRepository.putObdModule(new OBDModuleInformation(1));

        var dm2 = DM2PreviouslyActiveDTC.create(0, OFF, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, dm2));

        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(new BusResult<>(false, dm2));

        var nack = AcknowledgmentPacket.create(1, NACK);
        when(communicationsModule.requestDM2(any(), eq(1))).thenReturn(new BusResult<>(false, nack));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));
        verify(communicationsModule).requestDM2(any(), eq(1));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
    }

    @Test
    public void testFailureForNoNack() {

        dataRepository.putObdModule(new OBDModuleInformation(0));
        dataRepository.putObdModule(new OBDModuleInformation(1));

        var dm2 = DM2PreviouslyActiveDTC.create(0, OFF, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, dm2));

        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(new BusResult<>(false, dm2));

        when(communicationsModule.requestDM2(any(), eq(1))).thenReturn(new BusResult<>(true));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));
        verify(communicationsModule).requestDM2(any(), eq(1));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.22.4.b - OBD ECU Engine #2 (1) did not provide a response to Global query and did not provide a NACK for the DS query");
    }

    @Test
    public void testFailureForDTC() {
        OBDModuleInformation obdModule = new OBDModuleInformation(0);
        dataRepository.putObdModule(obdModule);

        var dtc = DiagnosticTroubleCode.create(123, 12, 0, 1);
        var dm2 = DM2PreviouslyActiveDTC.create(0, OFF, OFF, OFF, OFF, dtc);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, dm2));
        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(new BusResult<>(false, dm2));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.22.2.b - ECU Engine #1 (0) reported a previously active DTC");
    }

    @Test
    public void testFailureForMILNotOff() {
        OBDModuleInformation obdModule = new OBDModuleInformation(0);
        dataRepository.putObdModule(obdModule);

        var dm2 = DM2PreviouslyActiveDTC.create(0, ON, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, dm2));
        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(new BusResult<>(false, dm2));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.22.2.a - ECU Engine #1 (0) reported MIL status of on");
    }

    @Test
    public void testFailureGlobalAndDSDifference() {
        dataRepository.putObdModule(new OBDModuleInformation(0));

        var globalDM2 = DM2PreviouslyActiveDTC.create(0, OFF, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any())).thenReturn(new RequestResult<>(false, globalDM2));

        var dsDM2 = DM2PreviouslyActiveDTC.create(0, ON, OFF, OFF, OFF);
        when(communicationsModule.requestDM2(any(), eq(0))).thenReturn(new BusResult<>(false, dsDM2));

        runTest();

        verify(communicationsModule).requestDM2(any());
        verify(communicationsModule).requestDM2(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.9.22.4.a - Difference compared to data received during global request from Engine #1 (0)");
    }

}
