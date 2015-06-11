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

import client.MapleClient;
import client.inventory.Equip.ScrollResult;
import client.inventory.*;
import java.util.ArrayList;
import java.util.List;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ScrollHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleInventory useInventory = c.getPlayer().getInventory(MapleInventoryType.USE);
        
        slea.readInt();
        byte slot = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        boolean whiteScroll = ((byte) slea.readShort() & 2) == 2;
        
        Item wscroll = null;
        if (whiteScroll) {
            wscroll = useInventory.findById(2340000);
            if (wscroll == null || wscroll.getItemId() != 2340000) {
                return;
            }
        }

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip toScroll = (Equip) c.getPlayer().getInventory((dst >= 0)?MapleInventoryType.EQUIP:MapleInventoryType.EQUIPPED).getItem(dst);
        byte oldLevel = toScroll.getLevel();
        byte oldSlots = toScroll.getUpgradeSlots();
        
        Item scroll = useInventory.getItem(slot);
        boolean customScroll = false;
        if (scroll.getClass().getName().equals("CustomScroll")) {
            customScroll = true;
            CustomScroll cs = (CustomScroll) scroll;
            if (toScroll.getItemId()/10000 != cs.getScrollType().ordinal()) {
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            }
        } else {
            List<Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
            if (toScroll.getUpgradeSlots() < ((isCleanSlate(scroll.getItemId()))?0:1) || (scrollReqs.size() > 0 && !scrollReqs.contains(toScroll.getItemId()))) {
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            }
            if (scroll.getItemId() != 2049100 && !isCleanSlate(scroll.getItemId())) {
                if (!canScroll(scroll.getItemId(), toScroll.getItemId())) {
                    return;
                }
            }
        }
        
        if (scroll.getQuantity() < 1) {
            return;
        }
        
        if (scroll.getItemId() == 2049002) {
             if (!toScroll.isRevealed() && toScroll.getNumLines() > 0) {
                c.getPlayer().message("You should reveal this item's potential before resetting it.");
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            } else if (toScroll.getAllStats().isEmpty()) {
                c.getPlayer().message("A statless item cannot have a potential.");
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            }
        }
        
        if (scroll.getItemId() == 2049004) {
            if (toScroll.getNumLines() == 0) {
                c.getPlayer().message("This item has no potential.");
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            } else if (toScroll.isRevealed()) {
                c.getPlayer().message("This item's potential has already been revealed.");
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            } else if (c.getPlayer().getMeso() <= getMagnifyPrice(toScroll.getRank())) {
                c.getPlayer().message("You do not have enough mesos to reveal this item's potential.");
                c.announce(MaplePacketCreator.getInventoryFull());
                return;
            } else {
                c.getPlayer().gainMeso( -getMagnifyPrice(toScroll.getRank()), true, false, true);
            }
        }
        
        Equip scrolled;
        if (customScroll)
            scrolled = (Equip) ii.scrollEquipCustom(toScroll, (CustomScroll) scroll, whiteScroll, c.getPlayer().isGM());
        else
            scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll.getItemId(), whiteScroll, c.getPlayer().isGM());
        ScrollResult scrollSuccess = Equip.ScrollResult.FAIL;
        if (scrolled == null) {
            scrollSuccess = Equip.ScrollResult.CURSE;
        } else if (scrolled.getLevel() > oldLevel || (scrolled.getUpgradeSlots() > oldSlots) || scroll.getItemId() == 2049002 || scroll.getItemId() == 2049004) {
            scrollSuccess = Equip.ScrollResult.SUCCESS;
        }
        
        useInventory.removeItem(scroll.getPosition(), (short) 1, false);
        if (whiteScroll) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        }
        
        final List<ModifyInventory> mods = new ArrayList<>();
        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            mods.add(new ModifyInventory(3, toScroll));
            if (dst < 0) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else {
            mods.add(new ModifyInventory(3, scrolled));
            mods.add(new ModifyInventory(0, scrolled));
        }
        if (scroll.getQuantity() < 1)
            mods.add(new ModifyInventory(3, scroll));
        else
            mods.add(new ModifyInventory(1, scroll));
        c.announce(MaplePacketCreator.modifyInventory(true, mods));
        
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), scrollSuccess, dst >= 0));
        if (scroll.getItemId() == 2049002) {
            c.getPlayer().dropMessage(6, "Potential reset! Use a magnifying glass to reveal this item's potential");
        } else if (scroll.getItemId() == 2049004) {
            String msg = "Potential revealed! New Potential: ";
            for (byte i = 0; i < scrolled.getNumLines(); i++) {
                msg += scrolled.potentialName(i)+"  ";
                if (i == scrolled.getNumLines() - 1)
                    msg += "("+scrolled.rankName()+")";
            }
            c.getPlayer().dropMessage(6, msg);
        }
        
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE)) {
            c.getPlayer().equipChanged();
        }
    }

    private boolean isCleanSlate(int scrollId) {
        return scrollId > 2048999 && scrollId < 2049005;
    }

    public boolean canScroll(int scrollid, int itemid) {
        return (scrollid / 100) % 100 == (itemid / 10000) % 100;
    }
    
    public int getMagnifyPrice(short rank) {
        switch (rank) {
            case 1: return 1000;
            case 2: return 5000;
            case 3: return 27500;
            case 4: return 150000;
            case 5: return 1250000;
        }
        return 0;
    }
}