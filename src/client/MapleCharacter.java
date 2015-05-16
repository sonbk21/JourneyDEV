
/* 
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program unader any cother version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import client.Achievements.Achievement;
import client.Achievements.Status;
import client.autoban.AutobanManager;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import client.inventory.ModifyInventory;
import client.properties.BuddylistEntry;
import client.properties.DiseaseValueHolder;
import client.properties.MapleBuffStat;
import client.properties.MapleDisease;
import client.properties.MapleJob;
import client.properties.MapleKeyBinding;
import client.properties.MapleQuestStatus;
import client.properties.MapleSkinColor;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import client.properties.SkillMacro;
import constants.ExpTable;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.skills.Bishop;
import constants.skills.BlazeWizard;
import constants.skills.Corsair;
import constants.skills.Crusader;
import constants.skills.DarkKnight;
import constants.skills.DawnWarrior;
import constants.skills.FPArchMage;
import constants.skills.GM;
import constants.skills.Hermit;
import constants.skills.ILArchMage;
import constants.skills.Magician;
import constants.skills.Marauder;
import constants.skills.Priest;
import constants.skills.Ranger;
import constants.skills.Sniper;
import constants.skills.Spearman;
import constants.skills.SuperGM;
import constants.skills.Swordsman;
import constants.skills.ThunderBreaker;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.server.PlayerBuffValueHolder;
import net.server.PlayerCoolDownValueHolder;
import net.server.PlayerDiseaseValueHolder;
import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleMessenger;
import net.server.world.MapleMessengerCharacter;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.PartyOperation;
import net.server.world.World;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import server.CashShop;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.MapleStorage;
import server.TimerManager;
import server.events.RescueGaga;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapEffect;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleSummon;
import server.maps.PlayerNPCs;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

/*
* AbstractMapleCharacter is just a file that contains getter/setter and other simpler methods. 
* I tried to reduce the no. of lines in this file with it. But mabye it'll become actually useful If I try to make different kinds of characters.
* This is not finished at all.
*/

public class MapleCharacter extends AbstractMapleCharacter {

    private MapleCharacter() {
        setStance(0);
        inventory = new MapleInventory[MapleInventoryType.values().length];
        savedLocations = new SavedLocation[SavedLocationType.values().length];

        for (MapleInventoryType type : MapleInventoryType.values()) {
            byte b = 24;
            if (type == MapleInventoryType.CASH) {
                b = 96;
            }
            inventory[type.ordinal()] = new MapleInventory(type, (byte) b);
        }
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = null;
        }
        quests = new LinkedHashMap<>();
        setPosition(new Point(0, 0));
    }

    public static MapleCharacter getDefault(MapleClient c) {
        MapleCharacter ret = new MapleCharacter();
        ret.client = c;
        ret.gmLevel = c.gmLevel();
        int[] defaultStats = {1, 12, 5, 4, 4, 50, 50, 5, 5};
        ret.stats = new PlayerStats(defaultStats, 3);
        ret.accountid = c.getAccID();
        ret.buddylist = new BuddyList(20);
        ret.maplemount = null;
        ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(24);
        ret.getInventory(MapleInventoryType.USE).setSlotLimit(24);
        ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(24);
        ret.getInventory(MapleInventoryType.ETC).setSlotLimit(24);
        
        for (int i = 0; i < DEFAULT_KEY.length; i++) {
            ret.keymap.put(DEFAULT_KEY[i], new MapleKeyBinding(DEFAULT_TYPE[i], DEFAULT_ACTION[i]));
        }
        
        for (int i = 0; i < 5; i++) {
            ret.trockmaps[i] = 999999999;
        }
        
        for (int i = 0; i < 10; i++) {
            ret.viptrockmaps[i] = 999999999;
        }
        
        return ret;
    }

    public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        if (this.coolDowns.containsKey(skillId)) {
            this.coolDowns.remove(skillId);
        }
        this.coolDowns.put(skillId, new MapleCoolDownValueHolder(skillId, startTime, length, timer));
    }
    
    public void addAchievement(Achievement avm, int value) { //Achievement functions
        if (achievements.getStatus(avm).equals(Status.NULL) && value != -1)
            dropMessage(6, "New Achievement in progress: "+avm.getText(false));
        if (achievements.add(avm, value)) {
            dropMessage(6, "Achievement obtained! "+avm.getText(false)+" Use ~admin to claim your trophy!");
            saveToDB();
        }
    }
    
    public void addAchievementString(String name, int value) {
        addAchievement(Achievement.valueOf(name), value);
    }
    
    public void clearTrophies() {
        achievements.clearTrophies();
        saveToDB();
    } //End
    
    public void ban(String reason, int greason) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ?, greason = ? WHERE id = ?")) {
                ps.setString(1, reason);
                ps.setInt(2, greason);
                ps.setInt(3, accountid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
        }
    }

    public static boolean ipban(String id, String reason, boolean accountId) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }

            boolean ret = false;
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psb = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
                        psb.setString(1, reason);
                        psb.setInt(2, rs.getInt(1));
                        psb.executeUpdate();
                    }
                    ret = true;
                }
            }
            ps.close();
            return ret;
        } catch (SQLException ex) {
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return false;
    }
    
    public void block(int reason, int days, String desc) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);
        Timestamp TS = new Timestamp(cal.getTimeInMillis());
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banreason = ?, tempban = ?, greason = ? WHERE id = ?")) {
                ps.setString(1, desc);
                ps.setTimestamp(2, TS);
                ps.setInt(3, reason);
                ps.setInt(4, accountid);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
        }        
    }

    public void cancelAllBuffs(boolean disconnect) {
        if (disconnect) {
            effects.clear();
        } else {
            for (MapleBuffStatValueHolder mbsvh : new ArrayList<>(effects.values())) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void cancelBuffStats(MapleBuffStat stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void newClient(MapleClient c) {
        this.loggedIn = true;
        c.setAccountName(this.client.getAccountName());//No null's for accountName
        this.client = c;
        MaplePortal portal = map.findClosestSpawnpoint(getPosition());
        if (portal == null) {
            portal = map.getPortal(0);
        }
        this.setPosition(portal.getPosition());
        this.initialSpawnPoint = portal.getId();
        this.map = c.getChannelServer().getMapFactory().getMap(getMapId());
    }

    @Override
    public void cancelBuffEffects() {
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            mbsvh.schedule.cancel(false);
        }
        this.effects.clear();
    }

    public static class CancelCooldownAction implements Runnable {

        private final int skillId;
        private final WeakReference<MapleCharacter> target;

        public CancelCooldownAction(MapleCharacter target, int skillId) {
            this.target = new WeakReference<>(target);
            this.skillId = skillId;
        }

        
        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.removeCooldown(skillId);
                realTarget.client.announce(MaplePacketCreator.skillCooldown(skillId, 0));
            }
        }
    }

    public void cancelEffect(int itemId) {
        cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(itemId), false, -1);
    }

    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<MapleBuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<>(statups.size());
            for (Pair<MapleBuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            if (!getDoors().isEmpty()) {
                MapleDoor door = getDoors().iterator().next();
                for (MapleCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (MapleCharacter chr : door.getTown().getCharacters()) {
                    door.sendDestroyData(chr.client);
                }
                for (MapleDoor destroyDoor : getDoors()) {
                    door.getTarget().removeMapObject(destroyDoor);
                    door.getTown().removeMapObject(destroyDoor);
                }
                setClearDoors();
                silentPartyUpdate();
            }
        }
        if (effect.getSourceId() == Spearman.HYPER_BODY || effect.getSourceId() == GM.HYPER_BODY || effect.getSourceId() == SuperGM.HYPER_BODY) {
            List<Pair<MapleStat, Integer>> statup = new ArrayList<>(4);
            statup.add(new Pair<>(MapleStat.HP, Math.min(stats.getStat(MapleStat.HP), stats.getStat(MapleStat.MAXHP))));
            statup.add(new Pair<>(MapleStat.MP, Math.min(stats.getStat(MapleStat.MP), stats.getStat(MapleStat.MAXMP))));
            statup.add(new Pair<>(MapleStat.MAXHP, stats.getStat(MapleStat.MAXHP)));
            statup.add(new Pair<>(MapleStat.MAXMP, stats.getStat(MapleStat.MAXMP)));
            client.announce(MaplePacketCreator.updatePlayerStats(statup));
        }
        if (effect.isMonsterRiding()) {
            if (effect.getSourceId() != Corsair.BATTLE_SHIP) {
                this.getMount().cancelSchedule();
                this.getMount().setActive(false);
            }
        }
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);
        }
    }

    @Override
    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        MapleBuffStatValueHolder effect = effects.get(stat);
        if (effect != null) {
            cancelEffect(effect.effect, false, -1);
        }
    }

    public void Hide(boolean hide, boolean login) {
        if (isGM() && hide != this.hidden) {
            if (!hide) {
                this.hidden = false;
                announce(MaplePacketCreator.getGMEffect(0x10, (byte) 0));
                getMap().broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                updatePartyMemberHP();
            } else {
                this.hidden = true;
                announce(MaplePacketCreator.getGMEffect(0x10, (byte) 1));
                if (!login) {
                    getMap().broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
                }
            }
            announce(MaplePacketCreator.enableActions());
        }        
    }
    
    public void Hide(boolean hide) {
        Hide(hide, false);
    }
    
    public void toggleHide(boolean login) {
        Hide(!isHidden());
    }

    private void cancelFullnessSchedule(int petSlot) {
        if (fullnessSchedule[petSlot] != null) {
            fullnessSchedule[petSlot].cancel(false);
        }
    }

    public void cancelMagicDoor() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<>(effects.values())) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
        }
    }

    @Override
    protected void cancelPlayerBuffs(List<MapleBuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            recalcLocalStats();
            enforceMaxHpMp();
            client.announce(MaplePacketCreator.cancelBuff(buffstats));
            if (buffstats.size() > 0) {
                getMap().broadcastMessage(this, MaplePacketCreator.cancelForeignBuff(getId(), buffstats), false);
            }
        }
    }

    public static boolean canCreateChar(String name) {
        if (name.length() < 4 || name.length() > 12) {
            return false;
        }

        if (checkAvailableName(name)) {
            return false;
        }

        return getIdByName(name) < 0 && !name.toLowerCase().contains("gm") && Pattern.compile("[a-zA-Z0-9_-]{3,12}").matcher(name).matches();
    }

    
    public void changeJob(MapleJob newJob) {
        setJob(newJob);
        this.remainingSp++;
        if (newJob.getId() % 10 == 2) {
            this.remainingSp += 2;
        }
        if (newJob.getId() % 10 > 1) {
            this.remainingAp += 5;
        }
        int job_ = getStat(MapleStat.JOB) % 1000;
        if (job_ == 100) {
            addStat(MapleStat.MAXHP, Randomizer.rand(200, 250));
        } else if (job_ == 200) {
            addStat(MapleStat.MAXMP,Randomizer.rand(100, 150));
        } else if (job_ % 100 == 0) {
            addStat(MapleStat.MAXHP, Randomizer.rand(100, 150));
            addStat(MapleStat.MAXMP, Randomizer.rand(25, 50));
        } else if (job_ > 0 && job_ < 200) {
            addStat(MapleStat.MAXHP, Randomizer.rand(300, 350));
        } else if (job_ < 300) {
            addStat(MapleStat.MAXMP,Randomizer.rand(450, 500));
        } //handle KoC here (undone)
        else if (job_ > 0 && job_ != 1000) {
            addStat(MapleStat.MAXHP, Randomizer.rand(300, 350));
            addStat(MapleStat.MAXMP,Randomizer.rand(150, 200));
        }
        if (!isGM()) {
            for (byte i = 1; i < 5; i++) {
                gainSlots(i, 4, true);
            }
        }
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(5);
        statup.add(new Pair<>(MapleStat.MAXHP, stats.getStat(MapleStat.MAXHP)));
        statup.add(new Pair<>(MapleStat.MAXMP, stats.getStat(MapleStat.MAXMP)));
        statup.add(new Pair<>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<>(MapleStat.AVAILABLESP, remainingSp));
        statup.add(new Pair<>(MapleStat.JOB, getStat(MapleStat.JOB)));
        recalcLocalStats();
        client.announce(MaplePacketCreator.updatePlayerStats(statup));
        silentPartyUpdate();
        if (this.guildid > 0) {
            getGuild().broadcast(MaplePacketCreator.jobMessage(0, getStat(MapleStat.JOB), name), this.getId());
        }
        guildUpdate();
        getMap().broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 8), false);
    }

    public void changeKeybinding(int key, MapleKeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keymap.put(key, keybinding);
        } else {
            keymap.remove(key);
        }
    }

    
    public void changeMap(int map) {
        changeMap(map, 0);
    }

    
    public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    
    public void changeMap(int map, String portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    
    public void changeMap(int map, MaplePortal portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, portal);
    }

    
    public void changeMap(MapleMap to) {
        changeMap(to, to.getPortal(0));
    }

    
    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this));
    }

    
    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(to, 0x80, this));//Position :O (LEFT)
    }

    
    public void changeMapBanish(int mapid, String portal, String msg) {
        dropMessage(5, msg);
        MapleMap map_ = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map_, map_.getPortal(portal));
    }

    
    protected void changeMapInternal(final MapleMap to, final Point pos, final byte[] warpPacket) {
        if (getEventInstance() != null) {
            getEventInstance().mapChanged(this, to.getId());
        }
        client.announce(warpPacket);
        map.removePlayer(MapleCharacter.this);
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
            map = to;
            setPosition(pos);
            map.addPlayer(MapleCharacter.this);
            if (party != null) {
                mpc.setMapId(to.getId());
                silentPartyUpdate();
                client.announce(MaplePacketCreator.updateParty(client.getChannel(), party, PartyOperation.SILENT_UPDATE, null));
                updatePartyMemberHP();
            }
            if (getMap().getHPDec() > 0) {
                hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {
                    
                    @Override
                    public void run() {
                        doHurtHp();
                    }
                }, 10000);
            }
        }
    }

    
    public void changeSkillLevel(Skill skill, byte newLevel, int newMasterlevel, long expiration) {
        if (newLevel > -1) {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            this.client.announce(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, expiration));
        } else {
            skills.remove(skill);
            this.client.announce(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel, -1)); //Shouldn't use expiration anymore :)
            try {
                Connection con = DatabaseConnection.getConnection();
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = ? AND characterid = ?")) {
                    ps.setInt(1, skill.getId());
                    ps.setInt(2, id);
                    ps.execute();
                }
            } catch (SQLException ex) {
                System.out.print("Error deleting skill: " + ex);
            }
        }
    }

    public static Map<String, String> getCharacterFromDatabase(String name) {
        Map<String, String> character = new LinkedHashMap<>();

        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `id`, `accountid`, `name` FROM `characters` WHERE `name` = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }

                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        character.put(rs.getMetaData().getColumnLabel(i), rs.getString(i));
                    }
                }
            }
        } catch (SQLException sqle) {
            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, sqle);
        }

        return character;
    }

    public static boolean checkAvailableName(String name) {
        return getCharacterFromDatabase(name) != null;
    }
    
    public void checkBerserk() {
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        final MapleCharacter chr = this;
        if (getMapleJob().equals(MapleJob.DARKKNIGHT)) {
            Skill BerserkX = SkillFactory.getSkill(DarkKnight.BERSERK);
            final int skilllevel = getSkillLevel(BerserkX);
            if (skilllevel > 0) {
                Berserk = chr.stats.getStat(MapleStat.HP) * 100 / chr.stats.getStat(MapleStat.MAXHP) < BerserkX.getEffect(skilllevel).getX();
                BerserkSchedule = TimerManager.getInstance().register(new Runnable() {
                    
                    @Override
                    public void run() {
                        client.announce(MaplePacketCreator.showOwnBerserk(skilllevel, Berserk));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBerserk(getId(), skilllevel, Berserk), false);
                    }
                }, 5000, 3000);
            }
        }
    }
    
    public void startFishing() { //Fishing System
        cancelFishing(); //Shouldnt be needed I think
        if (mapid >= 970020000 && mapid < 970020006) {
            if (getItemQuantity(2270008, false) > 0) {
                fishingTask = TimerManager.getInstance().register(new Runnable() {
                    
                    @Override
                    public void run() {
                        if (getItemQuantity(2270008, false) > 0 && chair == 3011000) {
                            if (gainFishingReward())
                                MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, 2270008, 1, false, false);
                            else
                                fishingTask.cancel(false);
                            
                        } else {
                            dropMessage(6, "You don't have any fishing baits left.");
                            fishingTask.cancel(false);
                        }
                    }
                }, 15000, 15000);
            } else {
                dropMessage(6, "You don't have any fishing baits.");
            }
        }
    }
    
    public void cancelFishing() {
        if (fishingTask != null) {
            fishingTask.cancel(false);
        }
    }
    
    private boolean gainFishingReward() {
        if (getInventory(MapleInventoryType.USE).isFull() || getInventory(MapleInventoryType.EQUIP).isFull() || getInventory(MapleInventoryType.ETC).isFull()) {
            dropMessage(6, "Please make some space in your inventory.");
            return false;
        }
        int reward = fishingRewards[Randomizer.nextInt(fishingRewards.length)];
        int randomComp;
        switch (reward) {
            case 0: 
                gainMeso(Randomizer.nextInt(90001)+10000, true, true, true); break;
            case 1: 
                gainExp(Randomizer.nextInt(ExpTable.getExpNeededForLevel(stats.getStat(MapleStat.LEVEL))/(40*(30/Math.max(stats.getStat(MapleStat.LEVEL), 15)))), true, true, true); break;
            case 2:
                randomComp = Randomizer.nextInt(5);
                MapleInventoryManipulator.addById(client, 4001126, (short) randomComp);
                client.announce(MaplePacketCreator.getShowItemGain(reward, (short) randomComp, true));
                break;
            case 3:
                randomComp = (Randomizer.nextInt(5) == 0)?(Randomizer.nextInt(10) == 0)?(Randomizer.nextInt(25) == 0)?4280002:4280000:4280001:4001126;
                MapleInventoryManipulator.addById(client, randomComp, (short) 1);
                client.announce(MaplePacketCreator.getShowItemGain(randomComp, (short) 1, true));
                break;
            default: 
                MapleInventoryManipulator.addById(client, reward, (short) 1);
                client.announce(MaplePacketCreator.getShowItemGain(reward, (short) 1, true));
        }
        return true;
    } //End of modifications
    
    public void checkMessenger() {
        if (messenger != null && messengerposition < 4 && messengerposition > -1) {
            World worldz = Server.getInstance().getWorld(world);
            worldz.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(this, messengerposition), messengerposition);
            worldz.updateMessenger(getMessenger().getId(), name, client.getChannel());
        }
    }

    
    public void checkMonsterAggro(MapleMonster monster) {
        if (!monster.isControllerHasAggro()) {
            if (monster.getController() == this) {
                monster.setControllerHasAggro(true);
            } else {
                monster.switchController(this, true);
            }
        }
    }

    
    public void controlMonster(MapleMonster monster, boolean aggro) {
        monster.setController(this);
        controlled.add(monster);
        client.announce(MaplePacketCreator.controlMonster(monster, false, aggro));
    }

    
    public void decreaseBattleshipHp(int decrease) {
        this.battleshipHp -= decrease;
        if (battleshipHp <= 0) {
            this.battleshipHp = 0;
            Skill battleship = SkillFactory.getSkill(Corsair.BATTLE_SHIP);
            int cooldown = battleship.getEffect(getSkillLevel(battleship)).getCooldown();
            announce(MaplePacketCreator.skillCooldown(Corsair.BATTLE_SHIP, cooldown));
            addCooldown(Corsair.BATTLE_SHIP, System.currentTimeMillis(), cooldown, TimerManager.getInstance().schedule(new CancelCooldownAction(this, Corsair.BATTLE_SHIP), cooldown * 1000));
            removeCooldown(5221999);
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        } else {
            announce(MaplePacketCreator.skillCooldown(5221999, battleshipHp / 10));   //:D
            addCooldown(5221999, 0, battleshipHp, null);
        }
    }

    
    public void deleteGuild(int guildId) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?")) {
                ps.setInt(1, guildId);
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?")) {
                ps.setInt(1, id);
                ps.execute();
            }
        } catch (SQLException ex) {
            System.out.print("Error deleting guild: " + ex);
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int cid) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cid);
            ps.executeUpdate();
        }
    }

    private void deregisterBuffStats(List<MapleBuffStat> stats) {
        synchronized (stats) {
            List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
            for (MapleBuffStat stat : stats) {
                MapleBuffStatValueHolder mbsvh = effects.get(stat);
                if (mbsvh != null) {
                    effects.remove(stat);
                    boolean addMbsvh = true;
                    for (MapleBuffStatValueHolder contained : effectsToCancel) {
                        if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                            addMbsvh = false;
                        }
                    }
                    if (addMbsvh) {
                        effectsToCancel.add(mbsvh);
                    }
                    if (stat == MapleBuffStat.RECOVERY) {
                        if (recoveryTask != null) {
                            recoveryTask.cancel(false);
                            recoveryTask = null;
                        }
                    } else if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET) {
                        int summonId = mbsvh.effect.getSourceId();
                        MapleSummon summon = summons.get(summonId);
                        if (summon != null) {
                            getMap().broadcastMessage(MaplePacketCreator.removeSummon(summon, true), summon.getPosition());
                            getMap().removeMapObject(summon);
                            removeVisibleMapObject(summon);
                            summons.remove(summonId);
                        }
                        try {
                            if (summon.getSkill() == DarkKnight.BEHOLDER) {
                                if (beholderHealingSchedule != null) {
                                    beholderHealingSchedule.cancel(false);
                                    beholderHealingSchedule = null;
                                }
                                if (beholderBuffSchedule != null) {
                                    beholderBuffSchedule.cancel(false);
                                    beholderBuffSchedule = null;
                                }
                            }
                        } catch (NullPointerException npe) {
                            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, npe);
                        }
                    } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    }
                }
            }
            for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                }
            }
        }
    }

    
    public void disableDoor() {
        canDoor = false;
        TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                canDoor = true;
            }
        }, 5000);
    }

    
    public void disbandGuild() {
        if (guildid < 1 || guildrank != 1) {
            return;
        }
        try {
            Server.getInstance().disbandGuild(guildid);
        } catch (Exception e) {
        }
    }

    
    public void dispel() {
        for (MapleBuffStatValueHolder mbsvh : new ArrayList<>(effects.values())) {
            if (mbsvh.effect.isSkill()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    
    public final List<PlayerDiseaseValueHolder> getAllDiseases() {
        final List<PlayerDiseaseValueHolder> ret = new ArrayList<>(5);

        DiseaseValueHolder vh;
        for (Entry<MapleDisease, DiseaseValueHolder> disease : diseases.entrySet()) {
            vh = disease.getValue();
            ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
        }
        return ret;
    }

    
    public final boolean hasDisease(final MapleDisease dis) {
        for (final MapleDisease disease : diseases.keySet()) {
            if (disease == dis) {
                return true;
            }
        }
        return false;
    }

    
    public void giveDebuff(final MapleDisease disease, MobSkill skill) {
        final List<Pair<MapleDisease, Integer>> debuff = Collections.singletonList(new Pair<>(disease, Integer.valueOf(skill.getX())));

        if (!hasDisease(disease) && diseases.size() < 2) {
            if (!(disease == MapleDisease.SEDUCE || disease == MapleDisease.STUN)) {
                if (isActiveBuffedValue(2321005)) {
                    return;
                }
            }
            TimerManager.getInstance().schedule(new Runnable() {
                
                @Override
                public void run() {
                    dispelDebuff(disease);
                }
            }, skill.getDuration());

            diseases.put(disease, new DiseaseValueHolder(System.currentTimeMillis(), skill.getDuration()));
            client.announce(MaplePacketCreator.giveDebuff(debuff, skill));
            map.broadcastMessage(this, MaplePacketCreator.giveForeignDebuff(id, debuff, skill), false);
        }
    }

    
    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            announce(MaplePacketCreator.cancelDebuff(mask));
            map.broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, mask), false);

            diseases.remove(debuff);
        }
    }

    
    public void dispelDebuffs() {
        dispelDebuff(MapleDisease.CURSE);
        dispelDebuff(MapleDisease.DARKNESS);
        dispelDebuff(MapleDisease.POISON);
        dispelDebuff(MapleDisease.SEAL);
        dispelDebuff(MapleDisease.WEAKEN);
    }

    
    public void cancelAllDebuffs() {
        diseases.clear();
    }

    
    public void dispelSkill(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (skillid == 0) {
                if (mbsvh.effect.isSkill() && (mbsvh.effect.getSourceId() % 10000000 == 1004 || dispelSkills(mbsvh.effect.getSourceId()))) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            } else if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    private boolean dispelSkills(int skillid) {
        switch (skillid) {
            case DarkKnight.BEHOLDER:
            case FPArchMage.ELQUINES:
            case ILArchMage.IFRIT:
            case Priest.SUMMON_DRAGON:
            case Bishop.BAHAMUT:
            case Ranger.PUPPET:
            case Ranger.SILVER_HAWK:
            case Sniper.PUPPET:
            case Sniper.GOLDEN_EAGLE:
            case Hermit.SHADOW_PARTNER:
                return true;
            default:
                return false;
        }
    }

    
    public void doHurtHp() {
        if (this.getInventory(MapleInventoryType.EQUIPPED).findById(getMap().getHPDecProtect()) != null) {
            return;
        }
        decreaseHpMp(MapleStat.HP, -getMap().getHPDec());
        hpDecreaseTask = TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                doHurtHp();
            }
        }, 10000);
    }

    
    public void dropMessage(String message) {
        dropMessage(0, message);
    }

    
    public void dropMessage(int type, String message) {
        client.announce(MaplePacketCreator.serverNotice(type, message));
    }

    
    public String emblemCost() {
        return nf.format(MapleGuild.CHANGE_EMBLEM_COST);
    }

    
    @Override
    public List<ScheduledFuture<?>> getTimers() {
        return Collections.unmodifiableList(timers);
    }

    private void enforceMaxHpMp() {
        List<Pair<MapleStat, Integer>> enstats = new ArrayList<>(2);
        if (stats.getStat(MapleStat.MP) > getCurrentMaxMp()) {
            setStat(MapleStat.MP, stats.getStat(MapleStat.MP));
            enstats.add(new Pair<>(MapleStat.MP, stats.getStat(MapleStat.MP)));
        }
        if (stats.getStat(MapleStat.HP) > getCurrentMaxHp()) {
            setStat(MapleStat.HP, stats.getStat(MapleStat.HP));
            enstats.add(new Pair<>(MapleStat.HP, stats.getStat(MapleStat.HP)));
        }
        if (enstats.size() > 0) {
            client.announce(MaplePacketCreator.updatePlayerStats(enstats));
        }
    }

    
    public void enteredScript(String script, int mapid) {
        if (!entered.containsKey(mapid)) {
            entered.put(mapid, script);
        }
    }

    public void equipChanged() {
        getMap().broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        if (!abilities.getAdded())
            abilities.resetStats(this, true);
        recalcLocalStats();
        enforceMaxHpMp();
        if (getMessenger() != null) {
            Server.getInstance().getWorld(world).updateMessenger(getMessenger(), getName(), getWorld(), client.getChannel());
        }
    }

    
    public void cancelExpirationTask() {
        if (expiretask != null) {
            expiretask.cancel(false);
            expiretask = null;
        }
    }

    public void expirationTask() {
        if (expiretask == null) {
            expiretask = TimerManager.getInstance().register(new Runnable() {
                
                @Override
                public void run() {
                    long expiration, currenttime = System.currentTimeMillis();
                    Set<Skill> keys = getSkills().keySet();
                    for (Skill key : keys) {
                        SkillEntry skill = getSkills().get(key);
                        if (skill.expiration != -1 && skill.expiration < currenttime) {
                            changeSkillLevel(key, (byte) -1, 0, -1);
                        }
                    }

                    List<Item> toberemove = new ArrayList<>();
                    for (MapleInventory inv : inventory) {
                        for (Item item : inv.list()) {
                            expiration = item.getExpiration();
                            if (expiration != -1 && (expiration < currenttime) && ((item.getFlag() & ItemConstants.LOCK) == ItemConstants.LOCK)) {
                                byte aids = item.getFlag();
                                aids &= ~(ItemConstants.LOCK);
                                item.setFlag(aids); //Probably need a check, else people can make expiring items into permanent items...
                                item.setExpiration(-1);
                                forceUpdateItem(item);   //TEST :3
                            } else if (expiration != -1 && expiration < currenttime) {
                                client.announce(MaplePacketCreator.itemExpired(item.getItemId()));
                                toberemove.add(item);
                            }
                        }
                        for (Item item : toberemove) {
                            MapleInventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), true);
                        }
                        toberemove.clear();
                    }
                }
            }, 60000);
        }
    }

    
    public void forceUpdateItem(Item item) {
        final List<ModifyInventory> mods = new LinkedList<>();
        mods.add(new ModifyInventory(3, item));
        mods.add(new ModifyInventory(0, item));
        client.announce(MaplePacketCreator.modifyInventory(true, mods));
    }

    
    public void gainGachaExp() {
        int expgain = 0;
        int currentgexp = gachaexp.get();
        int level = stats.getStat(MapleStat.LEVEL);
        if ((currentgexp + exp.get()) >= ExpTable.getExpNeededForLevel(level)) {
            expgain += ExpTable.getExpNeededForLevel(level) - exp.get();
            int nextneed = ExpTable.getExpNeededForLevel(level + 1);
            if ((currentgexp - expgain) >= nextneed) {
                expgain += nextneed;
            }
            this.gachaexp.set(currentgexp - expgain);
        } else {
            expgain = this.gachaexp.getAndSet(0);
        }
        gainExp(expgain, false, false);
        updateSingleStat(MapleStat.GACHAEXP, this.gachaexp.get());
    }

    
    public void gainGachaExp(int gain) {
        updateSingleStat(MapleStat.GACHAEXP, gachaexp.addAndGet(gain));
    }

    
    public void gainExp(int gain, boolean show, boolean inChat) {
        gainExp(gain, show, inChat, true);
    }

    
    public void gainExp(int gain, boolean show, boolean inChat, boolean white) {
        int equip = (gain / 10) * pendantExp;
        int total = gain + equip;
        int level = stats.getStat(MapleStat.LEVEL);

        if (level < getMaxLevel()) {
            if ((long) this.exp.get() + (long) total > (long) Integer.MAX_VALUE) {
                int gainFirst = ExpTable.getExpNeededForLevel(level) - this.exp.get();
                total -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, inChat, white);
            }
            updateSingleStat(MapleStat.EXP, this.exp.addAndGet(total));
            if (show && gain != 0) {
                client.announce(MaplePacketCreator.getShowExpGain(gain, equip, inChat, white));
            }
            if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                levelUp(true);
                int need = ExpTable.getExpNeededForLevel(level);
                if (exp.get() >= need) {
                    setExp(need - 1);
                    updateSingleStat(MapleStat.EXP, need);
                }
            }
        }
    }
    
    
    public void gainExpSupreme(int moblevel, boolean show, boolean boss, boolean white) { //Exp for supremeWorld
        int level = stats.getStat(MapleStat.LEVEL);
        if (boss && level < 201) {
            levelUp(false);
        } else {
            int total = (int) ((Math.ceil(((double) moblevel*10)/level)/100)*ExpTable.getExpNeededForLevel(level));
            
            if (level < getMaxLevel() + 1) {
                if ((long) this.exp.get() + (long) total > (long) Integer.MAX_VALUE) {
                    int gainFirst = ExpTable.getExpNeededForLevel(level) - this.exp.get();
                    total -= gainFirst + 1;
                    this.gainExp(gainFirst + 1, false, false, white);
                }
                
                updateSingleStat(MapleStat.EXP, this.exp.addAndGet(total));
                if (show && total != 0) {
                    client.announce(MaplePacketCreator.getShowExpGain(total, 0, false, white));
                }
                
                if (exp.get() >= ExpTable.getExpNeededForLevel(level)) {
                    levelUp(true);
                    int need = ExpTable.getExpNeededForLevel(level);
                    if (exp.get() >= need) {
                        setExp(need - 1);
                        updateSingleStat(MapleStat.EXP, need);
                    }
                }
            }
        }
    }

    
    public void gainFame(int delta) {
        this.addFame(delta);
        this.updateSingleStat(MapleStat.FAME, this.fame);
    }

    
    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    
    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        if (meso.get() + gain < 0) {
            client.announce(MaplePacketCreator.enableActions());
            return;
        }
        updateSingleStat(MapleStat.MESO, meso.addAndGet(gain), enableActions);
        if (show) {
            client.announce(MaplePacketCreator.getShowMesoGain(gain, inChat));
        }
    }

    
    public void genericGuildMessage(int code) {
        this.client.announce(MaplePacketCreator.genericGuildMessage((byte) code));
    }
    
    public void clearMisc() {
        cancelFishing();
        fishingTask = null;
        shop = null;
        NPCScriptManager.getInstance().dispose(client);
        QuestScriptManager.getInstance().dispose(client);
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        if (skillid == 5221999) {
            this.battleshipHp = (int) length;
            addCooldown(skillid, 0, length, null);
        } else {
            int time = (int) ((length + starttime) - System.currentTimeMillis());
            addCooldown(skillid, System.currentTimeMillis(), time, TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time));
        }
    }

    
    @Override
    public String getGuildCost() {
        return nf.format(MapleGuild.CREATE_GUILD_COST);
    }

    private void guildUpdate() {
        if (this.guildid < 1) {
            return;
        }
        mgc.setLevel(stats.getStat(MapleStat.LEVEL));
        mgc.setJobId(getStat(MapleStat.JOB));
        try {
            Server.getInstance().memberLevelJobUpdate(this.mgc);
            int allianceId = getGuild().getAllianceId();
            if (allianceId > 0) {
                Server.getInstance().allianceMessage(allianceId, MaplePacketCreator.updateAllianceJobLevel(this), getId(), -1);
            }
        } catch (Exception e) {
            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void handleEnergyChargeGain() { // to get here energychargelevel has to be > 0
        Skill energycharge = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.ENERGY_CHARGE) : SkillFactory.getSkill(Marauder.ENERGY_CHARGE);
        MapleStatEffect ceffect;
        ceffect = energycharge.getEffect(getSkillLevel(energycharge));
        TimerManager tMan = TimerManager.getInstance();
        if (energybar < 10000) {
            energybar += 102;
            if (energybar > 10000) {
                energybar = 10000;
            }
            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.ENERGY_CHARGE, energybar));
            setBuffedValue(MapleBuffStat.ENERGY_CHARGE, energybar);
            client.announce(MaplePacketCreator.giveBuff(energybar, 0, stat));
            client.announce(MaplePacketCreator.showOwnBuffEffect(energycharge.getId(), 2));
            getMap().broadcastMessage(this, MaplePacketCreator.showBuffeffect(id, energycharge.getId(), 2));
            getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(energybar, stat));
        }
        if (energybar >= 10000 && energybar < 11000) {
            energybar = 15000;
            final MapleCharacter chr = this;
            tMan.schedule(new Runnable() {
                
                @Override
                public void run() {
                    energybar = 0;
                    List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.ENERGY_CHARGE, energybar));
                    setBuffedValue(MapleBuffStat.ENERGY_CHARGE, energybar);
                    client.announce(MaplePacketCreator.giveBuff(energybar, 0, stat));
                    getMap().broadcastMessage(chr, MaplePacketCreator.giveForeignBuff(energybar, stat));
                }
            }, ceffect.getDuration());
        }
    }

    public void handleOrbconsume() {
        int skillid = isCygnus() ? DawnWarrior.COMBO : Crusader.COMBO;
        Skill combo = SkillFactory.getSkill(skillid);
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, 1));
        setBuffedValue(MapleBuffStat.COMBO, 1);
        client.announce(MaplePacketCreator.giveBuff(skillid, combo.getEffect(getSkillLevel(combo)).getDuration() + (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis())), stat));
        getMap().broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat), false);
    }

    public boolean hasEntered(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEntered(String script, int mapId) {
        if (entered.containsKey(mapId)) {
            if (entered.get(mapId).equals(script)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(to.getId());
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
                ps.setInt(1, getId());
                ps.setInt(2, to.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
        }
    }

    public void increaseGuildCapacity() { //hopefully nothing is null <- lol?
        if (getMeso() < getGuild().getIncreaseGuildCost(getGuild().getCapacity())) {
            dropMessage(1, "You don't have enough mesos.");
            return;
        }
        Server.getInstance().increaseGuildCapacity(guildid);
        gainMeso(-getGuild().getIncreaseGuildCost(getGuild().getCapacity()), true, false, false);
    }

    public void leaveMap() {
        controlled.clear();
        visibleMapObjects.clear();
        if (chair != 0) {
            chair = 0;
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
    }

    @Override
    public void levelUp(boolean takeexp) {
        Skill improvingMaxHP = null;
        Skill improvingMaxMP = null;
        int improvingMaxHPLevel = 0;
        int improvingMaxMPLevel = 0;
        int level = stats.getStat(MapleStat.LEVEL);

        if (isBeginnerJob()) {
            remainingAp = 0;
            if (level < 6) {
                addStat(MapleStat.STR, 5);
            } else {
                addStat(MapleStat.STR, 4);
                addStat(MapleStat.DEX, 1);
            }
        } else {
            remainingAp += 5;
            if (isCygnus() && level < 70) {
                remainingAp++;
            }
        }
        if (getMapleJob() == MapleJob.BEGINNER || getMapleJob() == MapleJob.NOBLESSE || getMapleJob() == MapleJob.LEGEND) {
            addStat(MapleStat.MAXHP, Randomizer.rand(12, 16));
            addStat(MapleStat.MAXMP,Randomizer.rand(10, 12));
        } else if (getMapleJob().isA(MapleJob.WARRIOR) || getMapleJob().isA(MapleJob.DAWNWARRIOR1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(DawnWarrior.MAX_HP_INCREASE) : SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE);
            if (getMapleJob().isA(MapleJob.CRUSADER)) {
                improvingMaxMP = SkillFactory.getSkill(1210000);
            } else if (getMapleJob().isA(MapleJob.DAWNWARRIOR2)) {
                improvingMaxMP = SkillFactory.getSkill(11110000);
            }
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addStat(MapleStat.MAXHP, Randomizer.rand(24, 28));
            addStat(MapleStat.MAXMP,Randomizer.rand(4, 6));
        } else if (getMapleJob().isA(MapleJob.MAGICIAN) || getMapleJob().isA(MapleJob.BLAZEWIZARD1)) {
            improvingMaxMP = isCygnus() ? SkillFactory.getSkill(BlazeWizard.INCREASING_MAX_MP) : SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE);
            improvingMaxMPLevel = getSkillLevel(improvingMaxMP);
            addStat(MapleStat.MAXHP, Randomizer.rand(10, 14));
            addStat(MapleStat.MAXMP,Randomizer.rand(22, 24));
        } else if (getMapleJob().isA(MapleJob.BOWMAN) || getMapleJob().isA(MapleJob.THIEF) || (getMapleJob().getId() > 1299 && getMapleJob().getId() < 1500)) {
            addStat(MapleStat.MAXHP, Randomizer.rand(20, 24));
            addStat(MapleStat.MAXMP,Randomizer.rand(14, 16));
        } else if (getMapleJob().isA(MapleJob.GM)) {
            addStat(MapleStat.MAXHP, 30000);
            addStat(MapleStat.MAXMP, 30000);
        } else if (getMapleJob().isA(MapleJob.PIRATE) || getMapleJob().isA(MapleJob.THUNDERBREAKER1)) {
            improvingMaxHP = isCygnus() ? SkillFactory.getSkill(ThunderBreaker.IMPROVE_MAX_HP) : SkillFactory.getSkill(5100000);
            improvingMaxHPLevel = getSkillLevel(improvingMaxHP);
            addStat(MapleStat.MAXHP, Randomizer.rand(22, 28));
            addStat(MapleStat.MAXMP, Randomizer.rand(18, 23));
        } else if (getMapleJob().isA(MapleJob.ARAN1)) {
            addStat(MapleStat.MAXHP, Randomizer.rand(44, 48));
            int aids = Randomizer.rand(4, 8);
            addStat(MapleStat.MAXMP, (int) (aids + Math.floor(aids * 0.1)));
        }
        if (improvingMaxHPLevel > 0 && (getMapleJob().isA(MapleJob.WARRIOR) || getMapleJob().isA(MapleJob.PIRATE) || getMapleJob().isA(MapleJob.DAWNWARRIOR1))) {
            addStat(MapleStat.MAXHP, improvingMaxHP.getEffect(improvingMaxHPLevel).getX());
        }
        if (improvingMaxMPLevel > 0 && (getMapleJob().isA(MapleJob.MAGICIAN) || getMapleJob().isA(MapleJob.CRUSADER) || getMapleJob().isA(MapleJob.BLAZEWIZARD1))) {
            addStat(MapleStat.MAXMP, improvingMaxMP.getEffect(improvingMaxMPLevel).getX());
        }
        addStat(MapleStat.MAXMP, localint_ / 10);
        if (takeexp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level));
            if (exp.get() < 0) {
                exp.set(0);
            }
        }
        addStat(MapleStat.LEVEL, 1);
        
        if (supremeWorld && (level == 70 || level == 120))
            changeJob(MapleJob.getById(getStat(MapleStat.JOB) + 1));
        
        if (level == 201 && supremeWorld && autoRebirth) {
            setStat(MapleStat.LEVEL, 10);
            rebirths++;
            changeJob(MapleJob.BEGINNER);
        }
        
        if (level >= getMaxLevel()) {
            exp.set(0);
        }
        
        if (level == 200) {
            exp.set(0);
        }
        
        setStat(MapleStat.HP, getStat(MapleStat.MAXHP));
        setStat(MapleStat.MP, getStat(MapleStat.MAXMP));
        recalcLocalStats();
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(10);
        statup.add(new Pair<>(MapleStat.AVAILABLEAP, remainingAp));
        statup.add(new Pair<>(MapleStat.HP, localmaxhp));
        statup.add(new Pair<>(MapleStat.MP, localmaxmp));
        statup.add(new Pair<>(MapleStat.EXP, exp.get()));
        statup.add(new Pair<>(MapleStat.LEVEL, getStat(MapleStat.LEVEL)));
        statup.add(new Pair<>(MapleStat.MAXHP, getStat(MapleStat.MAXHP)));
        statup.add(new Pair<>(MapleStat.MAXMP, getStat(MapleStat.MAXMP)));
        statup.add(new Pair<>(MapleStat.STR, getStat(MapleStat.STR)));
        statup.add(new Pair<>(MapleStat.DEX, getStat(MapleStat.DEX)));
        if (getStat(MapleStat.JOB) % 1000 > 0) {
            remainingSp += 3;
            statup.add(new Pair<>(MapleStat.AVAILABLESP, remainingSp));
        }
        client.announce(MaplePacketCreator.updatePlayerStats(statup));
        getMap().broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 0), false);
        recalcLocalStats();
        setMPC(new MaplePartyCharacter(this));
        silentPartyUpdate();
        if (this.guildid > 0) {
            getGuild().broadcast(MaplePacketCreator.levelUpMessage(2, level, name), this.getId());
        }
        if (ServerConstants.PERFECT_PITCH) {
            //milestones?
            if (MapleInventoryManipulator.checkSpace(client, 4310000, (short) 1, "")) {
                MapleInventoryManipulator.addById(client, 4310000, (short) 1);
            }
        }
        if (level == 200 && !isGM()) {
            final String names = (getMedalText() + name);
            client.getWorldServer().broadcastPacket(MaplePacketCreator.serverNotice(6, String.format(LEVEL_200, names, names)));
        }
        guildUpdate();
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) throws SQLException {
        try {
            MapleCharacter ret = new MapleCharacter();
            ret.client = client;
            ret.id = charid;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading char failed (not found)");
            }
            ret.name = rs.getString("name");
            ret.rebirths = rs.getInt("rebirths");
            ret.fame = rs.getInt("fame");
            ret.exp.set(rs.getInt("exp"));
            ret.gachaexp.set(rs.getInt("gachaexp"));
            int[] loadedStats = {rs.getInt("skincolor"), rs.getInt("face"), rs.getInt("hair"), rs.getInt("level"), rs.getInt("job"), rs.getInt("str"), rs.getInt("dex"), rs.getInt("int"), rs.getInt("luk"), rs.getInt("hp"), rs.getInt("maxhp"), rs.getInt("mp"), rs.getInt("maxmp")};
            ret.stats = new PlayerStats(loadedStats);
            ret.abilities = new Potentials(rs.getInt("potential0"), rs.getInt("potential1"), rs.getInt("potential2"));
            ret.hpMpApUsed = rs.getInt("hpMpUsed");
            ret.hasMerchant = rs.getInt("HasMerchant") == 1;
            ret.remainingSp = rs.getInt("sp");
            ret.remainingAp = rs.getInt("ap");
            ret.meso.set(rs.getInt("meso"));
            ret.merchantmeso = rs.getInt("MerchantMesos");
            ret.gmLevel = rs.getInt("gm");
            ret.gender = rs.getInt("gender");
            ret.finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1;
            ret.vanquisherKills = rs.getInt("vanquisherKills");
            ret.omokwins = rs.getInt("omokwins");
            ret.omoklosses = rs.getInt("omoklosses");
            ret.omokties = rs.getInt("omokties");
            ret.matchcardwins = rs.getInt("matchcardwins");
            ret.matchcardlosses = rs.getInt("matchcardlosses");
            ret.matchcardties = rs.getInt("matchcardties");
            ret.accountid = rs.getInt("accountid");
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getByte("world");
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            int mountexp = rs.getInt("mountexp");
            int mountlevel = rs.getInt("mountlevel");
            int mounttiredness = rs.getInt("mounttiredness");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getInt("guildrank");
            ret.allianceRank = rs.getInt("allianceRank");
            ret.familyId = rs.getInt("familyId");
            ret.bookCover = rs.getInt("monsterbookcover");
            ret.monsterbook = new MonsterBook();
            ret.monsterbook.loadCards(charid);
            ret.achievements = new Achievements();
            ret.achievements.loadAchievements(charid);
            ret.vanquisherStage = rs.getInt("vanquisherStage");
            ret.dojoPoints = rs.getInt("dojoPoints");
            ret.dojoStage = rs.getInt("lastDojoStage");
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            int buddyCapacity = rs.getInt("buddyCapacity");
            ret.buddylist = new BuddyList(buddyCapacity);
            ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getByte("equipslots"));
            ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getByte("useslots"));
            ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getByte("setupslots"));
            ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getByte("etcslots"));
            for (Pair<Item, MapleInventoryType> item : ItemFactory.INVENTORY.loadItems(ret.id, !channelserver)) {
                ret.getInventory(item.getRight()).addFromDB(item.getLeft());
                Item itemz = item.getLeft();
                if (itemz.getPetId() > -1) {
                    MaplePet pet = itemz.getPet();
                    if (pet != null && pet.isSummoned()) {
                        ret.addPet(pet);
                    }
                    continue;
                }
                if (item.getRight().equals(MapleInventoryType.EQUIP) || item.getRight().equals(MapleInventoryType.EQUIPPED)) {
                    Equip equip = (Equip) item.getLeft();
                    if (equip.getRingId() > -1) {
                        MapleRing ring = MapleRing.loadFromDb(equip.getRingId());
                        if (item.getRight().equals(MapleInventoryType.EQUIPPED)) {
                            ring.equip();
                        }
                        if (ring.getItemId() > 1112012) {
                            ret.addFriendshipRing(ring);
                        } else {
                            ret.addCrushRing(ring);
                        }
                    }
                }
            }
            if (channelserver) {
                MapleMapFactory mapFactory = client.getChannelServer().getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) {
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0);
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());
                int partyid = rs.getInt("party");
                MapleParty party = Server.getInstance().getWorld(ret.world).getParty(partyid);
                if (party != null) {
                    ret.mpc = party.getMemberById(ret.id);
                    if (ret.mpc != null) {
                        ret.party = party;
                    }
                }
                int messengerid = rs.getInt("messengerid");
                int position = rs.getInt("messengerposition");
                if (messengerid > 0 && position < 4 && position > -1) {
                    MapleMessenger messenger = Server.getInstance().getWorld(ret.world).getMessenger(messengerid);
                    if (messenger != null) {
                        ret.messenger = messenger;
                        ret.messengerposition = position;
                    }
                }
                ret.loggedIn = true;
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT mapid,vip FROM trocklocations WHERE characterid = ? LIMIT 15");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            byte v = 0;
            byte r = 0;
            while (rs.next()) {
                if (rs.getInt("vip") == 1) {
                    ret.viptrockmaps[v] = rs.getInt("mapid");
                    v++;
                } else {
                    ret.trockmaps[r] = rs.getInt("mapid");
                    r++;
                }
            }
            while (v < 10) {
                ret.viptrockmaps[v] = 999999999;
                v++;
            }
            while (r < 5) {
                ret.trockmaps[r] = 999999999;
                r++;
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE id = ?", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, ret.accountid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.getClient().setAccountName(rs.getString("name"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `area`,`info` FROM area_info WHERE charid = ?");
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.area_info.put(rs.getShort("area"), rs.getString("info"));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT `name`,`info` FROM eventstats WHERE characterid = ?");
            ps.setInt(1, ret.id);
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                if (rs.getString("name").equals("rescueGaga")) {
                    ret.events.put(name, new RescueGaga(rs.getInt("info")));
                }
                //ret.events = new MapleEvents(new RescueGaga(rs.getInt("rescuegaga")), new ArtifactHunt(rs.getInt("artifacthunt")));
            }
            rs.close();
            ps.close();
            ret.cashshop = new CashShop(ret.accountid, ret.id, ret.getJobType());
            ret.autoban = new AutobanManager(ret);
            ret.marriageRing = null; //for now
            ps = con.prepareStatement("SELECT name, level FROM characters WHERE accountid = ? AND id != ? ORDER BY level DESC limit 1");
            ps.setInt(1, ret.accountid);
            ps.setInt(2, charid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ret.linkedName = rs.getString("name");
                ret.linkedLevel = rs.getInt("level");
            }
            rs.close();
            ps.close();
            if (channelserver) {
                ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                PreparedStatement psf;
                try (PreparedStatement pse = con.prepareStatement("SELECT * FROM questprogress WHERE queststatusid = ?")) {
                    psf = con.prepareStatement("SELECT mapid FROM medalmaps WHERE queststatusid = ?");
                    while (rs.next()) {
                        MapleQuest q = MapleQuest.getInstance(rs.getShort("quest"));
                        MapleQuestStatus status = new MapleQuestStatus(q, MapleQuestStatus.Status.getById(rs.getInt("status")));
                        long cTime = rs.getLong("time");
                        if (cTime > -1) {
                            status.setCompletionTime(cTime * 1000);
                        }
                        status.setForfeited(rs.getInt("forfeited"));
                        ret.quests.put(q, status);
                        pse.setInt(1, rs.getInt("queststatusid"));
                        try (ResultSet rsProgress = pse.executeQuery()) {
                            while (rsProgress.next()) {
                                status.setProgress(rsProgress.getInt("progressid"), rsProgress.getString("progress"));
                            }
                        }
                        psf.setInt(1, rs.getInt("queststatusid"));
                        try (ResultSet medalmaps = psf.executeQuery()) {
                            while (medalmaps.next()) {
                                status.addMedalMap(medalmaps.getInt("mapid"));
                            }
                        }
                    }
                    rs.close();
                    ps.close();
                }
                psf.close();
                ps = con.prepareStatement("SELECT skillid,skilllevel,masterlevel,expiration FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getByte("skilllevel"), rs.getInt("masterlevel"), rs.getLong("expiration")));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM cooldowns WHERE charid = ?");
                ps.setInt(1, ret.getId());
                rs = ps.executeQuery();
                while (rs.next()) {
                    final int skillid = rs.getInt("SkillID");
                    final long length = rs.getLong("length"), startTime = rs.getLong("StartTime");
                    if (skillid != 5221999 && (length + startTime < System.currentTimeMillis())) {
                        continue;
                    }
                    ret.giveCoolDowns(skillid, startTime, length);
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("DELETE FROM cooldowns WHERE charid = ?");
                ps.setInt(1, ret.getId());
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int key = rs.getInt("key");
                    int type = rs.getInt("type");
                    int action = rs.getInt("action");
                    ret.keymap.put(key, new MapleKeyBinding(type, action));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `locationtype`,`map`,`portal` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[SavedLocationType.valueOf(rs.getString("locationtype")).ordinal()] = new SavedLocation(rs.getInt("map"), rs.getInt("portal"));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(rs.getInt("characterid_to"));
                }
                rs.close();
                ps.close();
                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid, ret.world);
                ret.recalcLocalStats();
                //ret.resetBattleshipHp();
                ret.silentEnforceMaxHpMp();
                
                if (ret.world == ServerConstants.WORLDS)
                    ret.supremeWorld = true;
            }
            int mountid = ret.getJobType() * 10000000 + 1004;
            if (ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null) {
                ret.maplemount = new MapleMount(ret, ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId(), mountid);
            } else {
                ret.maplemount = new MapleMount(ret, 0, mountid);
            }
            ret.maplemount.setExp(mountexp);
            ret.maplemount.setLevel(mountlevel);
            ret.maplemount.setTiredness(mounttiredness);
            ret.maplemount.setActive(false);
            return ret;
        } catch (SQLException | RuntimeException e) {
            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }

    public void message(String m) {
        dropMessage(5, m);
    }
    
    public void yellowMessage(String m) {
        announce(MaplePacketCreator.sendYellowTip(m));
    }
    
    public void mobKilled(int id) {
        if (id == 1110100)
            mobKilled(9101000);
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == MapleQuestStatus.Status.COMPLETED || q.getQuest().canComplete(this, null)) {
                continue;
            }
            String progress = q.getProgress(id);
            if (!progress.isEmpty() && Integer.parseInt(progress) >= q.getQuest().getMobAmountNeeded(id)) {
                continue;
            }
            if (q.progress(id)) {
                client.announce(MaplePacketCreator.updateQuest(q.getQuest().getId(), q.getQuestData()));
            }
        }
    }
    
    public void mount(int id, int skillid) {
        maplemount = new MapleMount(this, id, skillid);
    }
    
    public void playerNPC(MapleCharacter v, int scriptId) {
        int npcId;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM playernpcs WHERE ScriptId = ?");
            ps.setInt(1, scriptId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps = con.prepareStatement("INSERT INTO playernpcs (name, hair, face, skin, x, cy, map, ScriptId, Foothold, rx0, rx1) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, v.getName());
                ps.setInt(2, v.getHair());
                ps.setInt(3, v.getFace());
                ps.setInt(4, v.getMapleSkinColor().getId());
                ps.setInt(5, getPosition().x);
                ps.setInt(6, getPosition().y);
                ps.setInt(7, getMapId());
                ps.setInt(8, scriptId);
                ps.setInt(9, getMap().getFootholds().findBelow(getPosition()).getId());
                ps.setInt(10, getPosition().x + 50);
                ps.setInt(11, getPosition().x - 50);
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                rs.next();
                npcId = rs.getInt(1);
                ps.close();
                ps = con.prepareStatement("INSERT INTO playernpcs_equip (NpcId, equipid, equippos) VALUES (?, ?, ?)");
                ps.setInt(1, npcId);
                for (Item equip : getInventory(MapleInventoryType.EQUIPPED)) {
                    int position = Math.abs(equip.getPosition());
                    if ((position < 12 && position > 0) || (position > 100 && position < 112)) {
                        ps.setInt(2, equip.getItemId());
                        ps.setInt(3, equip.getPosition());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
                ps.close();
                rs.close();
                ps = con.prepareStatement("SELECT * FROM playernpcs WHERE ScriptId = ?");
                ps.setInt(1, scriptId);
                rs = ps.executeQuery();
                rs.next();
                PlayerNPCs pn = new PlayerNPCs(rs);
                for (Channel channel : Server.getInstance().getChannelsFromWorld(world)) {
                    MapleMap m = channel.getMapFactory().getMap(getMapId());
                    m.broadcastMessage(MaplePacketCreator.spawnPlayerNPC(pn));
                    m.broadcastMessage(MaplePacketCreator.getPlayerNPC(pn));
                    m.addMapObject(pn);
                }
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    
    @Override
    protected void playerDead() {
        cancelAllBuffs(false);
        dispelDebuffs();
        clearMisc();
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        int[] charmID = {5130000, 4031283, 4140903};
        int possesed = 0;
        int i;
        for (i = 0; i < charmID.length; i++) {
            int quantity = getItemQuantity(charmID[i], false);
            if (possesed == 0 && quantity > 0) {
                possesed = quantity;
                break;
            }
        }
        if (possesed > 0) {
            message("You have used a safety charm, so your EXP points have not been decreased.");
            MapleInventoryManipulator.removeById(client, MapleItemInformationProvider.getInstance().getInventoryType(charmID[i]), charmID[i], 1, true, false);
        } else if (mapid > 925020000 && mapid < 925030000) {
            this.dojoStage = 0;
        } else if (mapid > 980000100 && mapid < 980000700) {
            getMap().broadcastMessage(this, MaplePacketCreator.CPQDied(this));
        } else if (getMapleJob() != MapleJob.BEGINNER) { //Hmm...
            int XPdummy = ExpTable.getExpNeededForLevel(getStat(MapleStat.LEVEL));
            if (getMap().isTown()) {
                XPdummy /= 100;
            }
            if (XPdummy == ExpTable.getExpNeededForLevel(getStat(MapleStat.LEVEL))) {
                if (getStat(MapleStat.LUK) <= 100 && getStat(MapleStat.LUK) > 8) {
                    XPdummy *= (200 - getStat(MapleStat.LUK)) / 2000;
                } else if (getStat(MapleStat.LUK) < 8) {
                    XPdummy /= 10;
                } else {
                    XPdummy /= 20;
                }
            }
            if (getExp() > XPdummy) {
                gainExp(-XPdummy, false, false);
            } else {
                gainExp(-getExp(), false, false);
            }
        }
        if (getBuffedValue(MapleBuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        }

        if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        }

        if (getChair() == -1) {
            setChair(0);
            client.announce(MaplePacketCreator.cancelChair(-1));
            getMap().broadcastMessage(this, MaplePacketCreator.showChair(getId(), 0), false);
        }
        client.announce(MaplePacketCreator.enableActions());
    }

    
    protected void prepareDragonBlood(final MapleStatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {
            
            @Override
            public void run() {
                decreaseHpMp(MapleStat.HP, -bloodEffect.getX());
                client.announce(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5), false);
                checkBerserk();
            }
        }, 4000, 4000);
    }

    
    @Override
    protected void recalcLocalStats() {
        int oldmaxhp = localmaxhp;
        localmaxhp = getStat(MapleStat.MAXHP);
        localmaxmp = getStat(MapleStat.MAXMP);
        localdex = getStat(MapleStat.DEX);
        localint_ = getStat(MapleStat.INT);
        localstr = getStat(MapleStat.STR);
        localluk = getStat(MapleStat.LUK);
        int speed = 100, jump = 100;
        magic = localint_;
        watk = 0;
        for (Item item : getInventory(MapleInventoryType.EQUIPPED)) {
            Equip equip = (Equip) item;
            localmaxhp += equip.getHp();
            localmaxmp += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk() + equip.getInt();
            watk += equip.getWatk();
            speed += equip.getSpeed();
            jump += equip.getJump();
        }
        magic = Math.min(magic, 2000);
        Integer hbhp = getBuffedValue(MapleBuffStat.HYPERBODYHP);
        if (hbhp != null) {
            localmaxhp += (hbhp.doubleValue() / 100) * localmaxhp;
        }
        Integer hbmp = getBuffedValue(MapleBuffStat.HYPERBODYMP);
        if (hbmp != null) {
            localmaxmp += (hbmp.doubleValue() / 100) * localmaxmp;
        }
        localmaxhp = Math.min(30000, localmaxhp);
        localmaxmp = Math.min(30000, localmaxmp);
        Integer watkbuff = getBuffedValue(MapleBuffStat.WATK);
        if (watkbuff != null) {
            watk += watkbuff;
        }
        if (getMapleJob().isA(MapleJob.BOWMAN)) {
            Skill expert = null;
            if (getMapleJob().isA(MapleJob.MARKSMAN)) {
                expert = SkillFactory.getSkill(3220004);
            } else if (getMapleJob().isA(MapleJob.BOWMASTER)) {
                expert = SkillFactory.getSkill(3120005);
            }
            if (expert != null) {
                int boostLevel = getSkillLevel(expert);
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel).getX();
                }
            }
        }
        Integer matkbuff = getBuffedValue(MapleBuffStat.MATK);
        if (matkbuff != null) {
            magic += matkbuff;
        }
        Integer speedbuff = getBuffedValue(MapleBuffStat.SPEED);
        if (speedbuff != null) {
            speed += speedbuff;
        }
        Integer jumpbuff = getBuffedValue(MapleBuffStat.JUMP);
        if (jumpbuff != null) {
            jump += jumpbuff;
        }
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            updatePartyMemberHP();
        }
    }
    
    
    public void receivePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = Server.getInstance().getWorld(world).getChannel(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        client.announce(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getStat(MapleStat.HP), other.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    
    @Override
    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule) {
        if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isBeholder()) {
            final int beholder = DarkKnight.BEHOLDER;
            if (beholderHealingSchedule != null) {
                beholderHealingSchedule.cancel(false);
            }
            if (beholderBuffSchedule != null) {
                beholderBuffSchedule.cancel(false);
            }
            Skill bHealing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER);
            int bHealingLvl = getSkillLevel(bHealing);
            if (bHealingLvl > 0) {
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                int healInterval = healEffect.getX() * 1000;
                beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {
                    
                    @Override
                    public void run() {
                        addStat(MapleStat.HP, healEffect.getHp());
                        client.announce(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), beholder, 5), true);
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showOwnBuffEffect(beholder, 2), false);
                    }
                }, healInterval, healInterval);
            }
            Skill bBuff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER);
            if (getSkillLevel(bBuff) > 0) {
                final MapleStatEffect buffEffect = bBuff.getEffect(getSkillLevel(bBuff));
                int buffInterval = buffEffect.getX() * 1000;
                beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {
                    
                    @Override
                    public void run() {
                        buffEffect.applyTo(MapleCharacter.this);
                        client.announce(MaplePacketCreator.showOwnBuffEffect(beholder, 2));
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.summonSkill(getId(), beholder, (int) (Math.random() * 3) + 6), true);
                        getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showBuffeffect(getId(), beholder, 2), false);
                    }
                }, buffInterval, buffInterval);
            }
        } else if (effect.isRecovery()) {
            final byte heal = (byte) effect.getX();
            recoveryTask = TimerManager.getInstance().register(new Runnable() {
                
                @Override
                public void run() {
                    addStat(MapleStat.HP, heal);
                    client.announce(MaplePacketCreator.showOwnRecovery(heal));
                    getMap().broadcastMessage(MapleCharacter.this, MaplePacketCreator.showRecovery(id, heal), false);
                }
            }, 5000, 5000);
        }
        for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
            effects.put(statup.getLeft(), new MapleBuffStatValueHolder(effect, starttime, schedule, statup.getRight()));
        }
        recalcLocalStats();
    }

    
    public void removeAllCooldownsExcept(int id) {
        for (MapleCoolDownValueHolder mcvh : coolDowns.values()) {
            if (mcvh.skillId != id) {
                coolDowns.remove(mcvh.skillId);
            }
        }
    }

    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }

    
    public void removeCooldown(int skillId) {
        if (this.coolDowns.containsKey(skillId)) {
            this.coolDowns.remove(skillId);
        }
    }

    
    public void removePet(MaplePet pet, boolean shift_left) {
        int slot = -1;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    pets[i] = null;
                    slot = i;
                    break;
                }
            }
        }
        if (shift_left) {
            if (slot > -1) {
                for (int i = slot; i < 3; i++) {
                    if (i != 2) {
                        pets[i] = pets[i + 1];
                    } else {
                        pets[i] = null;
                    }
                }
            }
        }
    }

    
    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public void resetStats() {
        List<Pair<MapleStat, Integer>> statup = new ArrayList<>(5);
        int tap = 0, tsp = 1;
        int tstr = 4, tdex = 4, tint = 4, tluk = 4;
        int levelap = (isCygnus() ? 6 : 5);
        switch (getStat(MapleStat.JOB)) {
            case 100:
            case 1100:
            case 2100://?
                tstr = 35;
                tap = ((getStat(MapleStat.LEVEL) - 10) * levelap) + 14;
                tsp += ((getStat(MapleStat.LEVEL) - 10) * 3);
                break;
            case 200:
            case 1200:
                tint = 20;
                tap = ((getStat(MapleStat.LEVEL) - 8) * levelap) + 29;
                tsp += ((getStat(MapleStat.LEVEL) - 8) * 3);
                break;
            case 300:
            case 1300:
            case 400:
            case 1400:
                tdex = 25;
                tap = ((getStat(MapleStat.LEVEL) - 10) * levelap) + 24;
                tsp += ((getStat(MapleStat.LEVEL) - 10) * 3);
                break;
            case 500:
            case 1500:
                tdex = 20;
                tap = ((getStat(MapleStat.LEVEL) - 10) * levelap) + 29;
                tsp += ((getStat(MapleStat.LEVEL) - 10) * 3);
                break;
        }
        this.remainingAp = tap;
        this.remainingSp = tsp;
        setStat(MapleStat.DEX, tdex);
        setStat(MapleStat.INT, tint);
        setStat(MapleStat.STR, tstr);
        setStat(MapleStat.LUK, tluk);
        statup.add(new Pair<>(MapleStat.AVAILABLEAP, tap));
        statup.add(new Pair<>(MapleStat.AVAILABLESP, tsp));
        statup.add(new Pair<>(MapleStat.STR, tstr));
        statup.add(new Pair<>(MapleStat.DEX, tdex));
        statup.add(new Pair<>(MapleStat.INT, tint));
        statup.add(new Pair<>(MapleStat.LUK, tluk));
        announce(MaplePacketCreator.updatePlayerStats(statup));
    }

    public void resetBattleshipHp() {
        this.battleshipHp = 4000 * getSkillLevel(SkillFactory.getSkill(Corsair.BATTLE_SHIP)) + ((getStat(MapleStat.LEVEL) - 120) * 2000);
    }

    public void resetEnteredScript() {
        if (entered.containsKey(map.getId())) {
            entered.remove(map.getId());
        }
    }

    public void resetEnteredScript(int mapId) {
        if (entered.containsKey(mapId)) {
            entered.remove(mapId);
        }
    }

    public void resetEnteredScript(String script) {
        for (int mapId : entered.keySet()) {
            if (entered.get(mapId).equals(script)) {
                entered.remove(mapId);
            }
        }
    }

    public void saveCooldowns() {
        if (getAllCooldowns().size() > 0) {
            try {
                Connection con = DatabaseConnection.getConnection();
                deleteWhereCharacterId(con, "DELETE FROM cooldowns WHERE charid = ?");
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, getId());
                    for (PlayerCoolDownValueHolder cooling : getAllCooldowns()) {
                        ps.setInt(2, cooling.skillId);
                        ps.setLong(3, cooling.startTime);
                        ps.setLong(4, cooling.length);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException se) {
            }
        }
    }

    public void saveGuildStatus() {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, allianceRank = ? WHERE id = ?")) {
                ps.setInt(1, guildid);
                ps.setInt(2, guildrank);
                ps.setInt(3, allianceRank);
                ps.setInt(4, id);
                ps.execute();
            }
        } catch (SQLException se) {
        }
    }

    public boolean insertNewChar() {
        final Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;

        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            ps = con.prepareStatement("INSERT INTO characters (str, dex, luk, `int`, gm, skincolor, gender, job, hair, face, map, meso, spawnpoint, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, 12);
            ps.setInt(2, 5);
            ps.setInt(3, 4);
            ps.setInt(4, 4);
            ps.setInt(5, gmLevel);
            ps.setInt(6, getStat(MapleStat.SKIN));
            ps.setInt(7, gender);
            ps.setInt(8, getStat(MapleStat.JOB));
            ps.setInt(9, getStat(MapleStat.HAIR));
            ps.setInt(10, getStat(MapleStat.FACE));
            ps.setInt(11, mapid);
            ps.setInt(12, Math.abs(meso.get()));
            ps.setInt(13, 0);
            ps.setInt(14, accountid);
            ps.setString(15, name);
            ps.setInt(16, world);

            int updateRows = ps.executeUpdate();
            if (updateRows < 1) {
                ps.close();
                FilePrinter.printError(FilePrinter.INSERT_CHAR, "Error trying to insert " + name);
                return false;
            }
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                this.id = rs.getInt(1);
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                FilePrinter.printError(FilePrinter.INSERT_CHAR, "Inserting char failed " + name);
                return false;
                //throw new RuntimeException("Inserting char failed.");
            }

            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (int i = 0; i < DEFAULT_KEY.length; i++) {
                ps.setInt(2, DEFAULT_KEY[i]);
                ps.setInt(3, DEFAULT_TYPE[i]);
                ps.setInt(4, DEFAULT_ACTION[i]);
                ps.execute();
            }
            ps.close();

            final List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

            for (MapleInventory iv : inventory) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
            ItemFactory.INVENTORY.saveItems(itemsWithType, id);
            con.commit();
            return true;
        } catch (Throwable t) {
            FilePrinter.printError(FilePrinter.INSERT_CHAR, t, "Error creating " + name + " Level: " + getStat(MapleStat.LEVEL) + " Job: " + getStat(MapleStat.JOB));
            try {
                con.rollback();
            } catch (SQLException se) {
                FilePrinter.printError(FilePrinter.INSERT_CHAR, se, "Error trying to rollback " + name);
            }
            return false;
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
            }
        }
    }

    
    @Override
    public void saveToDB() {
        Connection con = DatabaseConnection.getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            PreparedStatement ps;
            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, gachaexp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpMpUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, mountlevel = ?, mountexp = ?, mounttiredness= ?, equipslots = ?, useslots = ?, setupslots = ?, etcslots = ?,  monsterbookcover = ?, vanquisherStage = ?, dojoPoints = ?, lastDojoStage = ?, finishedDojoTutorial = ?, vanquisherKills = ?, matchcardwins = ?, matchcardlosses = ?, matchcardties = ?, omokwins = ?, omoklosses = ?, omokties = ?, potential0 = ?, potential1 = ?, potential2 = ?, rebirths = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS);
            if (gmLevel < 1 && getStat(MapleStat.LEVEL) > 199) {
                ps.setInt(1, isCygnus() ? 120 : 200);
            } else {
                ps.setInt(1, getStat(MapleStat.LEVEL));
            }
            abilities.resetStats(this, false);
            ps.setInt(2, fame);
            ps.setInt(3, getStat(MapleStat.STR));
            ps.setInt(4, getStat(MapleStat.DEX));
            ps.setInt(5, getStat(MapleStat.LUK));
            ps.setInt(6, getStat(MapleStat.INT));
            ps.setInt(7, Math.abs(exp.get()));
            ps.setInt(8, Math.abs(gachaexp.get()));
            ps.setInt(9, getStat(MapleStat.HP));
            ps.setInt(10, getStat(MapleStat.MP));
            ps.setInt(11, getStat(MapleStat.MAXHP));
            ps.setInt(12, getStat(MapleStat.MAXMP));
            ps.setInt(13, remainingSp);
            ps.setInt(14, remainingAp);
            ps.setInt(15, gmLevel);
            ps.setInt(16, getStat(MapleStat.SKIN));
            ps.setInt(17, gender);
            ps.setInt(18, getStat(MapleStat.JOB));
            ps.setInt(19, getStat(MapleStat.HAIR));
            ps.setInt(20, getStat(MapleStat.FACE));
            if (map == null || (cashshop != null && cashshop.isOpened())) {
                ps.setInt(21, mapid);
            } else {
                if (map.getForcedReturnId() != 999999999) {
                    ps.setInt(21, map.getForcedReturnId());
                } else {
                    ps.setInt(21, getStat(MapleStat.HP) < 1 ? map.getReturnMapId() : map.getId());
                }
            }
            ps.setInt(22, meso.get());
            ps.setInt(23, hpMpApUsed);
            if (map == null || map.getId() == 610020000 || map.getId() == 610020001) {
                ps.setInt(24, 0);
            } else {
                MaplePortal closest = map.findClosestSpawnpoint(getPosition());
                if (closest != null) {
                    ps.setInt(24, closest.getId());
                } else {
                    ps.setInt(24, 0);
                }
            }
            if (party != null) {
                ps.setInt(25, party.getId());
            } else {
                ps.setInt(25, -1);
            }
            ps.setInt(26, buddylist.getCapacity());
            if (messenger != null) {
                ps.setInt(27, messenger.getId());
                ps.setInt(28, messengerposition);
            } else {
                ps.setInt(27, 0);
                ps.setInt(28, 4);
            }
            if (maplemount != null) {
                ps.setInt(29, maplemount.getLevel());
                ps.setInt(30, maplemount.getExp());
                ps.setInt(31, maplemount.getTiredness());
            } else {
                ps.setInt(29, 1);
                ps.setInt(30, 0);
                ps.setInt(31, 0);
            }
            for (int i = 1; i < 5; i++) {
                ps.setInt(i + 31, getSlots(i));
            }

            monsterbook.saveCards(getId());
            achievements.saveAchievements(getId());

            ps.setInt(36, bookCover);
            ps.setInt(37, vanquisherStage);
            ps.setInt(38, dojoPoints);
            ps.setInt(39, dojoStage);
            ps.setInt(40, finishedDojoTutorial ? 1 : 0);
            ps.setInt(41, vanquisherKills);
            ps.setInt(42, matchcardwins);
            ps.setInt(43, matchcardlosses);
            ps.setInt(44, matchcardties);
            ps.setInt(45, omokwins);
            ps.setInt(46, omoklosses);
            ps.setInt(47, omokties);
            ps.setInt(48, abilities.getRank()*1000 + abilities.getLine(0).getSuperior().ordinal()*100 + abilities.getLine(0).getEffect().ordinal());
            ps.setInt(49, abilities.getLine(1).getSuperior().ordinal()*100 + abilities.getLine(1).getEffect().ordinal());
            if (abilities.getLines() > 2)
                ps.setInt(50, abilities.getLine(1).getSuperior().ordinal()*100 + abilities.getLine(2).getEffect().ordinal());
            else
                ps.setInt(50, 0);
            ps.setInt(51, rebirths);
            ps.setInt(52, id);

            int updateRows = ps.executeUpdate();
            if (updateRows < 1) {
                throw new RuntimeException("Character not in database (" + id + ")");
            }
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    pets[i].saveToDb();
                }
            }
            deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Integer, MapleKeyBinding> keybinding : keymap.entrySet()) {
                ps.setInt(2, keybinding.getKey());
                ps.setInt(3, keybinding.getValue().getType());
                ps.setInt(4, keybinding.getValue().getAction());
                ps.addBatch();
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, getId());
            for (int i = 0; i < 5; i++) {
                SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    ps.setInt(2, macro.getSkill1());
                    ps.setInt(3, macro.getSkill2());
                    ps.setInt(4, macro.getSkill3());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getShout());
                    ps.setInt(7, i);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

            for (MapleInventory iv : inventory) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }

            ItemFactory.INVENTORY.saveItems(itemsWithType, id);
            deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                ps.setInt(2, skill.getKey().getId());
                ps.setInt(3, skill.getValue().skillevel);
                ps.setInt(4, skill.getValue().masterlevel);
                ps.setLong(5, skill.getValue().expiration);
                ps.addBatch();
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`, `portal`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != null) {
                    ps.setString(2, savedLocationType.name());
                    ps.setInt(3, savedLocations[savedLocationType.ordinal()].getMapId());
                    ps.setInt(4, savedLocations[savedLocationType.ordinal()].getPortal());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 0)");
            for (int i = 0; i < getTrockSize(); i++) {
                if (trockmaps[i] != 999999999) {
                    ps.setInt(1, getId());
                    ps.setInt(2, trockmaps[i]);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 1)");
            for (int i = 0; i < getVipTrockSize(); i++) {
                if (viptrockmaps[i] != 999999999) {
                    ps.setInt(1, getId());
                    ps.setInt(2, viptrockmaps[i]);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
            ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`, `group`) VALUES (?, ?, 0, ?)");
            ps.setInt(1, id);
            for (BuddylistEntry entry : buddylist.getBuddies()) {
                if (entry.isVisible()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setString(3, entry.getGroup());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM area_info WHERE charid = ?");
            ps = con.prepareStatement("INSERT INTO area_info (id, charid, area, info) VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (Entry<Short, String> area : area_info.entrySet()) {
                ps.setInt(2, area.getKey());
                ps.setString(3, area.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            deleteWhereCharacterId(con, "DELETE FROM eventstats WHERE characterid = ?");
            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`) VALUES (DEFAULT, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement psf;
            try (PreparedStatement pse = con.prepareStatement("INSERT INTO questprogress VALUES (DEFAULT, ?, ?, ?)")) {
                psf = con.prepareStatement("INSERT INTO medalmaps VALUES (DEFAULT, ?, ?)");
                ps.setInt(1, id);
                for (MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus().getId());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        for (int mob : q.getProgress().keySet()) {
                            pse.setInt(1, rs.getInt(1));
                            pse.setInt(2, mob);
                            pse.setString(3, q.getProgress(mob));
                            pse.addBatch();
                        }
                        for (int i = 0; i < q.getMedalMaps().size(); i++) {
                            psf.setInt(1, rs.getInt(1));
                            psf.setInt(2, q.getMedalMaps().get(i));
                            psf.addBatch();
                        }
                        pse.executeBatch();
                        psf.executeBatch();
                    }
                }
            }
            psf.close();
            ps = con.prepareStatement("UPDATE accounts SET gm = ? WHERE id = ?");
            ps.setInt(1, gmLevel);
            ps.setInt(2, client.getAccID());
            ps.executeUpdate();
            if (cashshop != null) {
                cashshop.save();
            }
            if (storage != null) {
                storage.saveToDB();
            }
            ps.close();
            con.commit();
        } catch (SQLException | RuntimeException t) {
            FilePrinter.printError(FilePrinter.SAVE_CHAR, t, "Error saving " + name + " Level: " + getStat(MapleStat.LEVEL) + " Job: " + getStat(MapleStat.JOB));
            try {
                con.rollback();
            } catch (SQLException se) {
                FilePrinter.printError(FilePrinter.SAVE_CHAR, se, "Error trying to rollback " + name);
            }
        } finally {
            try {
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (Exception e) {
            }
        }
        abilities.resetStats(this, true);
    }

    public void sendPolice(int greason, String reason, int duration) {
        announce(MaplePacketCreator.sendPolice(String.format("You have been blocked by #bGMPolice for the %s reason.#k", "HACK")));
        this.isbanned = true;
        TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                client.disconnect(false, false); //FAGGOTS
            }
        }, duration);
    }

    public void sendPolice(String text) {
        announce(MaplePacketCreator.sendPolice(text));
        this.isbanned = true;
        TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                client.disconnect(false, false); //FAGGOTS
            }
        }, 6000);
    }

    public void sendKeymap() {
        client.announce(MaplePacketCreator.getKeymap(keymap));
    }

    public void sendMacros() {
        boolean macros = false;
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                macros = true;
            }
        }
        if (macros) {
            client.announce(MaplePacketCreator.getMacros(skillMacros));
        }
    }

    public void sendNote(String to, String msg, byte fame) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `fame`) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, to);
            ps.setString(2, this.getName());
            ps.setString(3, msg);
            ps.setLong(4, System.currentTimeMillis());
            ps.setByte(5, fame);
            ps.executeUpdate();
        }
    }

    public void saveSetHasMerchant(boolean set) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET HasMerchant = ? WHERE id = ?")) {
                ps.setInt(1, set ? 1 : 0);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            Logger.getLogger(MapleCharacter.class.getName()).log(Level.SEVERE, null, e);
        }
        hasMerchant = set;
    }

    public void saveAddMerchantMeso(int add) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, merchantmeso + add);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            return;
        }
        merchantmeso += add;
    }

    public void saveMerchantMeso(int set) {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET MerchantMesos = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, set);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            return;
        }
        merchantmeso = set;
    }

    public boolean gainSlots(int type, int slots) {
        return gainSlots(type, slots, true);
    }

    public boolean gainSlots(int type, int slots, boolean update) {
        slots += inventory[type].getSlotLimit();
        if (slots <= 96) {
            inventory[type].setSlotLimit(slots);

            saveToDB();
            if (update) {
                client.announce(MaplePacketCreator.updateInventorySlotLimit(type, slots));
            }

            return true;
        }

        return false;
    }
    
    public void shiftPetsRight() {
        if (pets[2] == null) {
            pets[2] = pets[1];
            pets[1] = pets[0];
            pets[0] = null;
        }
    }

    public void showDojoClock() {
        int stage = (map.getId() / 100) % 100;
        long time;
        if (stage % 6 == 1) {
            time = (stage > 36 ? 15 : stage / 6 + 5) * 60;
        } else {
            time = (dojoFinish - System.currentTimeMillis()) / 1000;
        }
        if (stage % 6 > 0) {
            client.announce(MaplePacketCreator.getClock((int) time));
        }
        boolean rightmap = true;
        int clockid = (dojoMap.getId() / 100) % 100;
        if (map.getId() > clockid / 6 * 6 + 6 || map.getId() < clockid / 6 * 6) {
            rightmap = false;
        }
        final boolean rightMap = rightmap; // lol
        TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                if (rightMap) {
                    client.getPlayer().changeMap(client.getChannelServer().getMapFactory().getMap(925020000));
                }
            }
        }, time * 1000 + 3000); // let the TIMES UP display for 3 seconds, then warp
    }

    public void showNote() {
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT * FROM notes WHERE `to`=? AND `deleted` = 0", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                ps.setString(1, this.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    int count = rs.getRow();
                    rs.first();
                    client.announce(MaplePacketCreator.showNotes(rs, count));
                }
            }
        } catch (SQLException e) {
        }
    }

    private void silentEnforceMaxHpMp() {
        setStat(MapleStat.HP, getStat(MapleStat.HP));
        setStat(MapleStat.MP, getStat(MapleStat.MP));
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime);
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            Server.getInstance().getWorld(world).updateParty(party.getId(), PartyOperation.SILENT_UPDATE, getMPC());
        }
    }

    public void startFullnessSchedule(final int decrease, final MaplePet pet, int petSlot) {
        ScheduledFuture<?> schedule;
        schedule = TimerManager.getInstance().register(new Runnable() {
            
            @Override
            public void run() {
                int newFullness = pet.getFullness() - decrease;
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    pet.saveToDb();
                    unequipPet(pet, true);
                } else {
                    pet.setFullness(newFullness);
                    pet.saveToDb();
                    Item petz = getInventory(MapleInventoryType.CASH).getItem(pet.getPosition());
                    forceUpdateItem(petz);
                }
            }
        }, 180000, 18000);
        fullnessSchedule[petSlot] = schedule;

    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, int duration) {
        final MapleMapEffect mapEffect = new MapleMapEffect(msg, itemId);
        getClient().announce(mapEffect.makeStartData());
        TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                getClient().announce(mapEffect.makeDestroyData());
            }
        }, duration);
    }

    public void stopControllingMonster(MapleMonster monster) {
        controlled.remove(monster);
    }

    public void unequipAllPets() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                unequipPet(pets[i], true);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shift_left) {
        unequipPet(pet, shift_left, false);
    }

    public void unequipPet(MaplePet pet, boolean shift_left, boolean hunger) {
        if (this.getPet(this.getPetIndex(pet)) != null) {
            this.getPet(this.getPetIndex(pet)).setSummoned(false);
            this.getPet(this.getPetIndex(pet)).saveToDb();
        }
        cancelFullnessSchedule(getPetIndex(pet));
        getMap().broadcastMessage(this, MaplePacketCreator.showPet(this, pet, true, hunger), true);
        client.announce(MaplePacketCreator.petStatUpdate(this));
        client.announce(MaplePacketCreator.enableActions());
        removePet(pet, shift_left);
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    @Override
    public void updatePartyMemberHP() {
        if (party != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = Server.getInstance().getWorld(world).getChannel(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.client.announce(MaplePacketCreator.updatePartyMemberHP(getId(), getStat(MapleStat.HP), getStat(MapleStat.MAXHP)));
                    }
                }
            }
        }
    }

    public void updateQuest(MapleQuestStatus quest) {
        quests.put(quest.getQuest(), quest);
        if (quest.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
            announce(MaplePacketCreator.questProgress((short) quest.getQuest().getId(), quest.getProgress(0)));
            if (quest.getQuest().getInfoNumber() > 0) {
                announce(MaplePacketCreator.questProgress(quest.getQuest().getInfoNumber(), Integer.toString(quest.getMedalProgress())));
            }
            announce(MaplePacketCreator.updateQuestInfo((short) quest.getQuest().getId(), quest.getNpc()));
        } else if (quest.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
            announce(MaplePacketCreator.completeQuest((short) quest.getQuest().getId(), quest.getCompletionTime()));
        } else if (quest.getStatus().equals(MapleQuestStatus.Status.NOT_STARTED)) {
            announce(MaplePacketCreator.forfeitQuest((short) quest.getQuest().getId()));
        }
    }

    public void questTimeLimit(final MapleQuest quest, int time) {
        ScheduledFuture<?> sf = TimerManager.getInstance().schedule(new Runnable() {
            
            @Override
            public void run() {
                announce(MaplePacketCreator.questExpire(quest.getId()));
                MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
                newStatus.setForfeited(getQuest(quest).getForfeited() + 1);
                updateQuest(newStatus);
            }
        }, time);
        announce(MaplePacketCreator.addQuestTimeLimit(quest.getId(), time));
        timers.add(sf);
    }

    @Override
    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    @Override
    protected void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        announce(MaplePacketCreator.updatePlayerStats(Collections.singletonList(new Pair<>(stat, newval)), itemReaction));
    }

    @Override
    public void announce(final byte[] packet) {
        client.announce(packet);
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (!this.isHidden() || client.getPlayer().getGMLevel() > 0) {
            client.announce(MaplePacketCreator.spawnPlayerMapobject(this));
        }
    }

    @Override
    public void setPortalDelay(long delay) {
        this.portaldelay = System.currentTimeMillis() + delay;
    }

    @Override
    public long getPortalDelay() {
        return portaldelay;
    }

    public void blockPortal(String scriptName) {
        if (!blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.add(scriptName);
            client.announce(MaplePacketCreator.enableActions());
        }
    }

    public void unblockPortal(String scriptName) {
        if (blockedPortals.contains(scriptName) && scriptName != null) {
            blockedPortals.remove(scriptName);
        }
    }

    public boolean containsAreaInfo(int area, String info) {
        Short area_ = (short) area;
        if (area_info.containsKey(area_)) {
            return area_info.get(area_).contains(info);
        }
        return false;
    }

    public void updateAreaInfo(int area, String info) {
        area_info.put((short) area, info);
        announce(MaplePacketCreator.updateAreaInfo(area, info));
    }

    public void equipPendantOfSpirit() {
        if (pendantOfSpirit == null) {
            pendantOfSpirit = TimerManager.getInstance().register(new Runnable() {
                
                @Override
                public void run() {
                    if (pendantExp < 3) {
                        pendantExp++;
                        message("Pendant of the Spirit has been equipped for " + pendantExp + " hour(s), you will now receive " + pendantExp + "0% bonus exp.");
                    } else {
                        pendantOfSpirit.cancel(false);
                    }
                }
            }, 3600000); //1 hour
        }
    }
    
    public void unequipPendantOfSpirit() {
        if (pendantOfSpirit != null) {
            pendantOfSpirit.cancel(false);
            pendantOfSpirit = null;
        }
        pendantExp = 0;
    }

    public void increaseEquipExp(int mobexp) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (Item item : getInventory(MapleInventoryType.EQUIPPED).list()) {
            Equip nEquip = (Equip) item;
            String itemName = mii.getName(nEquip.getItemId());
            if (itemName == null) {
                continue;
            }

            if ((itemName.contains("Reverse") && nEquip.getItemLevel() < 4) || itemName.contains("Timeless") && nEquip.getItemLevel() < 6) {
                nEquip.gainItemExp(client, mobexp, itemName.contains("Timeless"));
            }
        }
    }

    public void empty(final boolean remove) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        if (hpDecreaseTask != null) {
            hpDecreaseTask.cancel(false);
        }
        if (beholderHealingSchedule != null) {
            beholderHealingSchedule.cancel(false);
        }
        if (beholderBuffSchedule != null) {
            beholderBuffSchedule.cancel(false);
        }
        if (BerserkSchedule != null) {
            BerserkSchedule.cancel(false);
        }
        if (recoveryTask != null) {
            recoveryTask.cancel(false);
        }
        clearMisc();
        cancelExpirationTask();
        for (ScheduledFuture<?> sf : timers) {
            sf.cancel(false);
        }
        timers.clear();
        if (maplemount != null) {
            maplemount.empty();
            maplemount = null;
        }
        if (remove) {
            partyQuest = null;
            events = null;
            mpc = null;
            mgc = null;
            events = null;
            party = null;
            family = null;
            client = null;
            map = null;
            timers = null;
        }
    }
}
