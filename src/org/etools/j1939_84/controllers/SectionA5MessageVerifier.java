/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */

package org.etools.j1939_84.controllers;

import static org.etools.j1939tools.j1939.packets.LampStatus.OFF;

import java.util.Collection;
import java.util.List;

import org.etools.j1939_84.modules.VehicleInformationModule;
import org.etools.j1939tools.bus.BusResult;
import org.etools.j1939tools.j1939.Lookup;
import org.etools.j1939tools.j1939.packets.CompositeSystem;
import org.etools.j1939tools.j1939.packets.DM12MILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM20MonitorPerformanceRatioPacket;
import org.etools.j1939tools.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.DM23PreviouslyMILOnEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM25ExpandedFreezeFrame;
import org.etools.j1939tools.j1939.packets.DM26TripDiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.DM28PermanentEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DM29DtcCounts;
import org.etools.j1939tools.j1939.packets.DM30ScaledTestResultsPacket;
import org.etools.j1939tools.j1939.packets.DM31DtcToLampAssociation;
import org.etools.j1939tools.j1939.packets.DM33EmissionIncreasingAECDActiveTime;
import org.etools.j1939tools.j1939.packets.DM5DiagnosticReadinessPacket;
import org.etools.j1939tools.j1939.packets.DM6PendingEmissionDTCPacket;
import org.etools.j1939tools.j1939.packets.DiagnosticTroubleCodePacket;
import org.etools.j1939tools.j1939.packets.EngineHoursPacket;
import org.etools.j1939tools.j1939.packets.EngineHoursTimer;
import org.etools.j1939tools.j1939.packets.GenericPacket;
import org.etools.j1939tools.j1939.packets.IdleOperationPacket;
import org.etools.j1939tools.j1939.packets.MonitoredSystem;
import org.etools.j1939tools.j1939.packets.MonitoredSystemStatus;
import org.etools.j1939tools.j1939.packets.ScaledTestResult;
import org.etools.j1939tools.j1939.packets.SupportedSPN;
import org.etools.j1939tools.modules.CommunicationsModule;

public class SectionA5MessageVerifier extends SectionVerifier {
    SectionA5MessageVerifier(int partNumber, int stepNumber) {
        this(DataRepository.getInstance(),
             new CommunicationsModule(),
             new VehicleInformationModule(),
             partNumber,
             stepNumber);
    }

    protected SectionA5MessageVerifier(DataRepository dataRepository,
                                       CommunicationsModule communicationsModule,
                                       VehicleInformationModule vehInfoModule,
                                       int partNumber,
                                       int stepNumber) {

        super(dataRepository,
              communicationsModule,
              vehInfoModule,
              partNumber,
              stepNumber);
    }

    boolean checkDM5(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM5DiagnosticReadinessPacket.class, address);
        if (prev == null) {
            return true;
        }

        // 1.e. DM5 shall report zero for number of active and previously active DTCs.
        // 4.a. DM5 shall report test not complete (1) for all supported monitors except comprehensive components.
        return getCommunicationsModule().requestDM5(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            boolean isAllTestsIncomplete = p.getMonitoredSystems()
                                                                            .stream()
                                                                            .filter(s -> s.getId() != CompositeSystem.COMPREHENSIVE_COMPONENT)
                                                                            .filter(system -> {
                                                                                return isEngineModuleYearLessThan2019()
                                                                                        || system.getId() != CompositeSystem.MISFIRE;
                                                                            })

                                                                            .map(MonitoredSystem::getStatus)
                                                                            .filter(MonitoredSystemStatus::isEnabled)
                                                                            .noneMatch(MonitoredSystemStatus::isComplete);

                                            boolean isNoCodes = (p.getActiveCodeCount() == 0
                                                    && p.getPreviouslyActiveCodeCount() == 0
                                                    || (p.getActiveCodeCount() == (byte) 0xFF)
                                                            && p.getPreviouslyActiveCodeCount() == (byte) 0xFF);
                                            boolean isErased = isNoCodes && isAllTestsIncomplete;

                                            boolean wasAllTestsIncomplete = prev.getMonitoredSystems()
                                                                                .stream()
                                                                                .filter(s -> s.getId() != CompositeSystem.COMPREHENSIVE_COMPONENT)
                                                                                .map(MonitoredSystem::getStatus)
                                                                                .filter(MonitoredSystemStatus::isEnabled)
                                                                                .noneMatch(MonitoredSystemStatus::isComplete);

                                            boolean wasNoCodes = (prev.getActiveCodeCount() == 0
                                                    && prev.getPreviouslyActiveCodeCount() == 0)
                                                    || (prev.getActiveCodeCount() == (byte) 0xFF
                                                            && prev.getPreviouslyActiveCodeCount() == (byte) 0xFF);
                                            boolean wasErased = wasNoCodes && wasAllTestsIncomplete;

                                            return shouldBeReported(verifyIsErased, wasErased, isErased);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM6(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM6PendingEmissionDTCPacket.class, address);
        if (prev == null) {
            return true;
        }
        // 1.a. DM6 pending shall report no DTCs and MIL off and not flashing
        return getCommunicationsModule().requestDM6(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            return filterDTCPacket(verifyIsErased, p, prev);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM12(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM12MILOnEmissionDTCPacket.class, address);
        if (prev == null) {
            return true;
        }

        // 1.b. DM12 active shall report no DTCs and MIL off and not flashing
        return getCommunicationsModule().requestDM12(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            return filterDTCPacket(verifyIsErased, p, prev);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM20(ResultsListener listener, String section, int address) {
        // 7.a. DM20 Monitor Performance Ratio data shall not be reset/shall stay the same as it was before code clear
        // for all values including the number of ignition cycles, general denominators, monitor specific numerators,
        // and monitor specific denominators.
        var repoPacket = getLatest(DM20MonitorPerformanceRatioPacket.class, address);
        if (repoPacket == null) {
            return true;
        }

        return getCommunicationsModule().requestDM20(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            if (p.getIgnitionCycles() < repoPacket.getIgnitionCycles()) {
                                                return true;
                                            }

                                            if (p.getOBDConditionsCount() < repoPacket.getOBDConditionsCount()) {
                                                return true;
                                            }

                                            for (var ratio : p.getRatios()) {
                                                var optional = repoPacket.getRatio(ratio.getId());
                                                if (optional.isPresent()) {
                                                    var repoRatio = optional.get();
                                                    if (ratio.getNumerator() < repoRatio.getNumerator()) {
                                                        return true;
                                                    }
                                                    if (ratio.getDenominator() < repoRatio.getDenominator()) {
                                                        return true;
                                                    }
                                                } else {
                                                    return true;
                                                }
                                            }

                                            return false;
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, false, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM21(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM21DiagnosticReadinessPacket.class, address);
        if (prev == null) {
            return true;
        }
        // 3.b. DM21 diagnostic readiness 2 shall report 0 for distance with MIL on and minutes run with MIL on.
        // 5.b. DM21 diagnostic readiness 2 shall report 0 for distance since code clear and minutes run since code
        // clear.
        return getCommunicationsModule().requestDM21(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            boolean isErased = p.getKmWhileMILIsActivated() == 0
                                                    && p.getMinutesWhileMILIsActivated() == 0
                                                    && p.getKmSinceDTCsCleared() == 0
                                                    && p.getMinutesSinceDTCsCleared() == 0;

                                            boolean wasErased = prev.getKmWhileMILIsActivated() == 0
                                                    && prev.getMinutesWhileMILIsActivated() == 0
                                                    && prev.getKmSinceDTCsCleared() == 0
                                                    && prev.getMinutesSinceDTCsCleared() == 0;

                                            return shouldBeReported(verifyIsErased, wasErased, isErased);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM23(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM23PreviouslyMILOnEmissionDTCPacket.class, address);
        if (prev == null) {
            return true;
        }
        // 1.c. DM23 previously active shall report no DTCs and MIL off and not flashing
        return getCommunicationsModule().requestDM23(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            return filterDTCPacket(verifyIsErased, p, prev);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM25(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM25ExpandedFreezeFrame.class, address);
        if (prev == null) {
            return true;
        }
        // 2.a. DM25 expanded freeze frame shall report no data and DTC causing freeze frame
        // with bytes 1-5 = 0 and bytes 6-8 = 255.
        // In this case, we do not care to parse the SPN data, so pass a null DM24.
        return getCommunicationsModule().requestDM25(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            boolean isErased = p.getFreezeFrames().isEmpty();

                                            boolean wasErased = prev.getFreezeFrames().isEmpty();

                                            return shouldBeReported(verifyIsErased, wasErased, isErased);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM26(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM26TripDiagnosticReadinessPacket.class, address);
        if (prev == null) {
            return true;
        }
        // 5.a. DM26 diagnostic readiness 3 shall report 0 for number of warm-ups since code clear.
        return getCommunicationsModule().requestDM26(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            boolean isErased = p.getWarmUpsSinceClear() == 0;

                                            boolean wasErased = prev.getWarmUpsSinceClear() == 0;

                                            return shouldBeReported(verifyIsErased, wasErased, isErased);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM28(ResultsListener listener, String section, int address) {
        // 8.a. DM28 permanent DTCs shall not be erased/still report any permanent DTC
        // that was present before code clear.
        var repoPacket = getLatest(DM28PermanentEmissionDTCPacket.class, address);
        if (repoPacket == null || !repoPacket.hasDTCs()) {
            return true;
        }

        return getCommunicationsModule().requestDM28(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            return !p.hasDTCs();
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, false, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM29(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM29DtcCounts.class, address);
        if (prev == null) {
            return true;
        }
        // 1.d. DM29 shall report zero for number of pending, active, and previously active DTCs
        return getCommunicationsModule().requestDM29(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            boolean prevState = prev.getEmissionRelatedPendingDTCCount() == 0
                                                    && prev.getEmissionRelatedMILOnDTCCount() == 0
                                                    && prev.getEmissionRelatedPreviouslyMILOnDTCCount() == 0;

                                            boolean currentState = p.getEmissionRelatedPendingDTCCount() == 0
                                                    && p.getEmissionRelatedMILOnDTCCount() == 0
                                                    && p.getEmissionRelatedPreviouslyMILOnDTCCount() == 0;

                                            return shouldBeReported(verifyIsErased, prevState, currentState);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM31(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        var prev = getLatest(DM31DtcToLampAssociation.class, address);
        if (prev == null) {
            return true;
        }
        // 3.a. DM31 lamp status shall report no DTCs causing MIL on (if supported).
        return getCommunicationsModule().requestDM31(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {

                                            boolean isErased = p.getDtcLampStatuses()
                                                                .stream()
                                                                .allMatch(s -> s.getMalfunctionIndicatorLampStatus() == OFF);

                                            boolean wasErased = prev.getDtcLampStatuses()
                                                                    .stream()
                                                                    .allMatch(s -> s.getMalfunctionIndicatorLampStatus() == OFF);

                                            return shouldBeReported(verifyIsErased, wasErased, isErased);
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, verifyIsErased, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkDM33(ResultsListener listener, String section, int address) {
        // 9.a. DM33 EI-AECD information shall not be reset/cleared for any non-zero values present before code clear.
        var repoPacket = getLatest(DM33EmissionIncreasingAECDActiveTime.class, address);
        if (repoPacket == null) {
            return true;
        }

        return getCommunicationsModule().requestDM33(listener, address)
                                        .toPacketStream()
                                        .filter(p -> {
                                            for (EngineHoursTimer repoTimer : repoPacket.getEiAecdEngineHoursTimers()) {
                                                var pTimer = p.getTimer(repoTimer.getEiAecdNumber());
                                                if (repoTimer.getEiAecdTimer1() > pTimer.getEiAecdTimer1()) {
                                                    return true;
                                                }

                                                if (repoTimer.getEiAecdTimer2() > pTimer.getEiAecdTimer2()) {
                                                    return true;
                                                }
                                            }
                                            return false;
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, false, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkTestResults(ResultsListener listener, String section, int address, boolean verifyIsErased) {
        // 6.a. DM7/DM30 Test Results shall report all test results with initialized results and limits
        // (all 0x00 or 0xFB00 for results and 0xFFFF for limits).
        List<SupportedSPN> testResultSPNs = getDataRepository().getObdModule(address).getTestResultSPNs();
        boolean failure = false;
        // If there are no test results to verify, then no verification of erased or not erased can be made.
        if (!testResultSPNs.isEmpty()) {
            boolean isErased = testResultSPNs
                                             .stream()
                                             .map(SupportedSPN::getSpn)
                                             .map(spn -> getCommunicationsModule().requestTestResult(listener,
                                                                                                     address,
                                                                                                     247,
                                                                                                     spn,
                                                                                                     31))
                                             .flatMap(BusResult::toPacketStream)
                                             .map(DM30ScaledTestResultsPacket::getTestResults)
                                             .flatMap(Collection::stream)
                                             .allMatch(ScaledTestResult::isInitialized);

            failure = verifyIsErased != isErased;
            if (failure) {
                addFailure(listener,
                           section,
                           verifyIsErased,
                           Lookup.getAddressName(address),
                           "Test Results");
            }
        }
        return !failure;
    }

    boolean checkEngineRunTime(ResultsListener listener, String section, int address) {
        // 9.b. Cumulative engine runtime [PGN 65253 (SPN 247))] shall not be reset/cleared for any non-zero values
        // present before code clear.
        var packet = getLatest(EngineHoursPacket.class, address);
        if (packet == null || packet.getEngineHours() == 0) {
            return true;
        }

        return getCommunicationsModule().request(EngineHoursPacket.PGN, address, listener)
                                        .toPacketStream()
                                        .map(GenericPacket::getPacket)
                                        .map(EngineHoursPacket::new)
                                        .filter(p -> {
                                            return p.getEngineHours() < packet.getEngineHours();
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, false, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    boolean checkEngineIdleTime(ResultsListener listener, String section, int address) {
        // 9.b. Cumulative engine idle time [(PGN 65244 (SPN 235)] shall not be reset/cleared for any non-zero values
        // present before code clear.
        var packet = getLatest(IdleOperationPacket.class, address);
        if (packet == null || packet.getEngineIdleHours() == 0) {
            return true;
        }

        return getCommunicationsModule().request(IdleOperationPacket.PGN, address, listener)
                                        .toPacketStream()
                                        .map(GenericPacket::getPacket)
                                        .map(IdleOperationPacket::new)
                                        .filter(p -> {
                                            return p.getEngineIdleHours() < packet.getEngineIdleHours();
                                        })
                                        .peek(p -> {
                                            addFailure(listener, section, false, p);
                                        })
                                        .findAny()
                                        .isEmpty();
    }

    private static boolean filterDTCPacket(boolean verifyIsErased,
                                           DiagnosticTroubleCodePacket current,
                                           DiagnosticTroubleCodePacket previous) {
        boolean wasPreviouslyErased = !previous.hasDTCs() && previous.getMalfunctionIndicatorLampStatus() == OFF;
        boolean isCurrentlyErased = !current.hasDTCs() && current.getMalfunctionIndicatorLampStatus() == OFF;
        return shouldBeReported(verifyIsErased, wasPreviouslyErased, isCurrentlyErased);
    }

    private static boolean shouldBeReported(boolean verifyIsErased,
                                            boolean wasPreviouslyErased,
                                            boolean isCurrentlyErased) {
        if (verifyIsErased) {
            // Report if the data is not erased
            return !wasPreviouslyErased && !isCurrentlyErased;
        } else {
            // Report if the data is erased
            return isCurrentlyErased && !wasPreviouslyErased;
        }
    }

}
