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
        
        public boolean isInRange(EquipStat low, EquipStat high) {
            return compareTo(low) >= 0 && compareTo(high) <= 0;
        }
    }
    
    private final EnumMap<EquipStat, Short> stats = new EnumMap<>(EquipStat.class);
    private final EnumMap<EquipStat, Short> tempstats = new EnumMap<>(EquipStat.class);
    private final LinkedList<PotentialLine> potentials = new LinkedList<>();
    private short rank = 0;
    private short vicious;
    private byte upgradeSlots;
    private byte level, flag, itemLevel;
    private float itemExp;
    private int ringid = -1;
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
        ret.potentials.addAll(potentials);
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
    
    public int getRank() {
        return rank;
    }
    
    public int getPotentialForDB(int line) {
        return (potentials.size() > line)?(1000 * rank + 100 * potentials.get(line).getSuperior().ordinal() + potentials.get(line).getEffect().ordinal()):0;
    }
    
    public void resetPotential() {
        rank = (short) ((potentials.isEmpty())?1:(rank == 5)?5:rank + Randomizer.nextInt(26)/25);
        EquipStat newline = null;
        for (int i = 0; i < potentials.size(); i++) {
            do {
                newline = EquipStat.values()[Randomizer.nextInt(EquipStat.values().length)];
            } while (!isElligible(newline));
            potentials.add(new PotentialLine(newline));
            potentials.removeFirst();
        }
        if ((potentials.size() < 3 && Randomizer.nextInt(25) == 0) || potentials.isEmpty()) { //Lines increase or first potential
            do {
                newline = EquipStat.values()[Randomizer.nextInt(EquipStat.values().length)];
            } while (!isElligible(newline));
            potentials.add(new PotentialLine(newline));
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
        for (PotentialLine line : potentials) {
            addTempStat(line.getEffect(), line.getValue(rank));
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
    
    private boolean isElligible(EquipStat newline) {
        return (newline.ordinal() <= 5) || (getStat(newline) > 0) || (newline == EquipStat.HANDS && ItemConstants.isWeapon(getItemId()));
    }
    
    public String potentialName(int line) {
        if (potentials.get(line).getEffect() == EquipStat.HANDS)
            return "BDM".concat(" ").concat(String.valueOf(potentials.get(line).getValue(rank))).concat("%");
        else
            return potentials.get(line).getEffect().toString().concat(" +").concat(String.valueOf(potentials.get(line).getValue(rank)));
    }
    
    public String rankName() {
        return (rank == 5)?"Godly":(rank == 4)?"Legendary":(rank == 3)?"Unique":(rank == 2)?"Epic":"Rare";
    }
    
    public void addPotential(int potId) {
        this.potentials.add(new PotentialLine(potId));
    }
    
    public int getNumLines() {
        return potentials.size();
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