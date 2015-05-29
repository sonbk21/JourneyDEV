/*
 * JourneyMS
 * Gaga
 * Fishing Npc
 */

var status;

function start() {
    status = -1;
    if (cm.getMapId() == 970020000)
        cm.sendYesNo("Did you know that in some cases, artifacts can even be found underwater?\r\nQuite fascinating, I know! If you want to try, just get yourself a fishing chair and some baits. What do you say?");
    else {
        cm.sendOk("Artifacts are fascinating, don't you agree?");
        cm.dispose();
    }
}

function action(mode, type, selection) {
    if (mode ==  1) {
        status ++;
        if (status == 0)
            cm.sendGetNumber("How many nets do you want to buy? They are 5000 mesos each.", Math.min(100, cm.getPlayer().getMeso()/5000), 1, 100);
        else if (status == 1) {
            if (cm.getPlayer().getMeso() >= selection*5000 && cm.canHold(2270008)) {
                cm.gainMeso(-selection*5000);
                cm.gainItem(2270008, selection, true);
                cm.sendOk("If you need a fishing chair, pay a visit to Lucia. Oh and don't forget to tell me if you catch any artifacts!");
            } else {
                cm.sendOk("You do not have enough mesos or your invenory is full");
            }
            cm.dispose();
        }
    } else {
        cm.sendOk("Artifacts are fascinating, don't you agree?");
        cm.dispose();
    }
}
