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

/*
 Modified by SYJourney
*/
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.properties.MapleJob;
import client.properties.MapleStat;
import client.properties.PotentialLine.PotentialStat;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class DistributeAPHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        int num = slea.readInt();
        if (c.getPlayer().getRemainingAp() > 0) {
            if (addStat(c, num)) {
                c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - 1);
                c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    static boolean addStat(MapleClient c, int id) {
        int max = 999;
        MapleStat type = null;
        switch (id) {
            case 64: type = MapleStat.STR; break;
            case 128: type = MapleStat.DEX; break;
            case 256: type = MapleStat.INT; break;
            case 512: type = MapleStat.LUK; break;
            case 2048: type = MapleStat.MAXHP; break;
            case 8192: type = MapleStat.MAXMP;
        }
        if (id < 2048) {
            max += c.getPlayer().getAbilities().getTempStat(PotentialStat.values()[type.ordinal() + 5]);
            if (c.getPlayer().getStat(type) >= max) {
                return false;
            }
            c.getPlayer().addStat(type, 1);
        } else if (id == 2048) {
            addHP(c.getPlayer(), addHP(c));
        } else if (id == 8192) {
            addMP(c.getPlayer(), addMP(c));
        } else {
            c.announce(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true));
            return false;
        }
        return true;
    }

    static int addHP(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleJob job = player.getJob();
        int MaxHP = player.getStat(MapleStat.MAXHP);
        if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
            return MaxHP;
        }
        if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            MaxHP += 20;
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            MaxHP += 6;
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxHP += 16;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            MaxHP += 18;
        } else {
            MaxHP += 8;
        }
        return MaxHP;
    }

    static int addMP(MapleClient c) { //isnt this function also in maplecharacter?..
        MapleCharacter player = c.getPlayer();
        int MaxMP = player.getStat(MapleStat.MAXMP);
        MapleJob job = player.getJob();
        if (player.getHpMpApUsed() > 9999 || MaxMP >= 30000) {
            return MaxMP;
        }
        if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            MaxMP += 2;
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            MaxMP += 18;
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxMP += 10;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            MaxMP += 14;
        } else {
            MaxMP += 6;
        }
        return MaxMP;
    }

    static void addHP(MapleCharacter player, int MaxHP) {
        MaxHP = Math.min(30000, MaxHP);
        player.setHpMpApUsed(player.getHpMpApUsed() + 1);
        player.setStat(MapleStat.MAXHP, MaxHP);
    }

    static void addMP(MapleCharacter player, int MaxMP) {
        MaxMP = Math.min(30000, MaxMP);
        player.setHpMpApUsed(player.getHpMpApUsed() + 1);
        player.setStat(MapleStat.MAXMP, MaxMP);
    }
}
