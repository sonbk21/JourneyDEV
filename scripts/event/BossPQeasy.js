/*
 * Boss PQ - Easy Mode
 * JourneyMS
 */

var eventMap;
var lobby;
//MV - 750k:10k exp, Choco Cream - 400k, Cursed Cat - 600k:15k, Two-face - 1.5m:167k,>>2 Reaper -1.2m:150k, Master Omen - 1.7m:123k, Sorcerer - 2.25m:200k, DarkHeart - 5.8m:248k, Scarecrow - 10m:1.2m
//Total Hp - 26.4m, 2.1m(8.4m) exp 
var bosses = [9400748, 8850015, 9400637, 8850014, 8850019, 8850020, 8850021, 8850017, 8850022];
var timeLimit = 600;
var instanceId;
var index;
var startTime;

function init() {
    instanceId = 1;
    index = 0;
}

function setup() {
    lobby = em.getChannelServer().getMapFactory().getMap(689010000);
    var instanceName = "BossPQeasy" + instanceId;
    var eim = em.newInstance(instanceName);
    instanceId++;
    index = 0;
    eim.setProperty("start", "false");
    eim.setProperty("clear", "false");
    eim.schedule("start", 1000);
    return eim;
}

function playerEntry(eim, player) {
    eventMap = eim.getMapInstance(689010001);
    player.changeMap(eventMap,eventMap.getPortal(0));
}

function start(eim) {
    eventMap.toggleDrops();
    eim.setProperty("start", "true");
    eim.schedule("timeOut", (timeLimit-3)*1000);
    eim.broadcastClock(timeLimit);
    startTime = eim.getTime();
    broadcastMessage(eim, "Prepare, the battle is about to begin!");
    eim.schedule("spawnNext", 5000);
}

function spawnNext(eim) {
    if (index < bosses.length) {
        var boss = em.getMob(bosses[index]);
        eim.registerMonster(boss);
        eim.spawnMonster(eventMap, boss, 5, -85);
        index ++;
    } else {
        eim.setProperty("clear", "true");
        eim.schedule("clearPQ", 500);
    }
}

function allMonstersDead(eim) {
    if (index < bosses.length)
        broadcastMessage(eim, "Prepare, the next boss will appear within the glimpse of an eye!");
    eim.schedule("spawnNext", 3000);
}

function timeOut(eim) {
    if (eim.getProperty("clear").equals("true"))
        return;
    broadcastMessage(eim, "Your party has taken too long and failed to complete the PQ.");
    eim.schedule("end", 3000);
}

function clearPQ(eim) {
    var time = Math.floor((eim.getTime() - startTime)/1000);
    var rank = eim.setRecord(time);
    var readableTime = (Math.floor(time/60))+":"+((time%60 > 9)?(time%60):"0"+(time%60));
    if (rank > 0) {
        broadcastMessage(eim, "Your party has beaten Easy mode in "+readableTime+"!");
        if (rank == 1)
            broadcastMessage(eim,  "That is a new record, congratulations!");
        else
            broadcastMessage(eim,  "Your party has ranked No. "+rank+", congratulations!");
    } else
        broadcastMessage(eim, "Congratulations, your party has beaten Easy mode in "+readableTime+"!");
    eim.schedule("end", 3000);
}

function end(eim) {
    var party = eim.getPlayers();
    var iter = party.iterator();
    while (iter.hasNext()) {
        var player = iter.next();
        if (eim.getProperty("clear").equals("true")) {
            player.addAchievementString("EASY", -1);
            eim.giveItem(player, 4001126, 50);
        }
        player.changeMap(lobby,lobby.getPortal(0));
        eim.unregisterPlayer(player);
    }
    eim.dispose();
}

function broadcastMessage(eim, msg) {
    var party = eim.getPlayers();
    var iter = party.iterator();
    while (iter.hasNext())
        iter.next().message(msg);
}

function playerDisconnected(eim, player) {
    player.getMap().removePlayer(player);
    player.setMap(lobby);
    eim.unregisterPlayer(player);
    if (eim.getPlayers().size() == 0)
        end(eim);
}

function playerDead(eim,player) {
    eim.addToDeathCount(player);
    if (eim.getDeathCount(player) < 4)
        player.message("You may revive "+(4 - eim.getDeathCount(player))+" more times. To revive, simply choose ok.");
}

function playerRevive(eim, player) {
    if (eim.getDeathCount(player) > 3) {
        player.setHp(50);
        player.changeMap(lobby, lobby.getPortal(0));
    } else {
        player.setHp(30000);
        player.changeMap(eventMap, eventMap.getPortal(0));
    }
    return false;
}

function monsterValue(eim) {
    return 1;
}

function leftParty(eim, player) {
    playerExit(eim, player);
}

function disbandParty(eim) {
    var party = eim.getPlayers();
    for (var i = 0; i < party.size(); i++)
        playerExit(eim, party.get(i));
    eim.dispose();
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    player.changeMap(lobby, lobby.getPortal(0));
}

function mapChanged(eim, player, mapid) {
    if (mapid != eventMap.getId()) {
        eim.unregisterPlayer(player);
        if (eim.getPlayers().size() == 0)
            end(eim);
    } else {
        if (eim.getProperty("start").equals("true"))
            eim.broadcastClock(player, Math.floor((eim.getTime() - startTime)/1000));
    }
}