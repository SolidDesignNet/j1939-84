/*
 * Copyright 2021 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part03;

import static org.etools.j1939tools.j1939.packets.AcknowledgmentPacket.Response.NACK;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939tools.j1939.Lookup;
import org.etools.j1939tools.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939tools.j1939.packets.DM31DtcToLampAssociation;
import org.etools.j1939tools.j1939.packets.DM6PendingEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DTCLampStatus;
import org.etools.j1939tools.modules.CommunicationsModule;
import org.etools.j1939tools.modules.DateTimeModule;

/**
 * 6.3.5 DM31: DTC to lamp association
 */
public class Part03Step05Controller extends StepController {

    private static final int PART_NUMBER = 3;
    private static final int STEP_NUMBER = 5;
    private static final int TOTAL_STEPS = 0;

    Part03Step05Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
             new BannerModule(),
             DateTimeModule.getInstance(),
             DataRepository.getInstance(),
             new EngineSpeedModule(),
             new VehicleInformationModule(),
             new CommunicationsModule());
    }

    Part03Step05Controller(Executor executor,
                           BannerModule bannerModule,
                           DateTimeModule dateTimeModule,
                           DataRepository dataRepository,
                           EngineSpeedModule engineSpeedModule,
                           VehicleInformationModule vehicleInformationModule,
                           CommunicationsModule communicationsModule) {
        super(executor,
              bannerModule,
              dateTimeModule,
              dataRepository,
              engineSpeedModule,
              vehicleInformationModule,
              communicationsModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
    }

    @Override
    protected void run() throws Throwable {

        getDataRepository().getObdModuleAddresses()
                           .stream()
                           .filter(a -> !getDTCs(DM6PendingEmissionDTCPacket.class, a, 3).isEmpty())
                           .forEach(moduleAddress -> {
                               String moduleName = Lookup.getAddressName(moduleAddress);

                               // 6.3.5.1.a DS DM31 (send Request (PGN 59904) for PGN 41728 (SPNs 1214-1215, 4113,
                               // 4117)) to ECU with DM6 pending DTC.
                               var response = getCommunicationsModule().requestDM31(getListener(), moduleAddress);

                               List<DM31DtcToLampAssociation> packets = response.getPackets();
                               if (!packets.isEmpty()) {
                                   // 6.3.5.2.a (if supported) Fail if MIL not reported as off in all returned DTCs. See
                                   // section A.8 for allowed values.
                                   boolean milNotOff = packets
                                                              .stream()
                                                              .flatMap(p -> p.getDtcLampStatuses().stream())
                                                              .map(DTCLampStatus::getMalfunctionIndicatorLampStatus)
                                                              .anyMatch(this::isNotOff);
                                   if (milNotOff) {
                                       addFailure("6.3.5.2.a - " + moduleName
                                               + " did not report MIL 'off' in all returned DTCs");
                                   }
                               } else {
                                   // 6.3.5.2.b (if supported) Fail if NACK not received from OBD ECUs that did not
                                   // provided a DM31 message
                                   boolean nackReceived = response.getAcks()
                                                                  .stream()
                                                                  .filter(a -> a.getSourceAddress() == moduleAddress)
                                                                  .map(AcknowledgmentPacket::getResponse)
                                                                  .anyMatch(r -> r == NACK);
                                   if (!nackReceived) {
                                       addFailure("6.3.5.2.b - " + moduleName
                                               + " did not provide a DM31 and did not NACK the request");
                                   }
                               }
                           });

    }

}
