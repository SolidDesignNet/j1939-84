/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part03;

import static org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket.PGN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Part03Step14ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 3;
    private static final int STEP_NUMBER = 14;

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

    private TestResultsListener listener;

    @Mock
    private ResultsListener mockListener;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private VehicleInformationModule vehicleInformationModule;

    private DataRepository dataRepository;

    private StepController instance;

    @Before
    public void setUp() throws Exception {
        DateTimeModule.setInstance(new TestDateTimeModule());
        DateTimeModule dateTimeModule = DateTimeModule.getInstance();
        dataRepository = DataRepository.newInstance();

        listener = new TestResultsListener(mockListener);
        instance = new Part03Step14Controller(executor,
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
                                 engineSpeedModule,
                                 bannerModule,
                                 vehicleInformationModule,
                                 mockListener,
                                 communicationsModule);
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
    public void testRun() {

        int[] data = {
                0x04, // Ignition Cycle Counter
                0x00, // Ignition Cycle Counter
                0x5A, // OBD Monitoring Conditions Encountered
                0x5A, // OBD Monitoring Conditions Encountered

                0x11, // SPN of Applicable System Monitor
                0x11, // SPN of Applicable System Monitor
                0x11, // SPN of Applicable System Monitor
                0xAA, // Applicable System Monitor Numerator
                0xAA, // Applicable System Monitor Numerator
                0xBB, // Applicable System Monitor Denominator
                0xBB, // Applicable System Monitor Denominator
        };
        var dm20Packet = new DM20MonitorPerformanceRatioPacket(Packet.create(PGN, 0x00, data));
        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.of(dm20Packet));

        dataRepository.putObdModule(new OBDModuleInformation(0));

        runTest();

        assertSame(dm20Packet, dataRepository.getObdModule(0).get(DM20MonitorPerformanceRatioPacket.class, 3));
        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());

        verify(communicationsModule).requestDM20(any(), eq(0));
    }

    @Test
    public void testEmptyPackets() {

        when(communicationsModule.requestDM20(any(), eq(0))).thenReturn(BusResult.empty());

        dataRepository.putObdModule(new OBDModuleInformation(0));

        runTest();

        assertNull(dataRepository.getObdModule(0).get(DM20MonitorPerformanceRatioPacket.class, 3));

        assertEquals("", listener.getResults());
        assertEquals("", listener.getMessages());

        verify(communicationsModule).requestDM20(any(), eq(0));
    }
}
