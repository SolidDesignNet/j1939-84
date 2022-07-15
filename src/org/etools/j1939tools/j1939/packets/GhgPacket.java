/*
 * Copyright (c) 2022. Equipment & Tool Institute
 */

package org.etools.j1939tools.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.etools.j1939_84.utils.CollectionUtils;
import org.etools.j1939tools.bus.Packet;
import org.etools.j1939tools.j1939.model.Spn;
import org.etools.j1939tools.j1939.model.SpnDataParser;

public class GhgPacket extends GenericPacket {

    private List<GhgActiveTechnologyGroup> techGroups;

    public GhgPacket(Packet packet) {
        super(packet);
    }

    // public GhgActiveTechnologyGroup getGroup(int number) {
    // return geActiveTechnologyGroups().stream().filter(t -> {
    // return t == number;
    // }).findFirst().orElse(null);
    // }

    public List<GhgActiveTechnologyGroup> getActiveTechnologyGroups() {
        if (techGroups == null) {
            parsePacket();
        }
        return techGroups;
    }

    private void parsePacket() {
        techGroups = new ArrayList<>();
        for (Spn spn : getSpns()) {
            int length = spn.getBytes().length;
            for (int i = 0; i + 8 < length; i = i + 9) {
                int[] copyOfRange = getPacket().getData(i, i + 9);
                techGroups.add(new GhgActiveTechnologyGroup(copyOfRange, spn));
                System.out.println(Arrays.toString(SpnDataParser.parse(CollectionUtils.toByteArray(copyOfRange),
                                                                       getPgnDefinition().getSpnDefinitions().get(0),
                                                                       copyOfRange.length)));
            }
        }
    }

    // public List<Spn> getSpns() {
    // System.out.println("Made it here");
    // if (spns == null) {
    // System.out.println("Now, I made it here");
    // spns = new ArrayList<>();
    //
    // List<SpnDefinition> spnDefinitions = getPgnDefinition().getSpnDefinitions();
    // byte[] bytes = getPacket().getBytes();
    // for (SpnDefinition definition : spnDefinitions) {
    // Slot slot = getJ1939DaRepository().findSLOT(definition.getSlotNumber(), definition.getSpnId());
    // if (slot.getLength() != 0) {
    // byte[] data = SpnDataParser.parse(bytes, definition, slot.getLength());
    // spns.add(new Spn(definition.getSpnId(), definition.getLabel(), slot, data));
    // }
    // }
    // }
    // return spns;
    // }
        

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getStringPrefix()).append("[").append(NL);
        getActiveTechnologyGroups().forEach(grp -> {
            sb.append("  ").append(grp.toString()).append(NL);
        });
        sb.append("]" + NL);
        return sb.toString();
    }

}
