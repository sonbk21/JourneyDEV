/*
 * Copyright (C) 2015 SYJourney
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package client.inventory;

import client.MapleClient;
import constants.ItemConstants;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

/**
 * Author: SYJourney
 * This file is part of the Journey MMORPG Server
 */

public class Equip extends Item {
    
    private final EnumMap<EquipStat, Short> stats = new EnumMap<>(EquipStat.class);
    private final EnumMap<EquipStat, Short> tempstats = new EnumMap<>(EquipStat.class);
    private final LinkedList<PotentialLine> potentials = new LinkedList<>();
    private PotentialRank rank;
    
    private byte vicious;
    private byte upgradeSlots;
    private byte level, flag, itemLevel;
    private float itemExp;
    private int ringid = -1;
    
    private boolean isRevealed = false;
    private boolean isWearing = false;

    public enum EquipStat {
        STR, DEX, INT, LUK, HP, MP, WATK, MAGIC, WDEF, MDEF, ACC, AVOID, HANDS, SPEED, JUMP;
        
        public boolean isInRange(EquipStat low, EquipStat high) {
            return compareTo(low) >= 0 && compareTo(high) <= 0;
        }
    }
    
    public enum ScrollResult {
        FAIL, SUCCESS, CURSE;
    }
    
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
        ret.stats.putAll(stats);
        ret.potentials.addAll(potentials);
        ret.rank = rank;
        ret.isRevealed = isRevealed;
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
    
    public EnumMap<EquipStat, Short> getAllTempStats() {
        return tempstats;
    }
    
    public boolean hasNoStats() {
        return stats.entrySet().stream().noneMatch((singlestat) -> (singlestat.getValue() > 0));
    }
    
    public void clearStats() {
        for (EquipStat stat : EquipStat.values()) {
            stats.put(stat, (short) 0);
        }
    }
    
    public void setStat(EquipStat type, short value) {
        stats.put(type, value);
    }

    public short getVicious() {
        return vicious;
    }

    @Override
    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public void setVicious(byte vicious) {
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
                case "incSTR":
                    stats.put(EquipStat.STR, (short) (getStat(EquipStat.STR) + stat.getRight()));
                    break;
                case "incDEX":
                    stats.put(EquipStat.DEX, (short) (getStat(EquipStat.DEX) + stat.getRight()));
                    break;
                case "incINT":
                    stats.put(EquipStat.INT, (short) (getStat(EquipStat.INT) + stat.getRight()));
                    break;
                case "incLUK":
                    stats.put(EquipStat.LUK, (short) (getStat(EquipStat.LUK) + stat.getRight()));
                    break;
                case "incMHP":
                    stats.put(EquipStat.HP, (short) (getStat(EquipStat.HP) + stat.getRight()));
                    break;
                case "incMMP":
                    stats.put(EquipStat.MP, (short) (getStat(EquipStat.MP) + stat.getRight()));
                    break;
                case "incPAD":
                    stats.put(EquipStat.WATK, (short) (getStat(EquipStat.WATK) + stat.getRight()));
                    break;
                case "incMAD":
                    stats.put(EquipStat.MAGIC, (short) (getStat(EquipStat.MAGIC) + stat.getRight()));
                    break;
                case "incPDD":
                    stats.put(EquipStat.WDEF, (short) (getStat(EquipStat.WDEF) + stat.getRight()));
                    break;
                case "incMDD":
                    stats.put(EquipStat.MDEF, (short) (getStat(EquipStat.MDEF) + stat.getRight()));
                    break;
                case "incEVA":
                    stats.put(EquipStat.AVOID, (short) (getStat(EquipStat.AVOID) + stat.getRight()));
                    break;
                case "incACC":
                    stats.put(EquipStat.ACC, (short) (getStat(EquipStat.ACC) + stat.getRight()));
                    break;
                case "incSPEED":
                    stats.put(EquipStat.SPEED, (short) (getStat(EquipStat.SPEED) + stat.getRight()));
                    break;
                case "incJUMP":
                    stats.put(EquipStat.JUMP, (short) (getStat(EquipStat.JUMP) + stat.getRight()));
                    break;
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
        this.vicious = (byte) i;
    }

    public int getRingId() {
        return ringid;
    }
    
    public PotentialRank getRank() {
        return rank;
    }
    
    public int getPotentialForDB(int line) {
        if (rank == PotentialRank.NULL || potentials.size() <= line) {
            return 0;
        }
        return 1000 * rank.ordinal() + 100 * potentials.get(line).getSuperior().ordinal() + potentials.get(line).getEffect().ordinal();
    }
    
    public void resetPotential() {
        rank = rank.reset(25, 5);
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
    
    public boolean isRevealed() {
        return isRevealed;
    }
    
    public void setRevealed(boolean revealed) {
        this.isRevealed = revealed;
    }
    
    public void addTempStat(EquipStat type, int value) {
        if (tempstats.containsKey(type)) {
            tempstats.put(type, (short) (tempstats.get(type) + value));
        } else {
            tempstats.put(type, (short) value);
        }
    }
    
    public void recalcTempStats() {
        tempstats.clear();
        potentials.stream().forEach((line) -> addTempStat(line.getEffect(), line.getValue(rank)));
        isRevealed = false;
    }
    
    public short getTempStat(EquipStat type) {
        return tempstats.containsKey(type)?tempstats.get(type):0;
    }
    
    private boolean isElligible(EquipStat newline) {
        return (getStat(newline) > 0) || (newline == EquipStat.HANDS && ItemConstants.isWeapon(getItemId()));
    }
    
    public String potentialName(int line) {
        if (potentials.get(line).getEffect() == EquipStat.HANDS) {
            return "BDM".concat(" ").concat(String.valueOf(potentials.get(line).getValue(rank))).concat("%");
        } else {
            return potentials.get(line).getEffect().toString().concat(" +").concat(String.valueOf(potentials.get(line).getValue(rank)));
        }
    }
    
    public String getRankName() {
        return rank.toString().substring(0, 1) + rank.toString().toLowerCase().substring(1);
    }
    
    public void addPotential(int potId) {
        if (potId > 0) {
            this.potentials.add(new PotentialLine(potId));
        }
    }
    
    public int getNumLines() {
        return potentials.size();
    }
    
    public void setRank(int rank) {
        this.rank = PotentialRank.values()[rank];
    }

    public void setRingId(int id) {
        this.ringid = id;
    }

    public boolean isWearing() {
        return isWearing;
    }

    public void wear(boolean yes) {
        isWearing = yes;
    }

    public byte getItemLevel() {
        return itemLevel;
    }
}
