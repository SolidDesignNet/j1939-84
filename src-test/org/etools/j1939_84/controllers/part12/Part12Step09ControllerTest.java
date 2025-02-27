/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part12;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.etools.j1939_84.model.Outcome.WARN;
import static org.etools.j1939tools.j1939.packets.AcknowledgmentPacket.create;
import static org.etools.j1939tools.j1939.packets.AcknowledgmentPacket.Response.NACK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executor;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.ActionOutcome;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.VehicleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.TestDateTimeModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.model.FuelType;
import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.PerformanceRatio;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Part12Step09ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 12;
    private static final int STEP_NUMBER = 9;

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
    private TestDateTimeModule dateTimeModule;

    private StepController instance;

    @Before
    public void setUp() throws Exception {
        dataRepository = DataRepository.newInstance();
        listener = new TestResultsListener(mockListener);
        dateTimeModule = new TestDateTimeModule();

        instance = new Part12Step09Controller(executor,
                                              bannerModule,
                                              dateTimeModule,
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
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        // Module 0 Responds as expected
        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_1 = new PerformanceRatio(123, 1, 1, 0);
        var ratio5322_1 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          1,
                                                                          1,
                                                                          ratio3058_1,
                                                                          ratio123_1,
                                                                          ratio5322_1),
                                 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_9 = new PerformanceRatio(123, 1, 1, 0);
        var ratio5322_9 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          3,
                                                                          3,
                                                                          ratio3058_9,
                                                                          ratio123_9,
                                                                          ratio5322_9),
                                 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_11 = new PerformanceRatio(123, 1, 2, 0);
        var ratio5322_11 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          3,
                                                                          3,
                                                                          ratio3058_11,
                                                                          ratio123_11,
                                                                          ratio5322_11),
                                 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123 = new PerformanceRatio(123, 1, 2, 0);
        var ratio5322 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058, ratio123, ratio5322);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        // Module 1 won't be queried
        dataRepository.putObdModule(new OBDModuleInformation(1));

        // Module 3 will respond the same as Module 0
        OBDModuleInformation obdModuleInformation3 = new OBDModuleInformation(3);
        var ratio3058_1_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 1, 1, ratio3058_1_3), 1);

        var ratio3058_9_3 = new PerformanceRatio(3058, 1, 2, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_9_3), 9);

        var ratio3058_11_3 = new PerformanceRatio(3058, 1, 2, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_11_3), 11);

        dataRepository.putObdModule(obdModuleInformation3);

        var ratio3058_3 = new PerformanceRatio(3058, 1, 2, 3);
        var dm20_3 = DM20MonitorPerformanceRatioPacket.create(3, 4, 4, ratio3058_3);
        when(communicationsModule.requestDM20(any(), eq(3))).thenReturn(BusResult.of(dm20_3));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));
        verify(communicationsModule).requestDM20(any(), eq(3));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        assertEquals(List.of(), listener.getOutcomes());
    }

    @Test
    public void testFailureForRetry() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 2, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 2, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(new BusResult<>(false, create(0, NACK)));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.12.9.2.c - NACK received from OBD ECUs that previously provided a DM20 message");
    }

    @Test
    public void testFailureForNoGeneralDenominatorIncrease() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 2, 2, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.a - Engine #1 (0) numerator for monitor SPN 3058 Engine EGR System Monitor is greater than the corresponding value in part 11 test");
    }

    @Test
    public void testFailureDenominatorIncrease() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        // Module 0 Responds as expected
        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_1 = new PerformanceRatio(123, 1, 1, 0);
        var ratio5322_1 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          1,
                                                                          1,
                                                                          ratio3058_1,
                                                                          ratio123_1,
                                                                          ratio5322_1),
                                 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_9 = new PerformanceRatio(123, 1, 1, 0);
        var ratio5322_9 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          3,
                                                                          3,
                                                                          ratio3058_9,
                                                                          ratio123_9,
                                                                          ratio5322_9),
                                 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123_11 = new PerformanceRatio(123, 1, 2, 0);
        var ratio5322_11 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0,
                                                                          3,
                                                                          3,
                                                                          ratio3058_11,
                                                                          ratio123_11,
                                                                          ratio5322_11),
                                 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 1, 0);
        var ratio123 = new PerformanceRatio(123, 1, 2, 0);
        var ratio5322 = new PerformanceRatio(5322, 0xFFFF, 0xFFFF, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 8, 4, ratio3058, ratio123, ratio5322);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        // Module 1 won't be queried
        dataRepository.putObdModule(new OBDModuleInformation(1));

        // Module 3 will respond the same as Module 0
        OBDModuleInformation obdModuleInformation3 = new OBDModuleInformation(3);
        var ratio3058_1_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 1, 1, ratio3058_1_3), 1);

        var ratio3058_9_3 = new PerformanceRatio(3058, 1, 2, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_9_3), 9);

        var ratio3058_11_3 = new PerformanceRatio(3058, 1, 2, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_11_3), 11);

        dataRepository.putObdModule(obdModuleInformation3);

        var ratio3058_3 = new PerformanceRatio(3058, 1, 2, 3);
        var dm20_3 = DM20MonitorPerformanceRatioPacket.create(3, 8, 5, ratio3058_3);
        when(communicationsModule.requestDM20(any(), eq(3))).thenReturn(BusResult.of(dm20_3));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));
        verify(communicationsModule).requestDM20(any(), eq(3));

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.12.9.2.b - Transmission #1 (3) response indicates that the general denominator (SP 3049) is greater by more than 1 when compared to the general denominator received in Part 11 test 5");
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.d - More than one ECU reported DM20 data and general denominators do not match from all ECUs");

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        ActionOutcome expectedOutcome = new ActionOutcome(FAIL,
                                                          "6.12.9.2.b - Transmission #1 (3) response indicates that the general denominator (SP 3049) is greater by more than 1 when compared to the general denominator received in Part 11 test 5");
        ActionOutcome expectedOutcome2 = new ActionOutcome(WARN,
                                                           "6.12.9.3.d - More than one ECU reported DM20 data and general denominators do not match from all ECUs");
        assertEquals(List.of(expectedOutcome, expectedOutcome2), listener.getOutcomes());
    }

    @Test
    public void testFailureForRetryNoNACK() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation2 = new OBDModuleInformation(2);
        obdModuleInformation2.set(DM20MonitorPerformanceRatioPacket.create(2, 3, 3), 11);
        dataRepository.putObdModule(obdModuleInformation2);

        when(communicationsModule.requestDM20(any(), eq(2))).thenReturn(BusResult.empty());

        runTest();

        verify(communicationsModule, times(2)).requestDM20(any(), eq(2));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        FAIL,
                                        "6.12.9.2.a - Retry was required to obtain DM20 response from Turbocharger (2)");

    }

    @Test
    public void testWarningForMonitorDenominatorGreaterThanGeneralDenominator() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 4, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 2, 5, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 2, 5, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.b.i - Engine #1 (0) response shows denominator for monitor SPN 3058 Engine EGR System Monitor is greater than the general denominator");
    }

    @Test
    public void testWarningForGeneralDenominatorGreaterThanIgnitionCycles() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 4, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 4, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 5, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.b.ii - Engine #1 (0) response shows general denominator greater than the ignition cycle counter");
    }

    @Test
    public void testWarningForNumeratorGreaterThanIgnitionCycles() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 5, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 5, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.b.iii - Engine #1 (0) response shows numerator for monitor SPN 3058 Engine EGR System Monitor is greater than the ignition cycle counter");
    }

    @Test
    public void testWarningForRatioNumeratorLessThanPart11() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_11 = new PerformanceRatio(3058, 1, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 2, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.c.i - Engine #1 (0) denominator for monitor SPN 3058 Engine EGR System Monitor is less than the corresponding value in part 11 test");
    }

    @Test
    public void testWarningForRatioNumeratorLessThanPart1() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_11 = new PerformanceRatio(3058, 1, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 0, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.c.i - Engine #1 (0) numerator for monitor SPN 3058 Engine EGR System Monitor is less than the corresponding value in part 11 test");
    }

    @Test
    public void testWarningForRatioDenominatorLessThanPart1() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 5, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 11);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 11);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.a - Engine #1 (0) denominator for monitor SPN 3058 Engine EGR System Monitor is greater than the corresponding value in part 11 test");
    }

    @Test
    public void testWarningForIgnitionCycleLessThanPart1() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);
        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 2, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 7, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 7, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.c.i - Engine #1 (0) ignition cycle counter is less than the corresponding value in in part 11 test");
    }

    @Test
    public void testWarningForDifferentGeneralDenominators() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 11);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 11);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 1, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 5, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        OBDModuleInformation obdModuleInformation3 = new OBDModuleInformation(3);
        var ratio3058_1_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 1, 4, ratio3058_1_3), 11);

        var ratio3058_9_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 4, ratio3058_9_3), 11);

        var ratio3058_11_3 = new PerformanceRatio(3058, 1, 2, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 4, ratio3058_11_3), 11);

        dataRepository.putObdModule(obdModuleInformation3);

        var ratio3058_3 = new PerformanceRatio(3058, 1, 2, 3);
        var dm20_3 = DM20MonitorPerformanceRatioPacket.create(3, 5, 5, ratio3058_3);
        when(communicationsModule.requestDM20(any(), eq(3))).thenReturn(BusResult.of(dm20_3));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));
        verify(communicationsModule).requestDM20(any(), eq(3));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.d - More than one ECU reported DM20 data and general denominators do not match from all ECUs");
    }

    @Test
    public void testWarningForDifferentIgnitionCycles() {
        VehicleInformation vehicleInformation = new VehicleInformation();
        vehicleInformation.setFuelType(FuelType.DSL);
        dataRepository.setVehicleInformation(vehicleInformation);

        OBDModuleInformation obdModuleInformation = new OBDModuleInformation(0);
        var ratio3058_1 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 1, 1, ratio3058_1), 1);

        var ratio3058_9 = new PerformanceRatio(3058, 1, 1, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_9), 9);

        var ratio3058_11 = new PerformanceRatio(3058, 1, 3, 0);
        obdModuleInformation.set(DM20MonitorPerformanceRatioPacket.create(0, 3, 3, ratio3058_11), 11);

        dataRepository.putObdModule(obdModuleInformation);

        var ratio3058 = new PerformanceRatio(3058, 1, 3, 0);
        var dm20 = DM20MonitorPerformanceRatioPacket.create(0, 4, 4, ratio3058);
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20));

        OBDModuleInformation obdModuleInformation3 = new OBDModuleInformation(3);
        var ratio3058_1_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 1, 1, ratio3058_1_3), 1);

        var ratio3058_9_3 = new PerformanceRatio(3058, 1, 1, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_9_3), 9);

        var ratio3058_11_3 = new PerformanceRatio(3058, 1, 3, 3);
        obdModuleInformation3.set(DM20MonitorPerformanceRatioPacket.create(3, 3, 3, ratio3058_11_3), 11);

        dataRepository.putObdModule(obdModuleInformation3);

        var ratio3058_3 = new PerformanceRatio(3058, 1, 3, 3);
        var dm20_3 = DM20MonitorPerformanceRatioPacket.create(3, 5, 4, ratio3058_3);
        when(communicationsModule.requestDM20(any(), eq(3))).thenReturn(BusResult.of(dm20_3));

        runTest();

        verify(communicationsModule).requestDM20(any(), eq(0));
        verify(communicationsModule).requestDM20(any(), eq(3));

        assertEquals("", listener.getMessages());
        assertEquals("", listener.getResults());
        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        WARN,
                                        "6.12.9.3.d - More than one ECU reported DM20 data and ignition cycle counts do not match from all ECUs");
    }
}
