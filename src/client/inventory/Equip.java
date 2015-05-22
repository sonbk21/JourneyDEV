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
package client.inventory;

import client.MapleClient;
import constants.ItemConstants;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

public class Equip extends Item {

    public static enum ScrollResult {

        FAIL(0), SUCCESS(1), CURSE(2);
        private int value = -1;

        private ScrollResult(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    public enum EquipStat {
        STR, DEX, INT, LUK, HP, MP, WATK, MAGIC, WDEF, MDEF, ACC, AVOID, HANDS, SPEED, JUMP;
    }
    
    private enum Potential {
        STR, DEX, INT, LUK, HP, MP, ATK, MAGIC, BDM;
    }
    
    private final EnumMap<EquipStat, Short> stats = new EnumMap<>(EquipStat.class);
    private short vicious;
    private byte upgradeSlots;
    private byte level, flag, itemLevel;
    private final EnumMap<EquipStat, Short> tempstats = new EnumMap<>(EquipStat.class);
    private float itemExp;
    private int ringid = -1;
    private final int[] potentials = {0, 0, 0};
    private boolean potAdded = false;
    private boolean wear = false;

    public Equip(int id, byte position) {
        super(id, position, (short) 1);
        this.itemExp = 0;
        this.itemLevel = 1;
    }

    public Equip(int id, byte position, int slots) {
        super(id, position, (short) 1);
        this.upgradeSlots = (byte) slots;
        this.itemExp = 0;
        this.itemLevel = 1;
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUpgradeSlots());
        clearPotStats();
        ret.stats.putAll(stats);
        ret.potentials[0] = potentials[0];
        ret.potentials[1] = potentials[1];
        ret.potentials[2] = potentials[2];
        ret.flag = flag;
        ret.vicious = vicious;
        ret.upgradeSlots = upgradeSlots;
        ret.itemLevel = itemLevel;
        ret.itemExp = itemExp;
        ret.level = level;
        ret.log = new LinkedList<>(log);
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.setExpiration(getExpiration());
        ret.setGiftFrom(getGiftFrom());
        addPotStats();
        return ret;
    }

    @Override
    public byte getFlag() {
        return flag;
    }

    @Override
    public byte getType() {
        return 1;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }
    
    public short getStat(EquipStat type) {
        return stats.containsKey(type)?stats.get(type):0;
    }
    
    public EnumMap<EquipStat, Short> getAllStats() {
        return stats;
    }
    
    public void setStat(EquipStat type, short value) {
        stats.put(type, value);
    }
    
    public void addStat(EquipStat type, int value) {
        if (stats.containsKey(type))
            stats.put(type, (short) (stats.get(type) + value));
        else
            stats.put(type, (short) value);
    }

    public short getVicious() {
        return vicious;
    }

    @Override
    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public void setVicious(short vicious) {
        this.vicious = vicious;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public void gainLevel(MapleClient c, boolean timeless) {
        List<Pair<String, Integer>> statups = MapleItemInformationProvider.getInstance().getItemLevelupStats(getItemId(), itemLevel, timeless);
        for (Pair<String, Integer> stat : statups) {
            switch (stat.getLeft()) {
                case "incMHP":
                    addStat(EquipStat.HP, stat.getRight());
                    break;
                case "incMMP":
                    addStat(EquipStat.MP, stat.getRight());
                    break;
                case "incPAD":
                    addStat(EquipStat.WATK, stat.getRight());
                    break;
                case "incMAD":
                    addStat(EquipStat.MAGIC, stat.getRight());
                    break;
                case "incPDD":
                    addStat(EquipStat.WDEF, stat.getRight());
                    break;
                case "incMDD":
                    addStat(EquipStat.MDEF, stat.getRight());
                    break;
                case "incEVA":
                    addStat(EquipStat.AVOID, stat.getRight());
                    break;
                default:
                    addStat(EquipStat.valueOf(stat.getLeft().substring(3).toUpperCase()), stat.getRight());
            }
        }
        this.itemLevel++;
        c.announce(MaplePacketCreator.showEquipmentLevelUp());
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showForeignEffect(c.getPlayer().getId(), 15));
        c.getPlayer().forceUpdateItem(this);
    }

    public int getItemExp() {
        return (int) itemExp;
    }

    public void gainItemExp(MapleClient c, int gain, boolean timeless) {
        int expneeded = timeless ? (10 * itemLevel + 70) : (5 * itemLevel + 65);
        float modifier = 364 / expneeded;
        float exp = (expneeded / (1000000 * modifier * modifier)) * gain;
        itemExp += exp;
        if (itemExp >= 364) {
            itemExp = (itemExp - 364);
            gainLevel(c, timeless);
        } else {
            c.getPlayer().forceUpdateItem(this);
        }
    }

    public void setItemExp(int exp) {
        this.itemExp = exp;
    }

    public void setItemLevel(byte level) {
        this.itemLevel = level;
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    public void setUpgradeSlots(int i) {
        this.upgradeSlots = (byte) i;
    }

    public void setVicious(int i) {
        this.vicious = (short) i;
    }

    public int getRingId() {
        return ringid;
    }
    
    public int getPotential(int line) {
        return potentials[line];
    }
    
    public int getPotentialValue(int line) {
        if (potentials[line]%100 < 4)
            return (4*potentials[line]/1000 - (potentials[line]%1000)/100);
        else if (potentials[line]%100 < 6)
            return 5*(4*potentials[line]/1000 - (potentials[line]%1000)/100);
        else
            return (2*potentials[line]/1000 - (potentials[line]%1000)/200);
    }
    
    public void resetPotential() {
        int rank = (potentials[0] == 0)?1:(potentials[0]/1000 == 5)?5:(potentials[0]/1000)+Randomizer.nextInt(26)/25;
        int newPot;
        for (int i = 0; i < potentials.length; i++) {
            do {
                newPot = Randomizer.nextInt(Potential.values().length);
            } while (!isElligible(newPot));
            this.potentials[i] = (rank*1000)+newPot+100*(Randomizer.nextInt(4));
            if (i == 1 && this.potentials[2] == 0)
                i = 2-Randomizer.nextInt(26)/25;
        }
        recalcTempStats();
    }
    
    public void addTempStat(EquipStat type, int value) {
        if (tempstats.containsKey(type))
            tempstats.put(type, (short) (tempstats.get(type) + value));
        else
            tempstats.put(type, (short) value);
    }
    
    public void recalcTempStats() {
        clearPotStats();
        tempstats.clear();
        for (int i = 0; i < potentials.length; i++) {
            addTempStat(EquipStat.values()[potentials[i]%100], getPotentialValue(i));
        }
        addPotStats();
    }
    
    public void addPotStats() {
        if (!potAdded) {
            for (Map.Entry<EquipStat, Short> line : tempstats.entrySet()) {
                addStat(line.getKey(), line.getValue());
            }
            potAdded = true;
        }
    }
    
    public void clearPotStats() {
        if (potAdded) {
            for (Map.Entry<EquipStat, Short> line : tempstats.entrySet()) {
                addStat(line.getKey(), -line.getValue());
            }
            potAdded = false;
        }
    }
    
    public short getTempStat(EquipStat type) {
        return tempstats.containsKey(type)?tempstats.get(type):0;
    }
    
    public void removePotential() {
        for (int i = 0; i < potentials.length; i++) {
            this.potentials[i] = 0;
        }
        recalcTempStats();
    }
    
    private boolean isElligible(int potId) {
        if (potId == -1)
            return false;
        return (potId <= 5) || (potId == 6 && getStat(EquipStat.WATK) > 0) || (potId == 7 && getStat(EquipStat.MAGIC) > 0)  || (potId == 8 && ItemConstants.isWeapon(getItemId()));
    }
    
    public String potentialName(int line) {
        if (potentials[line] == 0)
            return "";
        
        if (potentials[line]%100 == 8)
            return Potential.values()[potentials[line]%100].toString().concat(" ").concat(String.valueOf(getPotentialValue(line))).concat("%");
        else
            return Potential.values()[potentials[line]%100].toString().concat(" +").concat(String.valueOf(getPotentialValue(line)));
    }
    
    public String rankName() {
        return (getPotential(0)/1000 == 5)?"Godly":(getPotential(0)/1000 == 4)?"Legendary":(getPotential(0)/1000 == 3)?"Unique":(getPotential(0)/1000 == 2)?"Epic":"Rare";
    }
    
    public void setPotential(int potId, int line) {
        this.potentials[line] = potId;
    }
    
    public int getLines() {
        return potentials.length;
    }

    public void setRingId(int id) {
        this.ringid = id;
    }

    public boolean isWearing() {
        return wear;
    }

    public void wear(boolean yes) {
        wear = yes;
    }

    public byte getItemLevel() {
        return itemLevel;
    }
}