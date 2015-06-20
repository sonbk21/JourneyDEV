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

package client;

import tools.FilePrinter;

/**
 *
 * @author kevintjuh93
 */
public enum AutobanFactory {
    MOB_COUNT, FIX_DAMAGE, HIGH_HP_HEALING, FAST_HP_HEALING(15), FAST_MP_HEALING(15), TUBI(20, 15000), SHORT_ITEM_VAC, ITEM_VAC(Action.BAN), FAST_ATTACK(10, 30000),
    MPCON(25, 30000), HIGHDAMAGE(Action.BAN), WZ_EDIT(Action.BAN), ILLEGAL_MESO_DROP(Action.BAN), ILLEGAL_MESO_STORAGE(Action.BAN), ILLEGAL_ITEM_USE, CUSTOM_PACKET(Action.BAN), 
    BOTTING;
    
    private int points;
    private long expiretime;
    private Action action;
    
    private enum Action {
        BAN, DISCONNECT;
    }

    private AutobanFactory() {
        this(1, -1, Action.DISCONNECT);
    }

    private AutobanFactory(int points) {
        this.points = points;
        this.expiretime = -1;
        this.action = Action.DISCONNECT;
    }
    
    private AutobanFactory(Action action) {
        this.points = 1;
        this.expiretime = -1;
        this.action = action;
    }
    
    private AutobanFactory(int points, Action action) {
        this.points = points;
        this.expiretime = -1;
        this.action = action;
    }

    private AutobanFactory(int points, long expire) {
        this.points = points;
        this.expiretime = expire;
        this.action = Action.DISCONNECT;
    }
    
    private AutobanFactory(int points, long expire, Action action) {
        this.points = points;
        this.expiretime = expire;
        this.action = action;
    }

    public int getMaximum() {
        return points;
    }

    public long getExpire() {
        return expiretime;
    }
    
    public void execute(MapleCharacter chr, String value) {
        if (action == Action.BAN) {
            FilePrinter.print(FilePrinter.AUTOBAN + chr.getName()+".txt", "Autobanned for (" + this.name() + ": " + value + ")");
            chr.ban("Autobanned for (" + this.name() + ": " + value + ")", 1);
            chr.getClient().disconnect(false, false);
        } else if (action == Action.DISCONNECT) {
            FilePrinter.print(FilePrinter.AUTOBAN + chr.getName()+".txt", "Disconnected for (" + this.name() + ": " + value + ")");
            chr.getClient().disconnect(false, false);
        }
    }
}
