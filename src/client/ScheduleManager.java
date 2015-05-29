/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.properties.MapleJob;
import client.properties.MapleQuestStatus;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import constants.ItemConstants;
import constants.skills.DarkKnight;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import server.MapleInventoryManipulator;
import server.MapleStatEffect;
import server.TimerManager;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;

/**
 * JourneyMS
 * 
 */
public final class ScheduleManager {

    private final MapleCharacter chr;
    private ScheduledFuture<?> dragonBlood, beholderRecovery, beholderBuff, berserk, hpDecr, expire, recovery, fishing, mapTimeLimit, pendantexp;
    private final List<ScheduledFuture<?>> timers = new ArrayList<>();
    private final ScheduledFuture<?>[] fullnessSchedule = new ScheduledFuture<?>[3];
    
    public ScheduleManager(MapleCharacter chr) {
        this.chr = chr;
    }
    
    public void scheduleHpDecr() {
        hpDecr = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                chr.doHurtHp();
            }
        }, 10000);
    }
    
    public void scheduleBerserk() {
        if (berserk != null) {
            berserk.cancel(false);
        }
        
        if (chr.getJob() == MapleJob.DARKKNIGHT) {
            Skill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = chr.getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                final boolean zerking = chr.getHp() * 100 / chr.getMaxHp() < BerserkX.getEffect(skilllevel).getX();
                berserk = TimerManager.getInstance().register(new Runnable() {
                    @Override
                    public void run() {
                        chr.getClient().announce(MaplePacketCreator.showOwnBerserk(skilllevel, zerking));
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBerserk(chr.getId(), skilllevel, zerking), false);
                    }
                }, 5000, 3000);
            }
        }
    }
    
    public void scheduleRecovery(int x) {
        final byte heal = (byte) x;
        recovery = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                chr.addStat(MapleStat.HP, heal);
                chr.getClient().announce(MaplePacketCreator.showOwnRecovery(heal));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showRecovery(chr.getId(), heal), false);
            }
        }, 5000, 5000);
    }   
    
    public void scheduleFullness(final int decrease, final MaplePet pet, int petSlot) {
        ScheduledFuture<?> schedule;
        schedule = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                int newFullness = pet.getFullness() - decrease;
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    pet.saveToDb();
                    chr.unequipPet(pet, true);
                } else {
                    pet.setFullness(newFullness);
                    pet.saveToDb();
                    Item petz = chr.getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
                    chr.forceUpdateItem(petz);
                }
            }
        }, 180000, 18000);
        fullnessSchedule[petSlot] = schedule;
    }
    
    public void scheduleTimeLimit(final MapleQuest quest, int time) {
        ScheduledFuture<?> sf = TimerManager.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                chr.announce(MaplePacketCreator.questExpire(quest.getId()));
                MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
                newStatus.setForfeited(chr.getQuest(quest).getForfeited() + 1);
                chr.updateQuest(newStatus);
            }
        }, time);
        chr.announce(MaplePacketCreator.addQuestTimeLimit(quest.getId(), time));
        timers.add(sf);
    }
    
    public void scheduleBeholder() {
        final int beholder = DarkKnight.BEHOLDER;
        cancelBeholder();
        Skill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
        int bHealingLvl = chr.getSkillLevel(bHealing);
        if (bHealingLvl > 0) {
            final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
            int healInterval = healEffect.getX() * 1000;
            beholderRecovery = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    chr.addStat(MapleStat.HP, healEffect.getHp());
                    chr.getClient().announce(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.summonSkill(chr.getId(), beholder, 5), true);
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showOwnBuffEffect(beholder, 2), false);
                }
            }, healInterval, healInterval);
        }
        Skill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
        if (chr.getSkillLevel(bBuff) > 0) {
            final MapleStatEffect buffEffect = bBuff.getEffect(chr.getSkillLevel(bBuff));
            int buffInterval = buffEffect.getX() * 1000;
            beholderBuff = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    buffEffect.applyTo(chr);
                    chr.getClient().announce(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.summonSkill(chr.getId(), beholder, (int) (Math.random() * 3) + 6), true);
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), beholder, 2), false);
                }
            }, buffInterval, buffInterval);
        }
    }
    
    public void scheduleExpiration() {
        if (expire == null) {
            expire = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    long expiration, currenttime = System.currentTimeMillis();
                    Set<Skill> keys = chr.getSkills().keySet();
                    for (Skill key : keys) {
                        MapleCharacter.SkillEntry skill = chr.getSkills().get(key);
                        if (skill.expiration != -1 && skill.expiration < currenttime) {
                            chr.changeSkillLevel(key, (byte) -1, 0, -1);
                        }
                    }

                    List<Item> toberemove = new ArrayList<>();
                    for (MapleInventory inv : chr.getInventories()) {
                        for (Item item : inv.list()) {
                            expiration = item.getExpiration();
                            if (expiration != -1 && (expiration < currenttime) && ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
                                byte aids = item.getFlag();
                                aids &= ~(ItemConstants.LOCK);
                                item.setFlag(aids); //Probably need a check, else people can make expiring items into permanent items...
                                item.setExpiration(-1);
                                chr.forceUpdateItem(item);   //TEST :3
                            } else if (expiration != -1 && expiration < currenttime) {
                                chr.getClient().announce(MaplePacketCreator.itemExpired(item.getItemId()));
                                toberemove.add(item);
                            }
                        }
                        for (Item item : toberemove) {
                            MapleInventoryManipulator.removeFromSlot(chr.getClient(), inv.getType(), item.getPosition(), item.getQuantity(), true);
                        }
                        toberemove.clear();
                    }
                }
            }, 60000);
        }
    }
    
    public void scheduleDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBlood != null) {
            dragonBlood.cancel(false);
        }
        dragonBlood = TimerManager.getInstance().register(new Runnable() {
            @Override
            public void run() {
                chr.decreaseHpMp(MapleStat.HP, bloodEffect.getX());
                chr.getClient().announce(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), bloodEffect.getSourceId(), 5), false);
                scheduleBerserk();
            }
        }, 4000, 4000);
    }
    
    public void schedulePendantExp() {
        if (pendantexp == null) {
            pendantexp = TimerManager.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    if (chr.getPendantExp() < 3) {
                        chr.addPendantExp();
                        chr.message("Pendant of the Spirit has been equipped for " + chr.getPendantExp() + " hour(s), you will now receive " + chr.getPendantExp() + "0% bonus exp.");
                    } else {
                        pendantexp.cancel(false);
                    }
                }
            }, 3600000); //1 hour
        }
    }
    
    public void scheduleFishing() { //Fishing System
        cancelFishing(); //Shouldnt be needed I think
        if (chr.getMapId() >= 970020000 && chr.getMapId() < 970020006) {
            if (chr.getItemQuantity(2270008, false) > 0) {
                fishing = TimerManager.getInstance().register(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (chr.getItemQuantity(2270008, false) > 0 && chr.getChair() == 3011000) {
                            if (chr.gainFishingReward())
                                MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, 2270008, 1, false, false);
                            else
                                fishing.cancel(false);
                            
                        } else {
                            chr.dropMessage(6, "You don't have any fishing baits left.");
                            fishing.cancel(false);
                        }
                    }
                }, 15000, 15000);
            } else {
                chr.dropMessage(6, "You don't have any fishing baits.");
            }
        }
    }
    
    public List<ScheduledFuture<?>> getTimeLimits() {
        return timers;
    }
    
    public void cancelAll() {
        if (dragonBlood != null) {
            dragonBlood.cancel(false);
        }
        if (hpDecr != null) {
            hpDecr.cancel(false);
        }
        if (beholderRecovery != null) {
            beholderRecovery.cancel(false);
        }
        if (beholderBuff != null) {
            beholderBuff.cancel(false);
        }
        if (berserk != null) {
            berserk.cancel(false);
        }
        if (recovery != null) {
            recovery.cancel(false);
        }
        if (fishing != null) {
            fishing.cancel(false);
        }
        cancelExpire();
        for (ScheduledFuture<?> sf : timers) {
            sf.cancel(false);
        }
        timers.clear();
    }
    
    public void cancelExpire() {
        if (expire != null) {
            expire.cancel(false);
            expire = null;
        }
    }
    
    public void cancelFishing() {
        if (fishing != null) {
            fishing.cancel(false);
        }
    }
    
    public void cancelPendantExp() {
        if (pendantexp != null) {
            pendantexp.cancel(false);
            pendantexp = null;
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimit != null) {
            mapTimeLimit.cancel(false);
        }
    }
    
    public void cancelFullnessSchedule(int petSlot) {
        if (fullnessSchedule[petSlot] != null) {
            fullnessSchedule[petSlot].cancel(false);
        }
    }
    
    public void cancelHpDecr() {
        if (hpDecr != null) {
            hpDecr.cancel(false);
        }
    }
    
    public void cancelBeholder() {
        if (beholderRecovery != null) {
            beholderRecovery.cancel(false);
            beholderRecovery = null;
        }
        
        if (beholderBuff != null) {
            beholderBuff.cancel(false);
            beholderBuff = null;
        }
    }
    
    public void cancelDragonBlood() {
        if (dragonBlood != null) {
            dragonBlood.cancel(false);
            dragonBlood = null;
        }
    }
    
    public void cancelRecovery() {
        if (recovery != null) {
            recovery.cancel(false);
            recovery = null;
        }
    }
}
