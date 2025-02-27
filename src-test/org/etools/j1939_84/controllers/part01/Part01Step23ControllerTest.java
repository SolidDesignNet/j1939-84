/*
 * Copyright 2021 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part01;

import static org.etools.j1939_84.model.Outcome.FAIL;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executor;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM31DtcToLampAssociation;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * The unit test for {@link Part01Step23Controller}
 *
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class Part01Step23ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 1;
    private static final int PGN = DM31DtcToLampAssociation.PGN;
    private static final int STEP_NUMBER = 23;

    @Mock
    private BannerModule bannerModule;

    @Mock
    private CommunicationsModule communicationsModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    private Part01Step23Controller instance;

    @Mock
    private J1939 j1939;

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    @Before
    public void setUp() {
        listener = new TestResultsListener(mockListener);
        DateTimeModule.setInstance(null);

        instance = new Part01Step23Controller(
                                              executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              communicationsModule,
                                              DateTimeModule.getInstance(),
                                              DataRepository.newInstance());

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

    /**
     * Test method for
     * {@link Part01Step23Controller#run()}.
     */
    @Test
    public void testFail() {

        int[] data = {
                0x61, // SPN least significant bit
                0x02, // SPN most significant bit
                0x13, // Failure mode indicator
                0x81, // SPN Conversion Occurrence Count
                0x62, // Lamp Status/Support
                0x1D, // Lamp Status/State
        };
        DM31DtcToLampAssociation packet = new DM31DtcToLampAssociation(
                                                                       Packet.create(PGN, 0x00, data));

        when(communicationsModule.requestDM31(any()))
                                                     .thenReturn(new RequestResult<>(false,
                                                                                     Collections.singletonList(packet),
                                                                                     Collections.emptyList()));

        runTest();

        verify(communicationsModule).setJ1939(j1939);
        verify(communicationsModule).requestDM31(any());

        verify(mockListener, atLeastOnce()).addOutcome(PART_NUMBER,
                                                       STEP_NUMBER,
                                                       FAIL,
                                                       "6.1.23.2.a - ECU Engine #1 (0) did not report MIL off");

        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());
    }

    /**
     * Test method for
     * {@link org.etools.j1939_84.controllers.StepController#getDisplayName()}.
     */
    @Test
    public void testGetDisplayName() {
        String name = "Part " + PART_NUMBER + " Step " + STEP_NUMBER;
        assertEquals("Display Name", name, instance.getDisplayName());
    }

    /**
     * Test method for
     * {@link org.etools.j1939_84.controllers.StepController#getPartNumber()}.
     */
    @Test
    public void testGetPartNumber() {
        assertEquals("Part Number", PART_NUMBER, instance.getPartNumber());
    }

    /**
     * Test method for
     * {@link org.etools.j1939_84.controllers.StepController#getStepNumber()}.
     */
    @Test
    public void testGetStepNumber() {
        assertEquals(STEP_NUMBER, instance.getStepNumber());
    }

    /**
     * Test method for
     * {@link org.etools.j1939_84.controllers.StepController#getTotalSteps()}.
     */
    @Test
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 0, instance.getTotalSteps());
    }

    /**
     * Test method for
     * {@link Part01Step23Controller#run()}.
     */
    @Test
    public void testRun() {

        int[] data = {
                0x61, // SPN least significant bit
                0x02, // SPN most significant bit
                0x13, // Failure mode indicator
                0x81, // SPN Conversion Occurrence Count
                0x00, // Lamp Status/Support
                0xFF, // Lamp Status/State
        };
        DM31DtcToLampAssociation packet = new DM31DtcToLampAssociation(
                                                                       Packet.create(PGN, 0x00, data));

        when(communicationsModule.requestDM31(any()))
                                                     .thenReturn(new RequestResult<>(false,
                                                                                     Collections.singletonList(packet),
                                                                                     Collections.emptyList()));

        runTest();

        verify(communicationsModule).setJ1939(j1939);
        verify(communicationsModule).requestDM31(any());

        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());
    }
}
