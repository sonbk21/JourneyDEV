/* 
 * Lucia's Shop - JourneyMS
*/
var status;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else {
        if (status != 1) {
            cm.dispose();
            return;
        }
        status--;
    }
    if (status == 0) {
        cm.sendSimple("Hello, my name is Lucia!\r\nDo you want to trade some of your #e#bMaple Coins#k#n?\r\n\r\n#b#L1#Buy Items with Maple Coins#l\r\n#L2#Trade Maple Leaves for Maple Coins (25:1)#l");
    } else if (status == 1) {
        if (selection == 1) {
            cm.dispose();
            cm.openShop();
        } else if (selection == 2) {
            cm.sendGetNumber("You currently have #e#b#c4001126# Maple Leaves#k#n. How many #eCoins#n would you like?", cm.getPlayer().getItemQuantity(4001126, false)/25, 1, 100);
        }
    } else if (status == 2) {
        if (cm.getPlayer().getItemQuantity(4001126, false) >= selection*25 && cm.canHold(4310000)) {
            cm.gainItem(4001126, -selection*25, true);
            cm.gainItem(4310000, selection, true);
            cm.sendOk("Thanks for doing business with me!\r\n\r\n#fUI/UIWindow.img/QuestIcon/4/0#\r\n#i4310000# #b"+selection+" #z4310000#");
            cm.dispose();
        } else {
            cm.sendOk("You do not have enough leaves or your inventory is full.");
            cm.dispose();
        }
    }
}