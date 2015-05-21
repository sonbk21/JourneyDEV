/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.properties.MapleStat;
import client.properties.PotentialLine.PotentialStat;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 *
 * @author Generic, modified by SYJourney
 */
public class AutoAssignHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        slea.skip(8);
        if (chr.getRemainingAp() < 1) {
            return;
        }
        int total = 0;
        int extras = 0;
        for (int i = 0; i < 2; i++) {
            int type = slea.readInt();
            int tempVal = slea.readInt();
            if (tempVal < 0 || tempVal > c.getPlayer().getRemainingAp()) {
                return;
            }
            total += tempVal;
            extras += gainStatByType(chr, MapleStat.getBy5ByteEncoding(type), tempVal);
        }
        int remainingAp = (chr.getRemainingAp() - total) + extras;
        chr.setRemainingAp(remainingAp);
        chr.updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
        c.announce(MaplePacketCreator.enableActions());
    }

    private int gainStatByType(MapleCharacter chr, MapleStat type, int gain) {
        int newVal = chr.getStat(type);
        PotentialStat temp = PotentialStat.values()[type.ordinal() + 5];
        if (newVal > 999 + chr.getAbilities().getTempStat(temp))
            chr.setStat(type, 999 + chr.getAbilities().getTempStat(temp));
        else
            chr.setStat(type, newVal);
        
        if (newVal < 1000)
            chr.updateSingleStat(type, newVal);
            
        return 0;
    }
}
