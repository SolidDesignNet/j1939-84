/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part01;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.controllers.QuestionListener.AnswerType.NO;
import static org.etools.j1939_84.controllers.QuestionListener.AnswerType.YES;
import static org.etools.j1939_84.controllers.ResultsListener.MessageType.WARNING;
import static org.etools.j1939_84.model.KeyState.KEY_OFF;
import static org.etools.j1939_84.model.KeyState.KEY_ON_ENGINE_OFF;
import static org.etools.j1939_84.model.Outcome.ABORT;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.QuestionListener;
import org.etools.j1939_84.controllers.ResultsListener;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.controllers.TestResultsListener;
import org.etools.j1939_84.model.VehicleInformation;
import org.etools.j1939_84.model.VehicleInformationListener;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.ReportFileModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.AbstractControllerTest;
import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.J1939;
import org.etools.j1939tools.j1939.model.FuelType;
import org.etools.j1939tools.j1939.packets.AddressClaimPacket;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;
import org.etools.testdoc.TestDoc;
import org.etools.testdoc.TestItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The unit test for {@link Part01Step01Controller}
 *
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
@TestDoc(description = "Part 1 Step 1 KOEO Data Collection")
public class Part01Step01ControllerTest extends AbstractControllerTest {
    private static final int PART_NUMBER = 1;
    private static final int STEP_NUMBER = 1;
    @Mock
    private BannerModule bannerModule;

    @Mock
    private CommunicationsModule communicationsModule;

    @Mock
    private EngineSpeedModule engineSpeedModule;

    @Mock
    private Executor executor;

    private Part01Step01Controller instance;

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

        instance = new Part01Step01Controller(executor,
                                              engineSpeedModule,
                                              bannerModule,
                                              vehicleInformationModule,
                                              communicationsModule,
                                              DataRepository.newInstance(),
                                              DateTimeModule.getInstance());

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
     * Test method for {@link Part01Step01Controller#getDisplayName()}.
     */
    @Test
    @TestDoc(value = @TestItem(verifies = "6.1.1", description = "Verifies part and step name for report"))
    public void testGetDisplayName() {
        assertEquals("Display Name", "Part 1 Step 1", instance.getDisplayName());
    }

    /**
     * Test method for {@link StepController#getPartNumber()}.
     */
    @Test
    public void testGetPartNumber() {
        assertEquals("Part Number", PART_NUMBER, instance.getPartNumber());
    }

    /**
     * Test method for {@link StepController#getStepNumber()}.
     */
    @Test
    public void testGetStepNumber() {
        assertEquals(STEP_NUMBER, instance.getStepNumber());
    }

    /**
     * Test method for {@link Part01Step01Controller#getTotalSteps()}.
     */
    @Test
    @TestDoc(value = @TestItem(verifies = "6.1.1", description = "Verifies that there is a single 6.1.1 step"))
    public void testGetTotalSteps() {
        assertEquals("Total Steps", 3, instance.getTotalSteps());
    }

    /**
     * Test method for {@link Part01Step01Controller#run()}.
     */
    @Test
    @TestDoc(value = {
            @TestItem(verifies = "6.1.1.1.a,b,c", dependsOn = "UserInterfacePresenterTest", description = "Verify that the UI presents the data."),
            @TestItem(verifies = "6.1.1.1.d", dependsOn = "EngineSpeedModuleTest"),
            @TestItem(verifies = "6.1.1.1", dependsOn = { "VehicleInformationTest", "VehicleInformationModuleTest" }),
            @TestItem(verifies = "6.1.1.1.e.i", dependsOn = { "VehicleInformationTest",
                    "VehicleInformationModuleTest.testGetVin",
                    "VehicleInformationModuleTest.testGetVinNoResponse",
                    "VehicleInformationModuleTest.testGetVinWithDifferentResponses",
                    "VehicleInformationModuleTest.testReportVin",
                    "VehicleInformationModuleTest.testReportVinWithNoResponses", "VinDecoderTest" }),
            @TestItem(verifies = "6.1.1.1.e.ii", dependsOn = { "VinDecoderTest" }),
            @TestItem(verifies = "6.1.1.1.e.iv", dependsOn = { "FuelTypeTest" }),
            @TestItem(verifies = "6.1.1.1.e.v", dependsOn = { "VehicleInformationTest",
                    "VehicleInformationModuleTest.testReportCalibrationInformation",
                    "VehicleInformationModuleTest.testReportCalibrationInformationWithNoResponses",
                    "VehicleInformationModuleTest.testRunHappyPath",
                    "VehicleInformationModuleTest.testRunNoModulesRespond",
                    "VehicleInformationModuleTest.testRunWithWarningsAndFailures" }),
    }, description = "Verify vehicle data collection and that the correct instructions to the user are transmitted to the UI presenter.")
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    public void testRun() {

        String expectedTitle = "Step 6.1.1.1.a, b & c";
        VehicleInformation vehicleInfo = new VehicleInformation();
        vehicleInfo.setFuelType(FuelType.DSL);
        AddressClaimPacket addressClaimPacket = mock(AddressClaimPacket.class);
        RequestResult<AddressClaimPacket> requestResult = new RequestResult<>(false, addressClaimPacket);
        vehicleInfo.setAddressClaim(requestResult);

        DataRepository.getInstance().setVehicleInformation(vehicleInfo);
        when(engineSpeedModule.getKeyState()).thenReturn(KEY_OFF, KEY_ON_ENGINE_OFF);
        when(engineSpeedModule.getEngineSpeedAsString()).thenReturn("0.0 RPMs");

        ArgumentCaptor<QuestionListener> questionCaptor = ArgumentCaptor.forClass(QuestionListener.class);
        runTest();

        String urgentMessages = "a. Confirm the vehicle is in a safe location and condition for the test" + NL;
        urgentMessages += "b. Confirm the vehicle battery is well charged. (Battery voltage >> 12 volts)" + NL;
        urgentMessages += "c. Confirm the vehicle condition and operator control settings according to the engine manufacturer’s instructions"
                + NL + NL;
        urgentMessages += "Please press OK to continue";

        verify(engineSpeedModule, times(2)).getKeyState();
        verify(engineSpeedModule, atLeastOnce()).getEngineSpeedAsString();
        verify(mockListener).onUrgentMessage(eq("Please turn the key on with the engine off"),
                                             eq("Step 6.1.1.1.d"),
                                             eq(WARNING),
                                             questionCaptor.capture());
        questionCaptor.getValue().answered(YES);

        verify(mockListener).onUrgentMessage(eq(urgentMessages), eq(expectedTitle), eq(WARNING), any());
        verify(mockListener).onVehicleInformationReceived(vehicleInfo);

        var vehicleInfoCaptor = ArgumentCaptor.forClass(VehicleInformationListener.class);
        verify(mockListener).onVehicleInformationNeeded(vehicleInfoCaptor.capture());
        vehicleInfoCaptor.getValue().onResult(vehicleInfo);

        String expectedMessages = "Step 6.1.1.1.a - Vehicle Data Collection" + NL;
        expectedMessages += "Step 6.1.1.1.d - Waiting for key on with engine off" + NL;
        expectedMessages += "Step 6.1.1.1.e - Collecting Vehicle Information";
        assertEquals(expectedMessages, listener.getMessages());

        String expectedResults = "Initial Engine Speed = 0.0 RPMs" + NL;
        expectedResults += "Final Engine Speed = 0.0 RPMs" + NL;
        expectedResults += vehicleInfo + NL;
        assertEquals(expectedResults, listener.getResults());
    }

    @Test
    public void testCancelButton() {

        String expectedTitle = "Step 6.1.1.1.a, b & c";
        VehicleInformation vehicleInfo = new VehicleInformation();
        vehicleInfo.setFuelType(FuelType.DSL);
        AddressClaimPacket addressClaimPacket = mock(AddressClaimPacket.class);
        RequestResult<AddressClaimPacket> requestResult = new RequestResult<>(false, addressClaimPacket);
        vehicleInfo.setAddressClaim(requestResult);

        DataRepository.getInstance().setVehicleInformation(vehicleInfo);
        when(engineSpeedModule.getKeyState()).thenReturn(KEY_OFF, KEY_ON_ENGINE_OFF);
        when(engineSpeedModule.getEngineSpeedAsString()).thenReturn("0.0 RPMs");

        ArgumentCaptor<QuestionListener> questionCaptor = ArgumentCaptor.forClass(QuestionListener.class);
        runTest();

        String urgentMessages = "a. Confirm the vehicle is in a safe location and condition for the test" + NL;
        urgentMessages += "b. Confirm the vehicle battery is well charged. (Battery voltage >> 12 volts)" + NL;
        urgentMessages += "c. Confirm the vehicle condition and operator control settings according to the engine manufacturer’s instructions"
                + NL + NL;
        urgentMessages += "Please press OK to continue";

        verify(engineSpeedModule, times(2)).getKeyState();
        verify(engineSpeedModule, atLeastOnce()).getEngineSpeedAsString();
        verify(mockListener).onUrgentMessage(eq("Please turn the key on with the engine off"),
                                             eq("Step 6.1.1.1.d"),
                                             eq(WARNING),
                                             questionCaptor.capture());
        questionCaptor.getValue().answered(NO);

        verify(mockListener).addOutcome(PART_NUMBER, STEP_NUMBER, ABORT, "User cancelled testing at Part 1 Step 1");
        verify(mockListener).onUrgentMessage(eq(urgentMessages), eq(expectedTitle), eq(WARNING), any());
        verify(mockListener).onVehicleInformationReceived(vehicleInfo);

        var vehicleInfoCaptor = ArgumentCaptor.forClass(VehicleInformationListener.class);
        verify(mockListener).onVehicleInformationNeeded(vehicleInfoCaptor.capture());
        vehicleInfoCaptor.getValue().onResult(vehicleInfo);

        String expectedMessages = "Step 6.1.1.1.a - Vehicle Data Collection" + NL;
        expectedMessages += "Step 6.1.1.1.d - Waiting for key on with engine off" + NL;
        expectedMessages += "Step 6.1.1.1.e - Collecting Vehicle Information" + NL;
        expectedMessages += "User cancelled testing at Part 1 Step 1";
        assertEquals(expectedMessages, listener.getMessages());

        String expectedResults = "Initial Engine Speed = 0.0 RPMs" + NL;
        expectedResults += "Final Engine Speed = 0.0 RPMs" + NL;
        expectedResults += vehicleInfo + NL;
        assertEquals(expectedResults, listener.getResults());
    }

    @Test
    @TestDoc(value = {
            @TestItem(verifies = "6.1.1.1.a,b,c", dependsOn = "UserInterfacePresenterTest"),
            @TestItem(verifies = "6.1.1.1.d", dependsOn = "EngineSpeedModuleTest"),
            @TestItem(verifies = "6.1.1.1.e", dependsOn = { "VehicleInformationTest",
                    "VehicleInformationModuleTest" }) }, description = "After the key was detected off, notify user to 'Please turn the Engine OFF with Key ON.', then continue with data collection.")
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The method is called just to get some exception.")
    public void testWaitForKey() {
        when(engineSpeedModule.getKeyState()).thenReturn(KEY_OFF);
        when(engineSpeedModule.getEngineSpeedAsString()).thenReturn("0.0 RPMs");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                instance.stop();
            }
        }, 500);

        runTest();

        verify(engineSpeedModule, atLeastOnce()).getKeyState();
        verify(engineSpeedModule, atLeastOnce()).getEngineSpeedAsString();

        String urgentMessages = "a. Confirm the vehicle is in a safe location and condition for the test" + NL;
        urgentMessages += "b. Confirm the vehicle battery is well charged. (Battery voltage >> 12 volts)" + NL;
        urgentMessages += "c. Confirm the vehicle condition and operator control settings according to the engine manufacturer’s instructions"
                + NL + NL;
        urgentMessages += "Please press OK to continue";
        verify(mockListener).onUrgentMessage(eq(urgentMessages), eq("Step 6.1.1.1.a, b & c"), eq(WARNING), any());
        verify(mockListener).onUrgentMessage(eq("Please turn the key on with the engine off"),
                                             eq("Step 6.1.1.1.d"),
                                             eq(WARNING),
                                             any());

        String expectedMessages = "Step 6.1.1.1.a - Vehicle Data Collection" + NL;
        expectedMessages += "Step 6.1.1.1.d - Waiting for key on with engine off" + NL;
        expectedMessages += "Step 6.1.1.1.d - Waiting for key on with engine off...";
        assertEquals(expectedMessages, listener.getMessages());

        String expectedResults = "Initial Engine Speed = 0.0 RPMs" + NL;
        assertEquals(expectedResults, listener.getResults());

        verify(mockListener).addOutcome(PART_NUMBER,
                                        STEP_NUMBER,
                                        ABORT,
                                        "User cancelled testing at Part 1 Step 1");
    }

}
