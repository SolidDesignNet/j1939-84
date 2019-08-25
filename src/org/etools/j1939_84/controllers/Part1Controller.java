/**
 * Copyright (c) 2019. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.controllers.ResultsListener.MessageType.WARNING;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket.Response;
import org.etools.j1939_84.bus.j1939.packets.DM24SPNSupportPacket;
import org.etools.j1939_84.bus.j1939.packets.DM5DiagnosticReadinessPacket;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.bus.j1939.packets.VehicleIdentificationPacket;
import org.etools.j1939_84.controllers.ResultsListener.MessageType;
import org.etools.j1939_84.model.OBDModuleInformation;
import org.etools.j1939_84.model.RequestResult;
import org.etools.j1939_84.model.VehicleInformation;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticReadinessModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.OBDTestsModule;
import org.etools.j1939_84.modules.SupportedSpnModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939_84.utils.VinDecoder;

/**
 * The {@link Controller} for the Part 1 Tests
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class Part1Controller extends Controller {

    private final DiagnosticReadinessModule diagnosticReadinessModule;

    /**
     * Map of OBD Module Source Address to {@link OBDModuleInformation}
     */
    private final Map<Integer, OBDModuleInformation> obdModules = new HashMap<>();

    private final OBDTestsModule obdTestsModule;

    private final SupportedSpnModule supportedSpnModule;

    private VehicleInformation vehicleInformation;

    private final VinDecoder vinDecoder;

    /**
     * Constructor
     */
    public Part1Controller() {
        this(Executors.newSingleThreadScheduledExecutor(), new EngineSpeedModule(), new BannerModule(),
                new DateTimeModule(), new VehicleInformationModule(), new DiagnosticReadinessModule(),
                new OBDTestsModule(), new SupportedSpnModule(), new VinDecoder());
    }

    /**
     * Constructor exposed for testing
     *
     * @param executor                 the {@link ScheduledExecutorService}
     * @param engineSpeedModule        the {@link EngineSpeedModule}
     * @param bannerModule             the {@link BannerModule}
     * @param dateTimeModule           the {@link DateTimeModule}
     * @param vehicleInformationModule the {@link VehicleInformationModule}
     * @param obdTestsModule           the {@link OBDTestsModule}
     * @param supportedSpnModule       the {@link SupportedSpnModule}
     * @param vinDecoder               the {@link VinDecoder}
     */
    public Part1Controller(ScheduledExecutorService executor, EngineSpeedModule engineSpeedModule,
            BannerModule bannerModule, DateTimeModule dateTimeModule, VehicleInformationModule vehicleInformationModule,
            DiagnosticReadinessModule diagnosticReadinessModule, OBDTestsModule obdTestsModule,
            SupportedSpnModule supportedSpnModule, VinDecoder vinDecoder) {
        super(executor, engineSpeedModule, bannerModule, dateTimeModule, vehicleInformationModule);
        this.diagnosticReadinessModule = diagnosticReadinessModule;
        this.obdTestsModule = obdTestsModule;
        this.supportedSpnModule = supportedSpnModule;
        this.vinDecoder = vinDecoder;
    }

    /**
     * Sends the request to the UI to gather vehicle information from the user.
     *
     * @throws InterruptedException if the cancelled the operation
     */
    private void collectVehicleInformation() throws InterruptedException {
        getListener().onVehicleInformationNeeded(vehInfo -> {
            if (vehInfo == null) {
                try {
                    setEnding(Ending.ABORTED);
                } catch (InterruptedException e) {
                    // This will be caught later.
                }
            } else {
                vehicleInformation = vehInfo;
            }
        });

        while (vehicleInformation == null) {
            Thread.sleep(500);
            updateProgress("6.1.1. Collecting Vehicle Information"); // To check for test aborted
        }
    }

    /**
     * Displays a warning message to the user.
     */
    private void displayWarningMessage() {
        String message = "Ready to begin Part 1" + NL;
        message += "a. Confirm the vehicle is in a safe location and condition for the test." + NL;
        message += "b. Confirm that the vehicle battery is well charged. (Battery voltage >> 12 volts)." + NL;
        message += "c. Confirm the vehicle condition and operator control settings according to the engine manufacturer’s instructions."
                + NL;

        getListener().onUrgentMessage(message, "Start Part 1", MessageType.WARNING);
    }

    /**
     * Ensures the Key is on with the Engine Off and prompts the user to make the
     * proper adjustments.
     *
     * @throws InterruptedException if the user cancels the operation
     */
    private void ensureKeyOnEngineOff() throws InterruptedException {
        if (!getEngineSpeedModule().isEngineNotRunning()) {
            getListener().onUrgentMessage("Please turn the Engine OFF with Key ON.", "Adjust Key Switch", WARNING);

            while (!getEngineSpeedModule().isEngineNotRunning() && getEnding() == null) {
                updateProgress("Waiting for Key ON, Engine OFF...");
                Thread.sleep(500);
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Part 1 Test";
    }

    @Override
    protected int getTotalSteps() {
        // TODO Auto-generated method stub
        return 1;
    }

    /**
     * Queries the vehicle to determine which modules are ODB ECUs and validates the
     * results
     */
    private void queryAndValidateOdbEcus() {

        List<ParsedPacket> packets = diagnosticReadinessModule.requestDM5Packets(getListener(), true);

        boolean nacked = packets.stream().anyMatch(packet -> packet instanceof AcknowledgmentPacket
                && ((AcknowledgmentPacket) packet).getResponse() == Response.NACK);
        if (nacked) {
            addFailure("6.1.3.2.b - The request for DM5 was NACK'ed");
        }

        Stream<DM5DiagnosticReadinessPacket> dm5Packets = packets.stream()
                .filter(p -> p instanceof DM5DiagnosticReadinessPacket)
                .map(p -> (DM5DiagnosticReadinessPacket) p);

        dm5Packets.filter(p -> p.isObd()).forEach(p -> {
            OBDModuleInformation info = new OBDModuleInformation(p.getSourceAddress());
            info.setObdCompliance(p.getOBDCompliance());
        });

        if (obdModules.isEmpty()) {
            addFailure("6.1.3.2.a - There needs to be at least one OBD Module");
        }

        long distinctCount = new HashSet<>(obdModules.values()).size();
        if (distinctCount > 1) {
            // All the values should be the same
            addWarning(
                    "6.1.3.3.a - An ECU responded with a value for OBD Compliance that was not identical to other ECUs");
        }
    }

    /**
     * Queries the vehicle to collect the Supported SPNs. Also validates the query.
     */
    private void queryAndValidateSupportedSPNs() {
        RequestResult<DM24SPNSupportPacket> result = obdTestsModule.requestSupportedSpnPackets(getListener(),
                obdModules.keySet());

        if (result.isRetryUsed()) {
            addFailure("6.1.4.2.a - Retry was required to obtain DM24 response.");
        }

        result.getPackets().stream().forEach(p -> {
            OBDModuleInformation info = obdModules.get(p.getSourceAddress());
            info.setSupportedSpns(p.getSupportedSpns());
        });

        Set<Integer> dataStreamSpns = obdModules.values().stream().map(info -> info.getDataStreamSpns())
                .flatMap(spns -> spns.stream()).map(s -> s.getSpn()).collect(Collectors.toSet());

        boolean dataStreamOk = supportedSpnModule
                .validateDataStreamSpns(getListener(), dataStreamSpns, vehicleInformation.getFuelType());
        if (!dataStreamOk) {
            addFailure("6.1.4.2.b - One or more SPNs for data stream is not supported");
        }

        Set<Integer> freezeFrameSpns = obdModules.values().stream().map(info -> info.getFreezeFrameSpns())
                .flatMap(spns -> spns.stream()).map(s -> s.getSpn()).collect(Collectors.toSet());
        boolean freezeFrameOk = supportedSpnModule.validateFreezeFrameSpns(getListener(), freezeFrameSpns);
        if (!freezeFrameOk) {
            addFailure("6.1.4.2.c - One or more SPNs for freeze frame are not supported");
        }
    }

    /**
     * Queries the vehicle for the VIN and validates the result
     */
    private void queryAndValidateVIN() {
        List<VehicleIdentificationPacket> packets = getVehicleInformationModule().requestVehicleIdentification();
        if (packets.isEmpty()) {
            addFailure("6.1.5.2.a - No VIN was provided");
        }

        long obdResponses = packets.stream().filter(p -> obdModules.containsKey(p.getSourceAddress())).count();
        if (obdResponses > 1) {
            addFailure("6.1.5.2.b - More than one OBD ECU responded with VIN");
        }

        VehicleIdentificationPacket packet = packets.get(0);
        String vin = packet.getVin();
        if (!vehicleInformation.getVin().equals(vin)) {
            addFailure("6.1.5.2.c - VIN does not match user entered VIN");
        }

        if (vinDecoder.getModelYear(vin) != vehicleInformation.getVehicleModelYear()) {
            addFailure("6.1.5.2.d - VIN Model Year does not match user entered Vehicle Model Year");
        }

        if (!vinDecoder.isVinValid(vin)) {
            addFailure("6.1.5.2.e - VIN is not valid (not 17 legal chars, incorrect checksum, or non-numeric sequence");
        }

        long nonObdResponses = packets.stream().filter(p -> !obdModules.containsKey(p.getSourceAddress())).count();
        if (nonObdResponses > 0) {
            addWarning("6.1.5.3.a - Non-OBD ECU responded with VIN");
        }

        long respondingSources = packets.stream().mapToInt(p -> p.getSourceAddress()).distinct().count();
        if (packets.size() > respondingSources) {
            addWarning("6.1.5.3.b - More than one VIN response from an ECU");
        }

        if (nonObdResponses > 1) {
            addWarning("6.1.5.3.c - VIN provided from more than one non-OBD ECU");
        }

        if (!packet.getManufacturerData().isEmpty()) {
            addWarning("6.1.5.3.d - Manufacturer defined data follows the VIN");
        }
    }

    @Override
    protected void run() throws Throwable {
        diagnosticReadinessModule.setJ1939(getJ1939());
        obdTestsModule.setJ1939(getJ1939());

        incrementProgress("6.1.1.1.a-c Displaying Warning Message");
        displayWarningMessage();

        incrementProgress("6.1.1.1.d - Ensuring Key On, Engine Off");
        ensureKeyOnEngineOff();

        incrementProgress("6.1.1.1.e - Collecting Vehicle Information");
        collectVehicleInformation();

        incrementProgress("6.1.3 - DM5 Diagnostic Readiness 1");
        queryAndValidateOdbEcus();

        incrementProgress("6.1.4 - DM24: SPN Support");
        queryAndValidateSupportedSPNs();

        incrementProgress("6.1.5 - PGN 65260 VIN Verification");
        queryAndValidateVIN();

    }

}
