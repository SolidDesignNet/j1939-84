/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part07;

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
import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939tools.j1939.packets.DM12MILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM23PreviouslyMILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM31DtcToLampAssociation;
import org.etools.j1939tools.j1939.packets.DTCLampStatus;
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
public class Part07Step11ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 7;
    private static final int STEP_NUMBER = 11;

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

        instance = new Part07Step11Controller(executor,
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

        var dtc = DiagnosticTroubleCode.create(123, 12, 0, 1);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        obdModuleInformation.set(DM12MILOnEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 7);
        dataRepository.putObdModule(obdModuleInformation);
        var dm31 = DM31DtcToLampAssociation.create(0, 0, DTCLampStatus.create(dtc, OFF, OFF, OFF, OFF));
        when(communicationsModule.requestDM31(any(), eq(0))).thenReturn(RequestResult.of(dm31));

        dataRepository.putObdModule(new OBDModuleInformation(1));
        var nack = AcknowledgmentPacket.create(1, NACK);
        when(communicationsModule.requestDM31(any(), eq(1))).thenReturn(new RequestResult<>(true, nack));

        runTest();

        verify(communicationsModule).requestDM31(any(), eq(0));
        verify(communicationsModule).requestDM31(any(), eq(1));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        assertEquals(List.of(), listener.getOutcomes());
    }

    @Test
    public void testFailureForMilOn() {
        var dtc = DiagnosticTroubleCode.create(123, 12, 0, 1);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        obdModuleInformation.set(DM23PreviouslyMILOnEmissionDTCPacket.create(0, ON, OFF, OFF, OFF, dtc), 7);
        dataRepository.putObdModule(obdModuleInformation);

        var dtc1 = DiagnosticTroubleCode.create(456, 12, 0, 1);
        var dm31 = DM31DtcToLampAssociation.create(0, 0, DTCLampStatus.create(dtc1, ON, ON, OFF, OFF));
        when(communicationsModule.requestDM31(any(), eq(0))).thenReturn(RequestResult.of(dm31));

        runTest();

        verify(communicationsModule).requestDM31(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.7.11.2.b - Engine #1 (0) did not report MIL off for all DTCs reported");
    }

    @Test
    public void testFailureForDtcSameAsDM23() {
        var dtc = DiagnosticTroubleCode.create(123, 12, 0, 1);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        obdModuleInformation.set(DM23PreviouslyMILOnEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 7);
        dataRepository.putObdModule(obdModuleInformation);

        var dm31 = DM31DtcToLampAssociation.create(0, 0, DTCLampStatus.create(dtc, OFF, OFF, OFF, OFF));
        when(communicationsModule.requestDM31(any(), eq(0))).thenReturn(RequestResult.of(dm31));

        runTest();

        verify(communicationsModule).requestDM31(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.7.11.2.a - Engine #1 (0) response includes the same DTC as it reported by DM23");
    }

    @Test
    public void testFailureForNoNACK() {
        var dtc = DiagnosticTroubleCode.create(123, 12, 0, 1);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        obdModuleInformation.set(DM12MILOnEmissionDTCPacket.create(0, OFF, OFF, OFF, OFF, dtc), 7);
        dataRepository.putObdModule(obdModuleInformation);
        var dm31 = DM31DtcToLampAssociation.create(0, 0, DTCLampStatus.create(dtc, OFF, OFF, OFF, OFF));
        when(communicationsModule.requestDM31(any(), eq(0))).thenReturn(RequestResult.of(dm31));

        dataRepository.putObdModule(new OBDModuleInformation(1));
        when(communicationsModule.requestDM31(any(), eq(1))).thenReturn(new RequestResult<>(true));

        runTest();

        verify(communicationsModule).requestDM31(any(), eq(0));
        verify(communicationsModule).requestDM31(any(), eq(1));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.7.11.2.c - OBD ECU Engine #2 (1) did not provide a NACK for the DS query");
    }

}
