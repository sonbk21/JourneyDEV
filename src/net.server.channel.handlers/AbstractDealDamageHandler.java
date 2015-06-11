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

import client.properties.MapleBuffStat;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import client.*;
import client.autoban.AutobanFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ItemConstants;
import constants.skills.*;
import java.awt.Point;
import java.util.*;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.partyquest.Pyramid;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;

public abstract class AbstractDealDamageHandler extends AbstractMaplePacketHandler {

    public static class AttackInfo {

        public int numAttacked, numDamage, numAttackedAndDamage, skill, skilllevel, stance, direction, rangedirection, charge, display;
        public Map<Integer, List<Integer>> allDamage;
        public boolean isHH = false;
        public int speed = 4;
        
        public boolean isFamiliarAttack() {
            return skill == 3111005 || skill == 3211005 || skill == 2121005 || skill == 2221005 || skill == 3121006 || skill == 3221005 || skill == 2321003 || skill == 5211001 || skill == 5211002 || skill == 5220002 || skill == 5220003;
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr, Skill theSkill) {
            Skill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(GameConstants.getHiddenSkill(skill));
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (mySkill.getId() % 10000000 == 1020) {
                if (chr.getPartyQuest() instanceof Pyramid) {
                    if (((Pyramid) chr.getPartyQuest()).useSkill()) {
                        skillLevel = 1;
                    }
                }
            }
            if (skillLevel == 0) {
                return null;
            }
            if (display > 80) { //Hmm
                if (!theSkill.getAction()) {
                    AutobanFactory.FAST_ATTACK.execute(chr, "WZ Edit; adding action to a skill: " + display);
                    return null;
                }
            }
            return mySkill.getEffect(skillLevel);
        }
    }

    protected synchronized void applyAttack(AttackInfo attack, final MapleCharacter player, int attackCount, boolean magic) {
        Skill theSkill = null;
        MapleStatEffect attackEffect = null;
        try {
            if (player.isBanned()) {
                return;
            }

            if (attack.skill != 0) {
                theSkill = SkillFactory.getSkill(attack.skill);
                attackEffect = attack.getAttackEffect(player, theSkill);
                if (attackEffect == null) {
                    player.getClient().announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (player.getStat(MapleStat.MAXMP) < attackEffect.getMpCon()) {
                    AutobanFactory.MPCON.addPoint(player.getAutobanManager(), "Skill: " + attack.skill + "; Player MP: " + player.getStat(MapleStat.MP) + "; MP Needed: " + attackEffect.getMpCon());
                }

                if (attack.skill != Cleric.HEAL) {
                    if (player.isAlive()) {
                        attackEffect.applyTo(player);
                    } else {
                        player.getClient().announce(MaplePacketCreator.enableActions());
                    }
                }
                int mobCount = attackEffect.getMobCount();
                if (attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == Page.FINAL_ATTACK_BW || attack.skill == Page.FINAL_ATTACK_SWORD || attack.skill == Fighter.FINAL_ATTACK_SWORD
                        || attack.skill == Fighter.FINAL_ATTACK_AXE || attack.skill == Spearman.FINAL_ATTACK_SPEAR || attack.skill == Spearman.FINAL_ATTACK_POLEARM || attack.skill == WindArcher.FINAL_ATTACK
                        || attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == Hunter.FINAL_ATTACK || attack.skill == Crossbowman.FINAL_ATTACK) {
                    mobCount = 6;
                }
                if (attack.numAttacked > mobCount) {
                    AutobanFactory.MOB_COUNT.execute(player, "Skill: " + attack.skill + "; Count: " + attack.numAttacked + " Max: " + attackEffect.getMobCount());
                    return;
                }
            }
            if (!player.isAlive()) {
                return;
            }
            int totDamage = 0;
            final MapleMap map = player.getMap();

            if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
                int delay = 0;
                for (Integer oned : attack.allDamage.keySet()) {
                    MapleMapObject mapobject = map.getMapObject(oned.intValue());
                    if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                        final MapleMapItem mapitem = (MapleMapItem) mapobject;
                        if (mapitem.getMeso() > 9) {
                            synchronized (mapitem) {
                                if (mapitem.isPickedUp()) {
                                    return;
                                }
                                TimerManager.getInstance().schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        map.removeMapObject(mapitem);
                                        map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                        mapitem.setPickedUp(true);
                                    }
                                }, delay);
                                delay += 100;
                            }
                        } else if (mapitem.getMeso() == 0) {
                            return;
                        }
                    } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                }
            }
            for (Integer oned : attack.allDamage.keySet()) {
                final MapleMonster monster = map.getMonsterByOid(oned.intValue());
                if (monster != null) {
                    int totalDamage = 0;
                    List<Integer> onedList = attack.allDamage.get(oned);
                    for (final Integer eachd : onedList) {
                        totalDamage += eachd;
                    }
                    totDamage += totalDamage;
                    player.checkMonsterAggro(monster);
                    if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null && (attack.skill == 0 || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT || attack.skill == Shadower.BOOMERANG_STEP)) {
                        Skill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                        int delay = 0;
                        final int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET);
                        for (final Integer eachd : onedList) {
                            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                                TimerManager.getInstance().schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachd / (double) 20000) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getPosition().getY())), monster, player, true, (byte) 0);
                                    }
                                }, delay);
                                delay += 100;
                            }
                        }
                    } else if (attack.skill == Marksman.SNIPE) {
                        totalDamage = 195000 + Randomizer.nextInt(5000);
                    } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                        player.addStat(MapleStat.HP, (short) Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0), player.getStat(MapleStat.MAXHP) / 2)));
                    } else if (attack.skill == Bandit.STEAL) {
                        Skill steal = SkillFactory.getSkill(Bandit.STEAL);
                        if (Math.random() < 0.3 && steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) { //Else it drops too many cool stuff :(
                            List<MonsterDropEntry> toSteals = MapleMonsterInformationProvider.getInstance().retrieveDrop(monster.getId());
                            Collections.shuffle(toSteals);
                            int toSteal = toSteals.get(rand(0, (toSteals.size() - 1))).itemId;
                            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            Item item;
                            if (ItemConstants.getInventoryType(toSteal).equals(MapleInventoryType.EQUIP)) {
                                item = ii.randomizeStats((Equip) ii.getEquipById(toSteal));
                            } else {
                                item = new Item(toSteal, (byte) 0, (short) 1, -1);
                            }
                            player.getMap().spawnItemDrop(monster, player, item, monster.getPosition(), false, false);
                            monster.addStolen(toSteal);
                        }
                    } else if (attack.skill == FPArchMage.FIRE_DEMON) {
                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(FPArchMage.FIRE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(FPArchMage.FIRE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == ILArchMage.ICE_DEMON) {
                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(ILArchMage.ICE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(ILArchMage.ICE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == Outlaw.HOMING_BEACON || attack.skill == Corsair.BULLSEYE) {
                        player.setMarkedMonster(monster.getObjectId());
                        player.announce(MaplePacketCreator.giveBuff(1, attack.skill, Collections.singletonList(new Pair<>(MapleBuffStat.HOMING_BEACON, monster.getObjectId()))));
                    }
                    if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                        Skill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                        if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, hamstring.getEffect(player.getSkillLevel(hamstring)).getX()), hamstring, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, hamstring.getEffect(player.getSkillLevel(hamstring)).getY() * 1000);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                        Skill blind = SkillFactory.getSkill(Marksman.BLIND);
                        if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                        }
                    }
                    final int id = player.getMapleJob().getId();
                    if (id == 121 || id == 122) {
                        for (int charge = 1211005; charge < 1211007; charge++) {
                            Skill chargeSkill = SkillFactory.getSkill(charge);
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                final ElementalEffectiveness iceEffectiveness = monster.getEffectiveness(Element.ICE);
                                if (totalDamage > 0 && iceEffectiveness == ElementalEffectiveness.NORMAL || iceEffectiveness == ElementalEffectiveness.WEAK) {
                                    monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.FREEZE, 1), chargeSkill, null, false), false, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 2000);
                                }
                                break;
                            }
                        }
                    } else if (player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null || player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                        Skill skill;
                        if (player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
                            skill = SkillFactory.getSkill(21101003);
                            final MapleStatEffect eff = skill.getEffect(player.getSkillLevel(skill));

                            if (eff.makeChanceResult()) {
                                monster.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.NEUTRALISE, 1), skill, null, false), false, eff.getX() * 1000, false);
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                            skill = SkillFactory.getSkill(21100005);
                            player.addStat(MapleStat.HP, (short) ((totDamage * skill.getEffect(player.getSkillLevel(skill)).getX()) / 100));
                        }
                    } else if (id == 412 || id == 422 || id == 1411) {
                        Skill type = SkillFactory.getSkill(player.getMapleJob().getId() == 412 ? 4120005 : (player.getMapleJob().getId() == 1411 ? 14110004 : 4220005));
                        if (player.getSkillLevel(type) > 0) {
                            MapleStatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                            for (int i = 0; i < attackCount; i++) {
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.getVenomMulti() < 3) {
                                        monster.setVenomMulti((monster.getVenomMulti() + 1));
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                        monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                    }
                                }
                            }
                        }
                    }
                    if (attack.skill != 0) {
                        if (attackEffect.getFixDamage() != -1) {
                            if (totalDamage != attackEffect.getFixDamage() && totalDamage != 0) {
                                AutobanFactory.FIX_DAMAGE.execute(player, String.valueOf(totalDamage) + " damage");
                            }
                        }
                    }
                    if (totalDamage > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                        if (attackEffect.makeChanceResult()) {
                            monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, null, false), attackEffect.isPoison(), attackEffect.getDuration());
                        }
                    }
                    
                    if (totalDamage > player.getMaxPossibleDamage(attack.skill, attack.skilllevel) || totalDamage < 0) {
                        AutobanFactory.HIGHDAMAGE.execute(player, String.valueOf(totalDamage) + " damage");
                        return;
                    }
                    
                    if (!attack.isFamiliarAttack()) {
                        byte reflect = monster.getReflectLevel();
                        
                        if (reflect > 0) {
                            if ((!magic && monster.hasWatkReflect()) || (magic && monster.hasMagicReflect()))
                                player.decreaseHpMp(MapleStat.HP, (player.getStat(MapleStat.MAXHP)/5)*reflect);
                        }
                    }
                    
                    if (attack.isHH && !monster.isBoss()) {
                        map.damageMonster(player, monster, monster.getHp() - 1);
                    } else if (attack.isHH) {
                        int HHDmg = (player.getMaxBaseDamage() * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)));
                    } else {
                        if (player.getBossDamage() > 0)
                            totalDamage += (player.getBossDamage()/100) * totalDamage;
                        map.damageMonster(player, monster, totalDamage);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected AttackInfo parseDamage(LittleEndianAccessor lea, MapleCharacter chr, boolean ranged) {
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new HashMap<>();
        ret.skill = lea.readInt();
        if (ret.skill > 0) {
            ret.skilllevel = chr.getSkillLevel(ret.skill);
        }
        if (ret.skill == FPArchMage.BIG_BANG || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = 0;
        }
        if (ret.skill == Paladin.HEAVENS_HAMMER) {
            ret.isHH = true;
        }
        lea.skip(8);
        ret.display = lea.readByte();
        ret.direction = lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(10);
                int bullets = lea.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = lea.readInt();
                    lea.skip(1);
                    ret.allDamage.put(mesoid, null);
                }
                return ret;
            } else {
                lea.skip(6);
            }
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = lea.readInt();
                if (i < ret.numAttacked) {
                    lea.skip(12);
                    int bullets = lea.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<>();
                    for (int j = 0; j < bullets; j++) {
                        int damage = lea.readInt();
                        allDamageNumbers.add(damage);
                    }
                    ret.allDamage.put(oid, allDamageNumbers);
                    lea.skip(4);
                } else {
                    int bullets = lea.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = lea.readInt();
                        lea.skip(1);
                        ret.allDamage.put(mesoid, null);
                    }
                }
            }
            return ret;
        }
        if (ranged) {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.readByte();
            ret.rangedirection = lea.readByte();
            lea.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                lea.skip(4);
            }
        } else {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.skip(4);
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = lea.readInt();
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.numDamage; j++) {
                int damage = lea.readInt();
                if (ret.skill == Marksman.SNIPE) {
                    damage += 0x80000000; //Critical
                }
                allDamageNumbers.add(damage);
            }
            if (ret.skill != 5221004) {
                lea.skip(4);
            }
            ret.allDamage.put(oid, allDamageNumbers);
        }
        return ret;
    }

    private static int rand(int l, int u) {
        return (int) ((Math.random() * (u - l + 1)) + l);
    }
}
