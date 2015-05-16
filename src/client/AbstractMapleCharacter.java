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

import client.properties.DiseaseValueHolder;
import client.properties.MapleBuffStat;
import client.properties.MapleDisease;
import client.properties.MapleJob;
import client.properties.MapleKeyBinding;
import client.inventory.MapleMount;
import client.properties.MapleQuestStatus;
import client.inventory.MapleRing;
import client.properties.MapleSkinColor;
import client.properties.MapleStat;
import client.properties.Skill;
import client.properties.SkillFactory;
import client.properties.SkillMacro;
import client.Achievements.Achievement;
import client.Achievements.Status;
import client.autoban.AutobanManager;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleWeaponType;
import client.properties.PotentialLine.PotentialStat;
import constants.ItemConstants;
import constants.skills.Beginner;
import constants.skills.DarkKnight;
import constants.skills.DragonKnight;
import constants.skills.Fighter;
import constants.skills.Hero;
import constants.skills.Page;
import constants.skills.Paladin;
import constants.skills.Spearman;
import constants.skills.Swordsman;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.server.PlayerBuffValueHolder;
import net.server.PlayerCoolDownValueHolder;
import net.server.Server;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.MapleMessenger;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import net.server.world.World;
import scripting.event.EventInstanceManager;
import server.CashShop;
import server.MapleItemInformationProvider;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MaplePortal;
import server.MapleShop;
import server.MapleStatEffect;
import server.MapleStorage;
import server.MapleTrade;
import server.events.MapleEvents;
import server.events.gm.MapleFitness;
import server.events.gm.MapleOla;
import server.life.MapleMonster;
import server.maps.AbstractAnimatedMapleMapObject;
import server.maps.HiredMerchant;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SavedLocation;
import server.maps.SavedLocationType;
import server.partyquest.MonsterCarnival;
import server.partyquest.MonsterCarnivalParty;
import server.partyquest.PartyQuest;
import server.quest.MapleQuest;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;

/*
* Journey MS
* This is not really interesting, I tried to use this for decreasing the no. lines in maplecharacter
*/

public abstract class AbstractMapleCharacter extends AbstractAnimatedMapleMapObject {

    protected static final String LEVEL_200 = "[Congrats] %s has reached Level 200! Congratulate %s on such an amazing achievement!";
    protected static final int[] DEFAULT_KEY = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39};
    protected static final int[] DEFAULT_TYPE = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4, 4};
    protected static final int[] DEFAULT_ACTION = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 22, 52, 21, 25, 26, 23, 27};
    protected int world;
    protected int accountid, id, gmLevel;
    protected int rank, rankMove, jobRank, jobRankMove;
    protected PlayerStats stats;
    protected int hpMpApUsed;
    protected int gender;
    protected int remainingAp, remainingSp;
    protected int fame;
    protected int initialSpawnPoint, mapid;
    protected int currentPage, currentType = 0, currentTab = 1;
    protected int chair;
    protected int itemEffect;
    protected int guildid, guildrank, allianceRank;
    protected int messengerposition = 4;
    protected int slots = 0;
    protected int energybar;
    protected int ci = 0;
    protected MapleFamily family;
    protected int familyId;
    protected int bookCover;
    protected int markedMonster = 0;
    protected int battleshipHp = 0;
    protected int mesosTraded = 0;
    protected int possibleReports = 10;
    protected int dojoPoints, vanquisherStage, dojoStage, dojoEnergy, vanquisherKills;
    protected int warpToId;
    protected int expRate = 1, mesoRate = 1, dropRate = 1;
    protected int omokwins, omokties, omoklosses, matchcardwins, matchcardties, matchcardlosses;
    protected int married;
    protected long dojoFinish, lastfametime, lastUsedCashItem, lastHealed;
    protected transient int localmaxhp, localmaxmp, localstr, localdex, localluk, localint_, magic, watk;
    protected Potentials abilities;
    protected boolean hidden, canDoor = true, Berserk, hasMerchant;
    protected int linkedLevel = 0;
    protected String linkedName = null;
    protected boolean finishedDojoTutorial, dojoParty;
    protected String name;
    protected String chalktext;
    protected String search = null;
    protected final AtomicInteger exp = new AtomicInteger();
    protected final AtomicInteger gachaexp = new AtomicInteger();
    protected final AtomicInteger meso = new AtomicInteger();
    protected int merchantmeso;
    protected BuddyList buddylist;
    protected EventInstanceManager eventInstance = null;
    protected HiredMerchant hiredMerchant = null;
    protected MapleClient client;
    protected MapleGuildCharacter mgc = null;
    protected MaplePartyCharacter mpc = null;
    protected MapleInventory[] inventory = null;
    protected MapleMap map, dojoMap;//Make a Dojo pq instance
    protected MapleMessenger messenger = null;
    protected MapleMiniGame miniGame;
    protected MapleMount maplemount;
    protected MapleParty party;
    protected final MaplePet[] pets = new MaplePet[3];
    protected MaplePlayerShop playerShop = null;
    protected MapleShop shop = null;
    protected MapleStorage storage = null;
    protected MapleTrade trade = null;
    protected SavedLocation savedLocations[];
    protected SkillMacro[] skillMacros = new SkillMacro[5];
    protected List<Integer> lastmonthfameids;
    protected Achievements achievements = null;
    protected Map<MapleQuest, MapleQuestStatus> quests = new LinkedHashMap<>();
    protected final Set<MapleMonster> controlled = new LinkedHashSet<>();
    protected final Map<Integer, String> entered = new LinkedHashMap<>();
    protected final Set<MapleMapObject> visibleMapObjects = new LinkedHashSet<>();
    protected final Map<Skill, SkillEntry> skills = new LinkedHashMap<>();
    protected final EnumMap<MapleBuffStat, MapleBuffStatValueHolder> effects = new EnumMap<>(MapleBuffStat.class);
    protected final Map<Integer, MapleKeyBinding> keymap = new LinkedHashMap<>();
    protected final Map<Integer, MapleSummon> summons = new LinkedHashMap<>();
    protected final Map<Integer, MapleCoolDownValueHolder> coolDowns = new LinkedHashMap<>(50);
    protected final EnumMap<MapleDisease, DiseaseValueHolder> diseases = new EnumMap<>(MapleDisease.class);
    protected final List<MapleDoor> doors = new ArrayList<>();
    protected ScheduledFuture<?> dragonBloodSchedule;
    protected final ScheduledFuture<?> mapTimeLimitTask = null;
    protected final ScheduledFuture<?>[] fullnessSchedule = new ScheduledFuture<?>[3];
    protected ScheduledFuture<?> hpDecreaseTask;
    protected ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, BerserkSchedule;
    protected ScheduledFuture<?> expiretask;
    protected ScheduledFuture<?> recoveryTask, fishingTask;
    protected List<ScheduledFuture<?>> timers = new ArrayList<>();
    protected final NumberFormat nf = new DecimalFormat("#,###,###,###");
    protected final ArrayList<Integer> excluded = new ArrayList<>();
    protected MonsterBook monsterbook;
    protected final List<MapleRing> crushRings = new ArrayList<>();
    protected final List<MapleRing> friendshipRings = new ArrayList<>();
    protected MapleRing marriageRing;
    protected static final String[] ariantroomleader = new String[3];
    protected static final int[] ariantroomslot = new int[3];
    protected CashShop cashshop;
    protected long portaldelay = 0, lastcombo = 0;
    protected short combocounter = 0;
    protected final List<String> blockedPortals = new ArrayList<>();
    protected final Map<Short, String> area_info = new LinkedHashMap<>();
    protected AutobanManager autoban;
    protected boolean isbanned = false;
    protected ScheduledFuture<?> pendantOfSpirit = null; //1122017
    protected byte pendantExp = 0, lastmobcount = 0;
    protected final int[] trockmaps = new int[5];
    protected final int[] viptrockmaps = new int[10];
    protected Map<String, MapleEvents> events = new LinkedHashMap<>();
    protected PartyQuest partyQuest = null;
    protected boolean loggedIn = false;
    protected final int[] fishingRewards = {0, 1, 2, 3, 2022012, 2022013, 4000088, 4000153, 4000154, 4000155, 4000156, 4000157, 4000158, 4000159, 4000160,
                                          4000161, 4000162, 4000163, 4000164, 2022022, 2022023, 2022265, 2022272, 1432008, 1432039, 2022324, 2022040,
                                          4000165, 4000166};
    protected boolean supremeWorld = false;
    protected int rebirths = 0;
    protected boolean autoRebirth = true;
    
    public boolean isAchievementCompleted(String name) {
        return achievements.getStatus(Achievement.valueOf(name)) == Status.END;
    }

    public void addCrushRing(MapleRing r) {
        crushRings.add(r);
    }

    public MapleRing getRingById(int id) {
        for (MapleRing ring : getCrushRings()) {
            if (ring.getRingId() == id) {
                return ring;
            }
        }
        for (MapleRing ring : getFriendshipRings()) {
            if (ring.getRingId() == id) {
                return ring;
            }
        }
        if (getMarriageRing().getRingId() == id) {
            return getMarriageRing();
        }

        return null;
    }

    public int addDojoPointsByMap() {
        int pts = 0;
        if (dojoPoints < 17000) {
            pts = 1 + ((getMap().getId() - 1) / 100 % 100) / 6;
            if (!dojoParty) {
                pts++;
            }
            this.dojoPoints += pts;
        }
        return pts;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void addExcluded(int x) {
        excluded.add(x);
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public void addFriendshipRing(MapleRing r) {
        friendshipRings.add(r);
    }
    
    public void addMesosTraded(int gain) {
        this.mesosTraded += gain;
    }
    
    public void addPet(MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                pets[i] = pet;
                return;
            }
        }
    }

    public void addStat(MapleStat type, int up) {
        int cur = stats.getStat(type);
        if (type == MapleStat.HP) {
            up = Math.min(up + cur, localmaxhp) - cur;
        } else if (type == MapleStat.MP) {
            up = Math.min(up + cur, localmaxmp) - cur;
        }  else if (type == MapleStat.MAXHP) {
            up = Math.min(up + cur, 30000) - cur;
        }  else if (type == MapleStat.MAXMP) {
            up = Math.min(up + cur, 30000) - cur;
        }
        int total = stats.addToStat(type, up);
        updateSingleStat(type, total);
        if (type == MapleStat.HP)
            updatePartyMemberHP();
    }
    
    public void addPotentialStat(PotentialStat type, int up) {
        MapleStat newtype;
        switch (type) {
            case BDM: stats.setBDM(stats.getBDM() + ((float) up)/100); return;
            case HP: newtype = MapleStat.MAXHP; break;
            case MP: newtype = MapleStat.MAXMP; break;
            default: newtype = MapleStat.values()[type.ordinal() + 5];
        }
        int total = stats.addToStat(newtype, up); 
        updateSingleStat(newtype, total);
    }
    
    public void decreaseHpMp(MapleStat type, int down) {
        int cur = stats.getStat(type);
        if (type == MapleStat.HP) {
            down = Math.max(cur - down, 0) + cur;
            if (cur == down)
                playerDead();
        } else if (type == MapleStat.MP) {
            down = Math.min(cur - down, 0) + cur;
        }
        int total = stats.decreaseStat(type, down);
        updateSingleStat(type, total);
        if (type == MapleStat.HP)
            updatePartyMemberHP();
    }
    
    public void setStat(MapleStat type, int value) {
        stats.setStat(type, value);
        recalcLocalStats();
        updateSingleStat(type, value);
    }
    
    public void setBossDamage(float bdm) {
        stats.setBDM(bdm);
    }
    
    public void setHp(int value) {
        setStat(MapleStat.HP, value);
    }

    public int addHP(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        MapleJob jobtype = player.getMapleJob();
        int MaxHP = stats.getStat(MapleStat.MAXHP);
        if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
            return MaxHP;
        }
        if (jobtype.isA(MapleJob.BEGINNER)) {
            MaxHP += 8;
        } else if (jobtype.isA(MapleJob.WARRIOR) || jobtype.isA(MapleJob.DAWNWARRIOR1)) {
            if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(10000000) : SkillFactory.getSkill(1000001)) > 0) {
                MaxHP += 20;
            } else {
                MaxHP += 8;
            }
        } else if (jobtype.isA(MapleJob.MAGICIAN) || jobtype.isA(MapleJob.BLAZEWIZARD1)) {
            MaxHP += 6;
        } else if (jobtype.isA(MapleJob.BOWMAN) || jobtype.isA(MapleJob.WINDARCHER1)) {
            MaxHP += 8;
        } else if (jobtype.isA(MapleJob.THIEF) || jobtype.isA(MapleJob.NIGHTWALKER1)) {
            MaxHP += 8;
        } else if (jobtype.isA(MapleJob.PIRATE) || jobtype.isA(MapleJob.THUNDERBREAKER1)) {
            if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100000) : SkillFactory.getSkill(5100000)) > 0) {
                MaxHP += 18;
            } else {
                MaxHP += 8;
            }
        }
        return MaxHP;
    }

    public int addMP(MapleClient c) {
        MapleCharacter player = c.getPlayer();
        int MaxMP = stats.getStat(MapleStat.MAXMP);
        if (player.getHpMpApUsed() > 9999 || MaxMP >= 30000) {
            return MaxMP;
        }
        if (player.getMapleJob().isA(MapleJob.BEGINNER) || player.getMapleJob().isA(MapleJob.NOBLESSE) || player.getMapleJob().isA(MapleJob.LEGEND)) {
            MaxMP += 6;
        } else if (player.getMapleJob().isA(MapleJob.WARRIOR) || player.getMapleJob().isA(MapleJob.DAWNWARRIOR1) || player.getMapleJob().isA(MapleJob.ARAN1)) {
            MaxMP += 2;
        } else if (player.getMapleJob().isA(MapleJob.MAGICIAN) || player.getMapleJob().isA(MapleJob.BLAZEWIZARD1)) {
            if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(12000000) : SkillFactory.getSkill(2000001)) > 0) {
                MaxMP += 18;
            } else {
                MaxMP += 14;
            }

        } else if (player.getMapleJob().isA(MapleJob.BOWMAN) || player.getMapleJob().isA(MapleJob.THIEF)) {
            MaxMP += 10;
        } else if (player.getMapleJob().isA(MapleJob.PIRATE)) {
            MaxMP += 14;
        }

        return MaxMP;
    }

    public void addSummon(int id, MapleSummon summon) {
        summons.put(id, summon);
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjects.add(mo);
    }

    public boolean isBanned() {
        return isbanned;
    }

    public int getMaxBaseDamage(int watk) {
        int maxbasedamage;
        if (watk == 0) {
            maxbasedamage = 1;
        } else {
            Item weapon_item = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            if (weapon_item != null) {
                MapleWeaponType weapon = MapleItemInformationProvider.getInstance().getWeaponType(weapon_item.getItemId());
                int mainstat;
                int secondarystat;
                if (weapon == MapleWeaponType.BOW || weapon == MapleWeaponType.CROSSBOW) {
                    mainstat = localdex;
                    secondarystat = localstr;
                } else if ((getMapleJob().isA(MapleJob.THIEF) || getMapleJob().isA(MapleJob.NIGHTWALKER1)) && (weapon == MapleWeaponType.CLAW || weapon == MapleWeaponType.DAGGER)) {
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                } else {
                    mainstat = localstr;
                    secondarystat = localdex;
                }
                maxbasedamage = (int) (((weapon.getMaxDamageMultiplier() * mainstat + secondarystat) / 100.0) * watk) + 10;
            } else {
                maxbasedamage = 0;
            }
        }
        return maxbasedamage;
    }

    public void setCombo(short count) {
        if (count < combocounter) {
            cancelEffectFromBuffStat(MapleBuffStat.ARAN_COMBO);
        }
        combocounter = (short) Math.min(30000, count);
        if (count > 0) {
            announce(MaplePacketCreator.showCombo(combocounter));
        }
    }
    
    abstract void cancelEffectFromBuffStat(MapleBuffStat stat);

    public void setLastCombo(long time) {
        lastcombo = time;
    }

    public short getCombo() {
        return combocounter;
    }

    public long getLastCombo() {
        return lastcombo;
    }

    public int getLastMobCount() { //Used for skills that have mobCount at 1. (a/b)
        return lastmobcount;
    }

    public void setLastMobCount(byte count) {
        lastmobcount = count;
    }

    abstract void cancelBuffEffects();

    public String getMedalText() {
        String medal = "";
        final Item medalItem = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -49);
        if (medalItem != null) {
            medal = "<" + MapleItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
        }
        return medal;
    }

    abstract void cancelPlayerBuffs(List<MapleBuffStat> buffstats);

    public boolean getCanDoor() {
        return canDoor;
    }

    public FameStatus getCanGiveFame(MapleCharacter from) {
        if (gmLevel > 0) {
            return FameStatus.OK;
        } else if (lastfametime >= System.currentTimeMillis() - 3600000 * 24) {
            return FameStatus.NOT_TODAY;
        } else if (lastmonthfameids.contains(from.getId())) {
            return FameStatus.NOT_THIS_MONTH;
        } else {
            return FameStatus.OK;
        }
    }

    public void setCI(int type) {
        this.ci = type;
    }

    public void setPage(int page) {
        this.currentPage = page;
    }

    public void setTab(int tab) {
        this.currentTab = tab;
    }

    public void setType(int type) {
        this.currentType = type;
    }

    public void setClearDoors() {
        doors.clear();
    }

    public void setClearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = null;
    }

    public int getQuantityItemId(int itemid) {
        return inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
    }

    public void decreaseReports() {
        this.possibleReports--;
    }

    public List<ScheduledFuture<?>> getTimers() {
        return Collections.unmodifiableList(timers);
    }
    
    public Potentials getAbilities() {
        return abilities;
    }

    public enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public int getAccountID() {
        return accountid;
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = new ArrayList<>();
        for (MapleBuffStatValueHolder mbsvh : effects.values()) {
            ret.add(new PlayerBuffValueHolder(mbsvh.startTime, mbsvh.effect));
        }
        return ret;
    }

    public List<PlayerCoolDownValueHolder> getAllCooldowns() {
        List<PlayerCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new PlayerCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public int getAllianceRank() {
        return this.allianceRank;
    }

    public int getAllowWarpToId() {
        return warpToId;
    }

    public static String getAriantRoomLeaderName(int room) {
        return ariantroomleader[room];
    }

    public static int getAriantSlotsRoom(int room) {
        return ariantroomslot[room];
    }

    public int getBattleshipHp() {
        return battleshipHp;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.startTime;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.value;
    }

    public int getBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return -1;
        }
        return mbsvh.effect.getSourceId();
    }

    protected List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        List<MapleBuffStat> buffstats = new ArrayList<>();
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : effects.entrySet()) {
            if (stateffect.getValue().effect.sameSource(effect) && (startTime == -1 || startTime == stateffect.getValue().startTime)) {
                buffstats.add(stateffect.getKey());
            }
        }
        return buffstats;
    }

    public int getChair() {
        return chair;
    }

    public String getChalkboard() {
        return this.chalktext;
    }

    public MapleClient getClient() {
        return client;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.COMPLETED)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public Collection<MapleMonster> getControlledMonsters() {
        return Collections.unmodifiableCollection(controlled);
    }

    public List<MapleRing> getCrushRings() {
        Collections.sort(crushRings);
        return Collections.unmodifiableList(crushRings);
    }

    public int getCurrentCI() {
        return ci;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getCurrentMaxHp() {
        return localmaxhp;
    }

    public int getCurrentMaxMp() {
        return localmaxmp;
    }

    public int getCurrentTab() {
        return currentTab;
    }

    public int getCurrentType() {
        return currentType;
    }

    public int getStat(MapleStat type) {
        return stats.getStat(type);
    }
    
    public int getLevel() {
        return stats.getStat(MapleStat.LEVEL);
    }
    
    public int getHp() {
        return stats.getStat(MapleStat.HP);
    }
    
    public int getMp() {
        return stats.getStat(MapleStat.MP);
    }
    
    public int getMaxHp() {
        return stats.getStat(MapleStat.MAXHP);
    }
    
    public int getMaxMp() {
        return stats.getStat(MapleStat.MAXMP);
    }
    
    public int getStr() {
        return stats.getStat(MapleStat.STR);
    }
    
    public int getDex() {
        return stats.getStat(MapleStat.DEX);
    }
    
    public int getInt() {
        return stats.getStat(MapleStat.INT);
    }
    
    public int getLuk() {
        return stats.getStat(MapleStat.LUK);
    }
    
    public float getBossDamage() {
        return stats.getBDM();
    }

    public int getDojoEnergy() {
        return dojoEnergy;
    }

    public boolean getDojoParty() {
        return dojoParty;
    }

    public int getDojoPoints() {
        return dojoPoints;
    }

    public int getDojoStage() {
        return dojoStage;
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public int getDropRate() {
        return dropRate;
    }

    public int getEnergyBar() {
        return energybar;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public ArrayList<Integer> getExcluded() {
        return excluded;
    }

    public int getExp() {
        return exp.get();
    }

    public int getGachaExp() {
        return gachaexp.get();
    }

    public int getExpRate() {
        return expRate;
    }

    public int getFace() {
        return getStat(MapleStat.FACE);
    }

    public int getFame() {
        return fame;
    }

    public MapleFamily getFamily() {
        return family;
    }

    public void setFamily(MapleFamily f) {
        this.family = f;
    }

    public int getFamilyId() {
        return familyId;
    }

    public boolean getFinishedDojoTutorial() {
        return finishedDojoTutorial;
    }

    public List<MapleRing> getFriendshipRings() {
        Collections.sort(friendshipRings);
        return Collections.unmodifiableList(friendshipRings);
    }

    public int getGender() {
        return gender;
    }

    public boolean isMale() {
        return getGender() == 0;
    }

    public MapleGuild getGuild() {
        try {
            return Server.getInstance().getGuild(getGuildId(), null);
        } catch (Exception ex) {
            return null;
        }
    }

    public int getGuildId() {
        return guildid;
    }

    public int getGuildRank() {
        return guildrank;
    }

    public int getHair() {
        return getStat(MapleStat.HAIR);
    }

    public HiredMerchant getHiredMerchant() {
        return hiredMerchant;
    }

    public int getHpMpApUsed() {
        return hpMpApUsed;
    }

    public int getId() {
        return id;
    }

    public static int getIdByName(String name) {
        try {
            int id;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id FROM characters WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return -1;
                    }
                    id = rs.getInt("id");
                }
            }
            return id;
        } catch (Exception e) {
        }
        return -1;
    }

    public static String getNameById(int id) {
        try {
            String name;
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }
                    name = rs.getString("name");
                }
            }
            return name;
        } catch (Exception e) {
        }
        return null;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[MapleItemInformationProvider.getInstance().getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public MapleJob getMapleJob() {
        return MapleJob.getById(getStat(MapleStat.JOB));
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public int getJobType() {
        return getStat(MapleStat.JOB) / 1000;
    }

    public Map<Integer, MapleKeyBinding> getKeymap() {
        return Collections.unmodifiableMap(keymap);
    }

    public long getLastHealed() {
        return lastHealed;
    }

    public long getLastUsedCashItem() {
        return lastUsedCashItem;
    }

    public int getFh() {
        if (getMap().getFootholds().findBelow(this.getPosition()) == null) {
            return 0;
        } else {
            return getMap().getFootholds().findBelow(this.getPosition()).getId();
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public int getMarkedMonster() {
        return markedMonster;
    }

    public MapleRing getMarriageRing() {
        return marriageRing;
    }

    public int getMarried() {
        return married;
    }

    public int getMasterLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).masterlevel;
    }
    
    public int getMaxPossibleDamage(int id, int level) {
        double maxDmg = getMaxBaseDamage(watk);
        switch (id) {
            case Beginner.THREE_SNAILS: maxDmg = 40; break;
            case Swordsman.POWER_STRIKE: maxDmg *= (1.60 + 0.05*level); break;
            case Swordsman.SLASH_BLAST: maxDmg *= (0.69 + 0.03*level); break;
            case Fighter.FINAL_ATTACK_AXE:
            case Fighter.FINAL_ATTACK_SWORD:
            case Spearman.FINAL_ATTACK_POLEARM:
            case Spearman.FINAL_ATTACK_SPEAR:
            case Page.FINAL_ATTACK_BW:
            case Page.FINAL_ATTACK_SWORD: maxDmg *= (1.00 + 0.05*level); break;
            case DragonKnight.DRAGON_ROAR: maxDmg *= (0.90 + 0.06*level); break;
            case DragonKnight.SACRIFICE: maxDmg *= (2.00 + 0.05*level); break;
            case DragonKnight.SPEAR_CRUSHER:
            case DragonKnight.POLEARM_CRUSHER: maxDmg = 3 * maxDmg * (0.50 + ((level < 11)?0.05:(level < 21)?0.045:0.04)*level); break;
            case DragonKnight.SPEAR_FURY:
            case DragonKnight.POLEARM_FURY: maxDmg *= (0.70 + ((level < 11)?0.10:(level < 21)?0.75:0.06)*level); break;
            case DarkKnight.RUSH:
            case Hero.RUSH:
            case Paladin.RUSH: maxDmg *= (0.80 + 0.02*level); break;
            default: maxDmg = 200000;
        }
        if (getStat(MapleStat.JOB) == 132 && Berserk)
            maxDmg *= (1.10 + 0.03*getSkillLevel(DarkKnight.BERSERK));
        return (int) Math.ceil(maxDmg*1.1);
    }

    public int getMaxLevel() {
        return isCygnus() ? 120 : 200;
    }

    public int getMeso() {
        return meso.get();
    }

    public int getMerchantMeso() {
        return merchantmeso;
    }

    public int getMesoRate() {
        return mesoRate;
    }

    public int getMesosTraded() {
        return mesosTraded;
    }

    public int getMessengerPosition() {
        return messengerposition;
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public MaplePartyCharacter getMPC() {
        //if (mpc == null) mpc = new MaplePartyCharacter(this);
        return mpc;
    }

    public void setMPC(MaplePartyCharacter mpc) {
        this.mpc = mpc;
    }

    public MapleMiniGame getMiniGame() {
        return miniGame;
    }

    public int getMiniGamePoints(String type, boolean omok) {
        if (omok) {
            switch (type) {
                case "wins":
                    return omokwins;
                case "losses":
                    return omoklosses;
                default:
                    return omokties;
            }
        } else {
            switch (type) {
                case "wins":
                    return matchcardwins;
                case "losses":
                    return matchcardlosses;
                default:
                    return matchcardties;
            }
        }
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }
    
    public Achievements getAchievements() {
        return achievements;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public MapleMount getMount() {
        return maplemount;
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public String getName() {
        return name;
    }

    public int getNextEmptyPetIndex() {
        for (int i = 0; i < 3; i++) {
            if (pets[i] == null) {
                return i;
            }
        }
        return 3;
    }

    public int getNoPets() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public int getNumControlledMonsters() {
        return controlled.size();
    }

    public MapleParty getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : -1);
    }

    public MaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public MaplePet[] getPets() {
        return pets;
    }

    public MaplePet getPet(int index) {
        return pets[index];
    }

    public byte getPetIndex(int petId) {
        for (byte i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == petId) {
                    return i;
                }
            }
        }
        return -1;
    }

    public byte getPetIndex(MaplePet pet) {
        for (byte i = 0; i < 3; i++) {
            if (pets[i] != null) {
                if (pets[i].getUniqueId() == pet.getUniqueId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int getPossibleReports() {
        return possibleReports;
    }

    public final byte getQuestStatus(final int quest) {
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getQuest().getId() == quest) {
                return (byte) q.getStatus().getId();
            }
        }
        return 0;
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
        }
        return quests.get(quest);
    }

    public boolean needQuestItem(int questid, int itemid) {
        if (questid <= 0) {
            return true; //For non quest items :3
        }
        MapleQuest quest = MapleQuest.getInstance(questid);
        return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid) < quest.getItemAmountNeeded(itemid);
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp;
    }

    public int getSavedLocation(String type) {
        SavedLocation sl = savedLocations[SavedLocationType.fromString(type).ordinal()];
        if (sl == null) {
            return 102000000;
        }
        int m = sl.getMapId();
        if (!SavedLocationType.fromString(type).equals(SavedLocationType.WORLDTOUR)) {
            setClearSavedLocation(SavedLocationType.fromString(type));
        }
        return m;
    }

    public String getSearch() {
        return search;
    }

    public MapleShop getShop() {
        return shop;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getSkillLevel(Skill skill) {
        if (skills.get(skill) == null) {
            return 0;
        }
        return skills.get(skill).skillevel;
    }

    public long getSkillExpiration(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return -1;
        }
        return ret.expiration;
    }

    public long getSkillExpiration(Skill skill) {
        if (skills.get(skill) == null) {
            return -1;
        }
        return skills.get(skill).expiration;
    }

    public MapleSkinColor getMapleSkinColor() {
        return MapleSkinColor.getById(getStat(MapleStat.SKIN));
    }

    public int getSlot() {
        return slots;
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public final int getStartedQuestsSize() {
        int i = 0;
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus().equals(MapleQuestStatus.Status.STARTED)) {
                if (q.getQuest().getInfoNumber() > 0) {
                    i++;
                }
                i++;
            }
        }
        return i;
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public Map<Integer, MapleSummon> getSummons() {
        return Collections.unmodifiableMap(summons);
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return magic;
    }

    public int getTotalWatk() {
        return watk;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public int getVanquisherKills() {
        return vanquisherKills;
    }

    public int getVanquisherStage() {
        return vanquisherStage;
    }

    public Collection<MapleMapObject> getVisibleMapObjects() {
        return Collections.unmodifiableCollection(visibleMapObjects);
    }

    public int getWorld() {
        return world;
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public String getGuildCost() {
        return nf.format(MapleGuild.CREATE_GUILD_COST);
    }

    abstract void hasGivenFame(MapleCharacter to);

    public boolean getHasMerchant() {
        return hasMerchant;
    }

    public boolean haveItem(int itemid) {
        return getItemQuantity(itemid, false) > 0;
    }

    public boolean isActiveBuffedValue(int skillid) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public boolean isAlive() {
        return stats.getStat(MapleStat.HP) > 0;
    }
    
    public boolean isSupremeWorld() {
        return supremeWorld;
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public boolean isCygnus() {
        return getJobType() == 1;
    }

    public boolean isAran() {
        return getMapleJob().getId() >= 2000 && getMapleJob().getId() <= 2112;
    }

    public boolean isBeginnerJob() {
        return (getMapleJob().getId() == 0 || getMapleJob().getId() == 1000 || getMapleJob().getId() == 2000) && stats.getStat(MapleStat.LEVEL) < 11;
    }

    public boolean isGM() {
        return gmLevel > 0;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public boolean isPartyLeader() {
        return party.getLeader() == party.getMemberById(getId());
    }
    
    abstract void levelUp(boolean takeexp);
    
    abstract void saveToDB();


    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }

    protected static class MapleBuffStatValueHolder {

        public MapleStatEffect effect;
        public long startTime;
        public int value;
        public ScheduledFuture<?> schedule;

        MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value) {
            super();
            this.effect = effect;
            this.startTime = startTime;
            this.schedule = schedule;
            this.value = value;
        }
    }

    protected static class MapleCoolDownValueHolder {

        public int skillId;
        public long startTime, length;
        public ScheduledFuture<?> timer;

        public MapleCoolDownValueHolder(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
            super();
            this.skillId = skillId;
            this.startTime = startTime;
            this.length = length;
            this.timer = timer;
        }
    }

    abstract void playerDead();

    abstract void recalcLocalStats();

    abstract void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule);

    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }

    public void setClearMGC() {
        this.mgc = null;
    }

    public void setSavedLocation(String type) {
        MaplePortal closest = map.findClosestPortal(getPosition());
        savedLocations[SavedLocationType.fromString(type).ordinal()] = new SavedLocation(getMapId(), closest != null ? closest.getId() : 0);
    }

    public void setAllianceRank(int rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public void setAllowWarpToId(int id) {
        this.warpToId = id;
    }

    public static void setAriantRoomLeader(int room, String charname) {
        ariantroomleader[room] = charname;
    }

    public static void setAriantSlotRoom(int room, int slot) {
        ariantroomslot[room] = slot;
    }

    public void setBattleshipHp(int battleshipHp) {
        this.battleshipHp = battleshipHp;
    }

    public void setBuddyCapacity(int capacity) {
        buddylist.setCapacity(capacity);
        client.announce(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setChair(int chair) {
        this.chair = chair;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
    }

    public void setDojoEnergy(int x) {
        this.dojoEnergy = x;
    }

    public void setDojoParty(boolean b) {
        this.dojoParty = b;
    }

    public void setDojoPoints(int x) {
        this.dojoPoints = x;
    }

    public void setDojoStage(int x) {
        this.dojoStage = x;
    }

    public void setDojoStart() {
        this.dojoMap = map;
        int stage = (map.getId() / 100) % 100;
        this.dojoFinish = System.currentTimeMillis() + (stage > 36 ? 15 : stage / 6 + 5) * 60000;
    }

    public void setRates() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        World worldz = Server.getInstance().getWorld(world);
        int hr = cal.get(Calendar.HOUR_OF_DAY);
        if ((haveItem(5360001) && hr > 6 && hr < 12) || (haveItem(5360002) && hr > 9 && hr < 15) || (haveItem(536000) && hr > 12 && hr < 18) || (haveItem(5360004) && hr > 15 && hr < 21) || (haveItem(536000) && hr > 18) || (haveItem(5360006) && hr < 5) || (haveItem(5360007) && hr > 2 && hr < 6) || (haveItem(5360008) && hr >= 6 && hr < 11)) {
            this.dropRate = 2 * worldz.getDropRate();
            this.mesoRate = 2 * worldz.getMesoRate();
        } else {
            this.dropRate = worldz.getDropRate();
            this.mesoRate = worldz.getMesoRate();
        }
        if ((haveItem(5211000) && hr > 17 && hr < 21) || (haveItem(5211014) && hr > 6 && hr < 12) || (haveItem(5211015) && hr > 9 && hr < 15) || (haveItem(5211016) && hr > 12 && hr < 18) || (haveItem(5211017) && hr > 15 && hr < 21) || (haveItem(5211018) && hr > 14) || (haveItem(5211039) && hr < 5) || (haveItem(5211042) && hr > 2 && hr < 8) || (haveItem(5211045) && hr > 5 && hr < 11) || haveItem(5211048)) {
            if (isBeginnerJob()) {
                this.expRate = 2;
            } else {
                this.expRate = 2 * worldz.getExpRate();
            }
        } else {
            if (isBeginnerJob()) {
                this.expRate = 1;
            } else {
                this.expRate = worldz.getExpRate();
            }
        }
    }

    public void setEnergyBar(int set) {
        energybar = set;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setGachaExp(int amount) {
        this.gachaexp.set(amount);
    }

    public void setFace(int face) {
        setStat(MapleStat.FACE, face);
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFamilyId(int familyId) {
        this.familyId = familyId;
    }

    public void setFinishedDojoTutorial() {
        this.finishedDojoTutorial = true;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public void setGM(int level) {
        this.gmLevel = level;
    }

    public void setGuildId(int _id) {
        this.guildid = _id;
    }

    public void setGuildRank(int _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setHair(int hair) {
        setStat(MapleStat.HAIR, hair);
    }

    public void setHiredMerchant(HiredMerchant merchant) {
        this.hiredMerchant = merchant;
    }
    
    abstract void updatePartyMemberHP();

    public void setHpMpApUsed(int mpApUsed) {
        this.hpMpApUsed = mpApUsed;
    }

    public void setInventory(MapleInventoryType type, MapleInventory inv) {
        inventory[type.ordinal()] = inv;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public void setJob(MapleJob job) {
        setStat(MapleStat.JOB, job.getId());
    }

    public void setLastHealed(long time) {
        this.lastHealed = time;
    }

    public void setLastUsedCashItem(long time) {
        this.lastUsedCashItem = time;
    }
    
    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMarkedMonster(int markedMonster) {
        this.markedMonster = markedMonster;
    }

    public void setMaxHp(int hp, boolean ap) {
        hp = Math.min(30000, hp);
        if (ap) {
            setHpMpApUsed(getHpMpApUsed() + 1);
        }
        stats.setStat(MapleStat.HP, hp);
        recalcLocalStats();
    }

    public void setMaxMp(int mp, boolean ap) {
        mp = Math.min(30000, mp);
        if (ap) {
            setHpMpApUsed(getHpMpApUsed() + 1);
        }
        stats.setStat(MapleStat.MP, mp);
        recalcLocalStats();
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void setMessengerPosition(int position) {
        this.messengerposition = position;
    }

    public void setMiniGame(MapleMiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public void setMiniGamePoints(MapleCharacter visitor, int winnerslot, boolean omok) {
        if (omok) {
            if (winnerslot == 1) {
                this.omokwins++;
                visitor.omoklosses++;
            } else if (winnerslot == 2) {
                visitor.omokwins++;
                this.omoklosses++;
            } else {
                this.omokties++;
                visitor.omokties++;
            }
        } else {
            if (winnerslot == 1) {
                this.matchcardwins++;
                visitor.matchcardlosses++;
            } else if (winnerslot == 2) {
                visitor.matchcardwins++;
                this.matchcardlosses++;
            } else {
                this.matchcardties++;
                visitor.matchcardties++;
            }
        }
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParty(MapleParty party) {
        if (party == null) {
            this.mpc = null;
        }
        this.party = party;
    }

    public void setPlayerShop(MaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp = remainingSp;
    }

    public void setSearch(String find) {
        search = find;
    }

    public void setSkinColor(MapleSkinColor skinColor) {
        setStat(MapleStat.SKIN, skinColor.getId());
    }

    public byte getSlots(int type) {
        return type == MapleInventoryType.CASH.getType() ? 96 : inventory[type].getSlotLimit();
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public void setSlot(int slotid) {
        slots = slotid;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public void setVanquisherKills(int x) {
        this.vanquisherKills = x;
    }

    public void setVanquisherStage(int x) {
        this.vanquisherStage = x;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public static class SkillEntry {

        public int masterlevel;
        public byte skillevel;
        public long expiration;

        public SkillEntry(byte skillevel, int masterlevel, long expiration) {
            this.skillevel = skillevel;
            this.masterlevel = masterlevel;
            this.expiration = expiration;
        }

        @Override
        public String toString() {
            return skillevel + ":" + masterlevel;
        }
    }

    public boolean isSkillCooling(int skillId) {
        return coolDowns.containsKey(skillId);
    }

    abstract void updateSingleStat(MapleStat stat, int newval);

    abstract void updateSingleStat(MapleStat stat, int newval, boolean itemReaction);

    abstract void announce(final byte[] packet);

    @Override
    public int getObjectId() {
        return getId();
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void setObjectId(int id) {
    }

    @Override
    public String toString() {
        return name;
    }
    private int givenRiceCakes;
    private boolean gottenRiceHat;

    public int getGivenRiceCakes() {
        return givenRiceCakes;
    }

    public void increaseGivenRiceCakes(int amount) {
        this.givenRiceCakes += amount;
    }

    public boolean getGottenRiceHat() {
        return gottenRiceHat;
    }

    public void setGottenRiceHat(boolean b) {
        this.gottenRiceHat = b;
    }

    public int getLinkedLevel() {
        return linkedLevel;
    }

    public String getLinkedName() {
        return linkedName;
    }

    public CashShop getCashShop() {
        return cashshop;
    }

    public void setPortalDelay(long delay) {
        this.portaldelay = System.currentTimeMillis() + delay;
    }

    public long getPortalDelay() {
        return portaldelay;
    }

    public List<String> getBlockedPortals() {
        return Collections.unmodifiableList(blockedPortals);
    }

    public String getAreaInfo(int area) {
        return area_info.get((short) area);
    }

    public Map<Short, String> getAreaInfos() {
        return Collections.unmodifiableMap(area_info);
    }

    public int[] getTrockMaps() {
        return trockmaps;
    }

    public int[] getVipTrockMaps() {
        return viptrockmaps;
    }

    public int getTrockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (trockmaps[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void removeFromTrocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (trockmaps[i] == map) {
                trockmaps[i] = 999999999;
                break;
            }
        }
    }

    public void addTrockMap() {
        if (getTrockSize() >= 5) {
            return;
        }
        trockmaps[getTrockSize()] = getMapId();
    }

    public boolean isTrockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (trockmaps[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int getVipTrockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (viptrockmaps[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void removeFromVipTrocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (viptrockmaps[i] == map) {
                viptrockmaps[i] = 999999999;
                break;
            }
        }
    }

    public void addVipTrockMap() {
        if (getVipTrockSize() >= 10) {
            return;
        }

        viptrockmaps[getVipTrockSize()] = getMapId();
    }

    public boolean isVipTrockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (viptrockmaps[i] == id) {
                return true;
            }
        }
        return false;
    }
    //EVENTS
    private byte team = 0;
    private MapleFitness fitness;
    private MapleOla ola;
    private long snowballattack;

    public byte getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = (byte) team;
    }

    public MapleOla getOla() {
        return ola;
    }

    public void setOla(MapleOla ola) {
        this.ola = ola;
    }

    public MapleFitness getFitness() {
        return fitness;
    }

    public void setFitness(MapleFitness fit) {
        this.fitness = fit;
    }

    public long getLastSnowballAttack() {
        return snowballattack;
    }

    public void setLastSnowballAttack(long time) {
        this.snowballattack = time;
    }
    
    private int cp = 0;
    private int obtainedcp = 0;
    private MonsterCarnivalParty carnivalparty;
    private MonsterCarnival carnival;

    public MonsterCarnivalParty getCarnivalParty() {
        return carnivalparty;
    }

    public void setCarnivalParty(MonsterCarnivalParty party) {
        this.carnivalparty = party;
    }

    public MonsterCarnival getCarnival() {
        return carnival;
    }

    public void setCarnival(MonsterCarnival car) {
        this.carnival = car;
    }

    public int getCP() {
        return cp;
    }

    public int getObtainedCP() {
        return obtainedcp;
    }

    public void addCP(int cp) {
        this.cp += cp;
        this.obtainedcp += cp;
    }

    public void decreaseCP(int cp) {
        this.cp -= cp;
    }

    public void setObtainedCP(int cp) {
        this.obtainedcp = cp;
    }

    public int getAndRemoveCP() {
        int rCP = 10;
        if (cp < 9) {
            rCP = cp;
            cp = 0;
        } else {
            cp -= 10;
        }

        return rCP;
    }

    public AutobanManager getAutobanManager() {
        return autoban;
    }

    public Map<String, MapleEvents> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    public PartyQuest getPartyQuest() {
        return partyQuest;
    }

    public void setPartyQuest(PartyQuest pq) {
        partyQuest = pq;
    }

    public void setLoggedInFalse() {
        loggedIn = false;
    }

    public boolean isLoggedin() {
        return loggedIn;
    }

    public void setMapId(int mapid) {
        this.mapid = mapid;
    }
    
    public boolean toggleAutoRebirth() {
        autoRebirth = !autoRebirth;
        return autoRebirth;
    }
}
