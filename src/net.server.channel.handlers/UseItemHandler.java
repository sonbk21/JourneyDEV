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

import client.LastActionManager.MapleAction;
import client.MapleClient;
import client.Professions.MapleProfession;
import client.Professions.ProfessionLevel;
import client.properties.MapleDisease;
import client.autoban.AutobanFactory;
import client.inventory.Equip.EquipStat;
import client.inventory.Equip.ScrollResult;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.properties.MapleStat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.MapleHarvestable;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public final class UseItemHandler extends AbstractMaplePacketHandler {
    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null) {
        } else if (toUse.getQuantity() > 0 && toUse.getItemId() == itemId) {
            
            switch (itemId) {
                case 2022178:
                case 2022433:
                case 2050004:
                    c.getPlayer().dispelDebuffs();
                    remove(c, slot);
                    return;
                case 2050003:
                    c.getPlayer().dispelDebuff(MapleDisease.SEAL);
                    remove(c, slot);
                    return;
                case 2022053:
                    c.getPlayer().getStats().resetAbility();
                    c.getPlayer().getStats().resetStats(c.getPlayer(), true);
                    List<Pair<MapleStat, Integer>> statup = new ArrayList<>(10);
                    statup.add(new Pair<>(MapleStat.MAXHP, (int) c.getPlayer().getStat(MapleStat.MAXHP)));
                    statup.add(new Pair<>(MapleStat.MAXMP, (int) c.getPlayer().getStat(MapleStat.MAXMP)));
                    statup.add(new Pair<>(MapleStat.STR, (int) c.getPlayer().getStat(MapleStat.STR)));
                    statup.add(new Pair<>(MapleStat.DEX, (int) c.getPlayer().getStat(MapleStat.DEX)));
                    statup.add(new Pair<>(MapleStat.INT, (int) c.getPlayer().getStat(MapleStat.INT)));
                    statup.add(new Pair<>(MapleStat.LUK, (int) c.getPlayer().getStat(MapleStat.LUK)));
                    c.announce(MaplePacketCreator.updatePlayerStats(statup));
                    
                    String msg = "Inner Ability Reset! New Inner Ability ("+c.getPlayer().getStats().getAbilityName()+"): ";
                    for (byte i = 0; i < c.getPlayer().getStats().getAbilityLines(); i++)
                        msg += c.getPlayer().getStats().getAbilityLine(i).getName(c.getPlayer().getStats().getAbilityRank())+"  ";
                    
                    c.getPlayer().dropMessage(6, msg);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getScrollEffect(c.getPlayer().getId(), ScrollResult.SUCCESS, false));
                    remove(c, slot);
                    return;
                case 2023000:
                case 2023001:
                case 2023100:
                case 2023101:
                    MapleProfession profession = c.getPlayer().getProfessions().getProfession(true);
                    
                    if (profession != null) { //Shouldnt even have this item
                        if (c.getPlayer().isAllowed(MapleAction.HARVEST)) {
                            List<MapleMapObject> reactors = c.getPlayer().getMap().getMapObjectsInFront(c.getPlayer().getPosition(), c.getPlayer().getDirection(), Arrays.asList(MapleMapObjectType.REACTOR));
                            boolean found = false;
                            MapleHarvestable mh = null;
                    
                            for (MapleMapObject mmo : reactors) {
                        
                                if (mmo instanceof MapleHarvestable)
                                    mh = (MapleHarvestable) mmo;
                                else
                                    continue;
                        
                                if (mh.canHarvest(profession.getType())) {
                                    found = true;
                                    break;
                                }
                            }
                    
                            if (found) {
                                if (mh.harvestReturnSuccess(profession.getLevel())) {
                                    mh.dropItems(c.getPlayer(), itemId%10 == 1); //second argument: golden or normal
                                    short expgain = mh.getExpGain(profession.getLevel());
                                    if (profession.gainExp(expgain, c.getPlayer().getLevel())) {
                                        c.getPlayer().dropMessage(6, "You now have enough exp to raise your "+profession.getName()+" level! Talk to your instructor to raise your level.");
                                    } else if (profession.getLevel() == ProfessionLevel.MASTER) {
                                        c.getPlayer().message("Harvesting successful!");
                                    } else {
                                        c.getPlayer().message("Harvesting successful! You have gained "+expgain+" "+profession.getName()+" exp.");
                                    }
                                }
                                c.getPlayer().saveLastAction(MapleAction.HARVEST);
                            } else {
                                c.getPlayer().message("You have to use this item on a "+profession.getHarvestableName()+".");
                            }
                        } else {
                            c.getPlayer().message("You may not use this item yet.");
                        }
                    }
                    c.announce(MaplePacketCreator.enableActions());
                    return;
            }
            
            if (isTownScroll(itemId)) {
                if (ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer())) {
                    remove(c, slot);
                }
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
            remove(c, slot);
            ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
            c.getPlayer().getTimer().scheduleBerserk();
        } else {
            AutobanFactory.ILLEGAL_ITEM_USE.execute(c.getPlayer(), String.valueOf(toUse.getItemId()));
        }
    }

    private void remove(MapleClient c, byte slot) {
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        c.announce(MaplePacketCreator.enableActions());
    }

    private boolean isTownScroll(int itemId) {
        return itemId >= 2030000 && itemId < 2030021;
    }
}
