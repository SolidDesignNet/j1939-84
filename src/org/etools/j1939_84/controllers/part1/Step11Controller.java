/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.etools.j1939_84.bus.j1939.BusResult;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket.Response;
import org.etools.j1939_84.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DiagnosticReadinessModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 * <p>
 * The controller for 6.1.11 DM21: Diagnostic readiness 2
 */

public class Step11Controller extends StepController {

    private static final int PART_NUMBER = 1;
    private static final int STEP_NUMBER = 11;
    private static final int TOTAL_STEPS = 1;

    private final DataRepository dataRepository;
    private final DiagnosticReadinessModule diagnosticReadinessModule;

    Step11Controller(DataRepository dataRepository) {
        this(Executors.newSingleThreadScheduledExecutor(),
             new EngineSpeedModule(),
             new BannerModule(),
             new DiagnosticReadinessModule(),
             new VehicleInformationModule(),
             dataRepository);
    }

    Step11Controller(Executor executor,
                     EngineSpeedModule engineSpeedModule,
                     BannerModule bannerModule,
                     DiagnosticReadinessModule diagnosticReadinessModule,
                     VehicleInformationModule vehicleInformationModule,
                     DataRepository dataRepository) {
        super(executor,
              engineSpeedModule,
              bannerModule,
              vehicleInformationModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
        this.diagnosticReadinessModule = diagnosticReadinessModule;
        this.dataRepository = dataRepository;
    }

    @Override
    protected void run() throws Throwable {
        diagnosticReadinessModule.setJ1939(getJ1939());
        // 6.1.11.1.a. Global DM21 (send Request (PGN 59904) for PGN 49408
        List<DM21DiagnosticReadinessPacket> globalDm21Packets = diagnosticReadinessModule
                .requestDM21Packets(getListener(), true).getPackets();

        globalDm21Packets.forEach(packet -> {
            // 6.1.11.2.a. Fail if any ECU reports distance with MIL on (SPN 3069) is not zero.
            if (packet.getKmSinceDTCsCleared() != 0 || packet.getMilesSinceDTCsCleared() != 0) {
                addFailure(1,
                           11,
                           "6.1.11.1.a - An ECU reported distance with MIL on (SPN 3069) is not zero");
            }
            // 6.1.11.2.b. Fail if any ECU reports distance SCC (SPN 3294) is not zero.
            if (packet.getKmWhileMILIsActivated() != 0 || packet.getMilesWhileMILIsActivated() != 0) {
                addFailure(1,
                           11,
                           "6.1.11.1.b - An ECU reported distance SCC (SPN 3294) is not zero");
            }
            // 6.1.11.2.c. Fail if any ECU reports time with MIL on (SPN 3295) is not zero (if supported)
            if (packet.getMinutesWhileMILIsActivated() != 0) {
                addFailure(1,
                           11,
                           "6.1.11.1.c - An ECU reported time with MIL on (SPN 3295) is not zero");
            }
            // 6.1.11.2.d. Fail if any ECU reports time SCC (SPN 3296) > 1 minute (if supported).
            if (packet.getMinutesSinceDTCsCleared() > 1) {
                addFailure(1,
                           11,
                           "6.1.11.1.d - An ECU reported time SCC (SPN 3296) > 1 minute");
            }
        });
        // 6.1.11.2.e. Fail if no OBD ECU provides a DM21 message.
        if (globalDm21Packets.isEmpty()) {
            addFailure(1, 11, "6.1.11.1.e - No OBD ECU provided a DM21 message");
        }

        // 6.1.11.3.a. DS DM21 to each OBD ECU
        List<BusResult<DM21DiagnosticReadinessPacket>> addressSpecificDM21Results = dataRepository
                .getObdModuleAddresses().stream()
                .map(addr -> diagnosticReadinessModule.getDM21Packets(getListener(), true, addr))
                .collect(Collectors.toList());

        // ignore missing responses and NACKs
        List<DM21DiagnosticReadinessPacket> addressSpecificDm21Packets = addressSpecificDM21Results.stream()
                .flatMap(r -> r.getPacket().stream())
                .flatMap(r -> r.left.stream())
                .collect(Collectors.toList());
        addressSpecificDm21Packets
                .forEach(packet -> {
                    // 6.1.11.4.a. Fail if any ECU reports distance with MIL on (SPN 3069) is not zero.
                    if (packet.getKmSinceDTCsCleared() != 0 || packet.getMilesSinceDTCsCleared() != 0) {
                        addFailure(1,
                                   11,
                                   "6.1.11.4.a - An ECU reported distance with MIL on (SPN 3069) is not zero");
                    }
                    // 6.1.11.4.b. Fail if any ECU reports distance SCC (SPN 3294) is not zero.
                    if (packet.getKmWhileMILIsActivated() != 0 || packet.getMilesWhileMILIsActivated() != 0) {
                        addFailure(1,
                                   11,
                                   "6.1.11.4.b. - An ECU reported distance SCC (SPN 3294) is not zero");
                    }
                    // 6.1.11.4.c. Fail if any ECU reports time with MIL on (SPN 3295) is not zero (if supported)
                    if (packet.getMinutesWhileMILIsActivated() != 0) {
                        addFailure(1,
                                   11,
                                   "6.1.11.4.c - An ECU reported time with MIL on (SPN 3295) is not zero");
                    }
                    // 6.1.11.4.d. Fail if any ECU reports time SCC (SPN 3296) > 1 minute (if supported).
                    if (packet.getMinutesSinceDTCsCleared() != 0) {
                        addFailure(1,
                                   11,
                                   "6.1.11.4.d - An ECU reported time SCC (SPN 3296) > 1 minute");
                    }
                });

        // 6.1.11.4.e. Fail if any responses differ from global responses.
        List<DM21DiagnosticReadinessPacket> results = new ArrayList<>();

        if (addressSpecificDm21Packets.size() > globalDm21Packets.size()) {
            results.addAll(addressSpecificDm21Packets);
            results.removeAll(globalDm21Packets);
        } else {
            results.addAll(globalDm21Packets);
            results.removeAll(addressSpecificDm21Packets);
        }

        if (!results.isEmpty()) {
            addFailure(1,
                       11,
                       "6.1.11.4.e - DS responses differ from global responses");
        }

        // 6.1.11.4.f. Fail if NACK not received from OBD ECUs that did not respond to global query.
        addressSpecificDm21Packets.forEach(addressPacket -> globalDm21Packets
                .removeIf(globalPacket -> globalPacket.getSourceAddress() == addressPacket.getSourceAddress()));

        addressSpecificDM21Results.stream()
                // for each bus result
                .flatMap(br -> br.getPacket().stream())
                // only consider the NACKs
                .flatMap(e -> e.right.stream())
                .filter(a -> a.getResponse() == Response.NACK)
                .forEach(nackPacket -> globalDm21Packets
                        .removeIf(globalPacket -> globalPacket.getSourceAddress() == nackPacket.getSourceAddress()));

        if (!globalDm21Packets.isEmpty()) {
            addFailure(1,
                       11,
                       "6.1.11.4.f - NACK not received from OBD ECUs that did not respond to global query");
        }

    }

}
