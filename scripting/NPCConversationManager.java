/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import client.Battler;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import client.inventory.Equip;
import client.Skill;
import client.inventory.Item;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import constants.GameConstants;
import client.inventory.ItemFlag;
import client.MapleClient;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.SkillFactory;
import client.SkillEntry;
import client.MapleStat;
import constants.BattleConstants;
import constants.BattleConstants.MobExp;
import constants.BattleConstants.PokedexEntry;
import constants.DojoRank;
import server.MapleCarnivalParty;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleShopFactory;
import server.MapleSquad;
import server.maps.MapleMap;
import server.maps.Event_DojoAgent;
import server.quest.MapleQuest;
import tools.packet.EtcPacket;
import server.MapleItemInformationProvider;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import database.DatabaseConnection;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.InterServerHandler;
import handling.channel.handler.PlayersHandler;
import handling.login.LoginInformationProvider;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.guild.MapleGuild;
import server.MapleCarnivalChallenge;
import java.util.HashMap;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.script.Invocable;
import server.ItemInformation;
import server.MapleStatEffect;
import server.PokemonBattle;
import server.RankingWorker;
import server.RankingWorker.PokebattleInformation;
import server.RankingWorker.PokedexInformation;
import server.RankingWorker.PokemonInformation;
import server.SpeedRunner;
import server.StructItemOption;
import server.Timer.CloneTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.maps.Event_PyramidSubway;
import server.maps.MapleReactor;
import tools.FileoutputUtil;
import tools.HairAndEye;
import tools.StringUtil;
import tools.Triple;
import tools.packet.EtcPacket.NPCPacket;
import tools.packet.EtcPacket.UIPacket;
import tools.packet.MaplePacketCreator;
import tools.packet.MaplePacketCreator.GuildPacket;
import tools.packet.MaplePacketCreator.InfoPacket;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private String getText;
    private byte type; // -1 = NPC, 0 = start quest, 1 = end quest
    private byte lastMsg = -1;
    public boolean pendingDisposal = false;
    private Invocable iv;
    MapleReactor Tree = getClient().getPlayer().getMap().getReactorById(9702000);

    public NPCConversationManager(MapleClient c, int npc, int questid, byte type, Invocable iv) {
        super(c, npc, questid);
        this.type = type;
        this.iv = iv;
    }

    public int getTreeStat() {
        return Tree.getState();
    }

    public void gainTreeStat(int Stat) {
        Tree.setState((byte) (Tree.getState() + Stat));
    }

    public void UpTree() {
        Tree.hitReactor(c);
    }

    public int getTreeState() {
        return Tree.getState();
    }

    public void ResetTree() {
        Tree.setState((byte) 0);
        getPlayer().getMap().resetReactors();
        Item DropItem;
        int y = 1264;
        for (int x = -375; x < 1005; x += 35, y += 100) {
            if (Math.random() * 100 < 5) {
                DropItem = new Item(4000313, (byte) 0, (short) 1);
            } else {
                DropItem = new Item(4001126, (byte) 0, (short) 1);
            }
            getPlayer().getMap().spawnItemDrop(Tree, getPlayer(), DropItem, new Point(x, y), false, false);
        }
    }

    public Invocable getIv() {
        return iv;
    }

    public int getNpc() {
        return id;
    }

    public int getQuest() {
        return id2;
    }

    public byte getType() {
        return type;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void enterCS() {
        InterServerHandler.EnterCS(c, c.getPlayer(), false, false);
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void askMapSelection(final String sel) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(NPCPacket.getMapSelection(id, sel));
        lastMsg = (byte) (GameConstants.GMS ? 0x11 : 0x10);
    }

    public void sendNext(String text) {
        sendNext(text, id);
    }

    public void sendNext(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { //sendNext will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 01", (byte) 0));
        lastMsg = 0;
    }

    public void sendPlayerToNpc(String text) {
        sendNextS(text, (byte) 3, id);
    }

    public void sendNextNoESC(String text) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextNoESC(String text, int id) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextS(String text, byte type) {
        sendNextS(text, type, id);
    }

    public void sendNextS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 01", type, idd));
        lastMsg = 0;
    }

    public void sendPrev(String text) {
        sendPrev(text, id);
    }

    public void sendPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendPrevS(String text, byte type) {
        sendPrevS(text, type, id);
    }

    public void sendPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 00", type, idd));
        lastMsg = 0;
    }

    public void sendNextPrev(String text) {
        sendNextPrev(text, id);
    }

    public void sendNextPrev(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 01", (byte) 0));
        lastMsg = 0;
    }

    public void PlayerToNpc(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text, byte type) {
        sendNextPrevS(text, type, id);
    }

    public void sendNextPrevS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "01 01", type, idd));
        lastMsg = 0;
    }

    public void sendOk(String text) {
        sendOk(text, id);
    }

    public void sendOk(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 00", (byte) 0));
        lastMsg = 0;
    }

    public void sendOkS(String text, byte type) {
        sendOkS(text, type, id);
    }

    public void sendOkS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 0, text, "00 00", type, idd));
        lastMsg = 0;
    }

    public void sendYesNo(String text) {
        sendYesNo(text, id);
    }

    public void sendYesNo(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 2, text, "", (byte) 0));
        lastMsg = 2;
    }

    public void sendYesNoS(String text, byte type) {
        sendYesNoS(text, type, id);
    }

    public void sendYesNoS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 2, text, "", type, idd));
        lastMsg = 2;
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalkNum(id, text, def, min, max));
        lastMsg = 4;
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        askAcceptDecline(text, id);
    }

    public void askAcceptDecline(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = (byte) (GameConstants.GMS ? 0xF : 0xE);
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) lastMsg, text, "", (byte) 0));
    }

    public void askAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text, id);
    }

    public void askAcceptDeclineNoESC(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = (byte) (GameConstants.GMS ? 0xF : 0xE);
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) lastMsg, text, "", (byte) 1));
    }

    public void askAvatar(String text, int... args) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalkStyle(id, text, args));
        lastMsg = 9;
    }

    public void sendSimple(String text) {
        sendSimple(text, id);
    }

    public void sendSimple(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNext(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 5, text, "", (byte) 0));
        lastMsg = 5;
    }

    public void sendSimpleS(String text, byte type) {
        sendSimpleS(text, type, id);
    }

    public void sendSimpleS(String text, byte type, int idd) {
        if (lastMsg > -1) {
            return;
        }
        if (!text.contains("#L")) { //sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalk(id, (byte) 5, text, "", (byte) type, idd));
        lastMsg = 5;
    }

    public void sendStyle(String text, int styles[]) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalkStyle(id, text, styles));
        lastMsg = 9;
    }

    public void sendGetNumberS(int type, int type2, String text, int def, int min, int max) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalkNumS(type, type2, id, text, def, min, max));
        lastMsg = 4;
    }

    public void sendGetText(String text) {
        sendGetText(text, id);
    }

    public void sendGetText(String text, int id) {
        if (lastMsg > -1) {
            return;
        }
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().write(NPCPacket.getNPCTalkText(id, text));
        lastMsg = 3;
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void setHair(int hair) {
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.HAIR, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.FACE, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            c.getPlayer().setFace(args);
            c.getPlayer().updateSingleStat(MapleStat.FACE, args);
        } else {
            c.getPlayer().setHair(args);
            c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public void sendStorage() {
        if (c.getPlayer().getLevel() >= 50) {
            c.getPlayer().setConversation(4);
            c.getPlayer().getStorage().sendStorage(c, id);
        } else {
            c.getPlayer().dropMessage(1, "레벨 50 미만은 창고 이용이 불가능합니다.");
        }
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void openShopNPC(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c, this.id);
    }

    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, c.getPlayer().getMap().getStreetName());
    }

    public int gainGachaponItem(int id, int quantity, final String msg) {
        try {
            if (!MapleItemInformationProvider.getInstance().itemExists(id)) {
                return -1;
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness > 0) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega(c.getPlayer().getName(), " : got a(n)", item, rareness, msg));
            }
            c.getSession().write(InfoPacket.getShowItemGain(item.getItemId(), (short) quantity, true));
            return item.getItemId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int useNebuliteGachapon() {
        try {
            if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < 1) {
                return -1;
            }
            int grade = 0; // Default D
            final int chance = Randomizer.nextInt(100); // cannot gacha S, only from alien cube.
            final int chanceS = Randomizer.nextInt(200);
            if (chanceS == 1) { // Grade S
                grade = 4;
            } else if (chance < 1) { // Grade A
                grade = 3;
            } else if (chance < 3) { // Grade B
                grade = 2;
            } else if (chance < 35) { // Grade C
                grade = 1;
            } else { // grade == 0
                grade = Randomizer.nextInt(100) < 25 ? 5 : 0; // 25% again to get premium ticket piece				
            }
            int newId = 0;
            if (grade == 5) {
                newId = 4420000;
            } else {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final List<StructItemOption> pots = new LinkedList<StructItemOption>(ii.getAllSocketInfo(grade).values());
                while (newId == 0) {
                    StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                    if (pot != null) {
                        newId = pot.opID;
                    }
                }
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, newId, (short) 1);
            if (item == null) {
                return -1;
            }
            if (grade >= 3 && grade != 5) {
                World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega(c.getPlayer().getName(), " : (n)", item, (byte) 0, "Maple World"));
            }
            c.getSession().write(InfoPacket.getShowItemGain(newId, (short) 1, true));
            gainItem(2430748, (short) 1);
            gainItemSilent(5220094, (short) -1);
            return item.getItemId();
        } catch (Exception e) {
            System.out.println("[Error] Failed to use Nebulite Gachapon. " + e);
        }
        return -1;
    }

    
    public final boolean getFits(int nebuliteId, int itemId) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final StructItemOption pot = ii.getSocketInfo(nebuliteId);
            if (pot != null && GameConstants.optionTypeFits(pot.optionType, itemId)) {
                return true;
            } else {
                return false;
    }
    }   
    
    public void changeJob(int job) {
        c.getPlayer().changeJob(job);
    }

    public int getDemonMarking() {
        return c.getPlayer().getDemonMarking();
    }

    public void setDemonMarking(int mark) {
        c.getPlayer().setDemonMarking(mark);
    }

    public void startQuest(int idd) {
        MapleQuest.getInstance(idd).start(getPlayer(), id);
    }

    public void completeQuest(int idd) {
        MapleQuest.getInstance(idd).complete(getPlayer(), id);
    }

    public void forfeitQuest(int idd) {
        MapleQuest.getInstance(idd).forfeit(getPlayer());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(int idd) {
        MapleQuest.getInstance(idd).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(id2).forceComplete(getPlayer(), getNpc());
    }

    public final int getSkillMaxLevel(int skillid) {
        return SkillFactory.getSkill(skillid).getMaxLevel();
    }

    public void forceCompleteQuest(final int idd) {
        MapleQuest.getInstance(idd).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData() {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).setCustomData(customData);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        c.getPlayer().gainAp((short) amount);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().expandInventory(type, amt);
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<Short>();
        for (Item item : equipped.newList()) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            MapleInventoryManipulator.unequip(getC(), id, equip.getNextFreeSlot());
        }
    }

    public final void clearSkills() {
        final Map<Skill, SkillEntry> skills = new HashMap<Skill, SkillEntry>(getPlayer().getSkills());
        final Map<Skill, SkillEntry> newList = new HashMap<Skill, SkillEntry>();
        for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            newList.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
        }
        getPlayer().changeSkillsLevel(newList);
        newList.clear();
        skills.clear();
    }

    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(EtcPacket.showEffect(effect));
        } else {
            c.getSession().write(EtcPacket.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(EtcPacket.playSound(sound));
        } else {
            c.getSession().write(EtcPacket.playSound(sound));
        }
    }

    public void environmentChange(boolean broadcast, String env) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(EtcPacket.environmentChange(env, 2));
        } else {
            c.getSession().write(EtcPacket.environmentChange(env, 2));
        }
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return c.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        if (getPlayer().getParty() == null) {
            return inMap;
        }
        for (MapleCharacter char2 : getPlayer().getMap().getCharactersThreadsafe()) {
            if (char2.getParty() != null && char2.getParty().getId() == getPlayer().getParty().getId()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<MapleCharacter>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            gainMeso(meso);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null) || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public MapleSquad getSquad(String type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public boolean registerSquad(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000, startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();

                map.broadcastMessage(EtcPacket.getClock(minutes * 60));
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad == null) {
                return false;
            }
            if (type_ == 0 || type_ == 3) { // Normal viewing
                sendNext(squad.getSquadMemberString(type_));
            } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
                sendSimple(squad.getSquadMemberString(type_));
            } else if (type_ == 2) {
                if (squad.getBannedMemberSize() > 0) {
                    sendSimple(squad.getSquadMemberString(type_));
                } else {
                    sendNext(squad.getSquadMemberString(type_));
                }
            }
            return true;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return false;
        }
    }

    public byte isSquadLeader(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getLeader() != null && squad.getLeader().getId() == c.getPlayer().getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    public void banMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void acceptMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public int addMember(String type, boolean join) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad != null) {
                return squad.addMember(c.getPlayer(), join);
            }
            return -1;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return -1;
        }
    }

    public byte isSquadMember(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getMembers().contains(c.getPlayer())) {
                return 1;
            } else if (squad.isBanned(c.getPlayer())) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        c.getSession().write(GuildPacket.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0 || c.getPlayer().getGuildRank() != 1) {
            return;
        }
        World.Guild.disbandGuild(gid);
    }

    public void increaseGuildCapacity(boolean trueMax) {
        if (c.getPlayer().getMeso() < 500000 && !trueMax) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "메소가 부족합니다."));
            return;
        }
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        if (World.Guild.increaseGuildCapacity(gid, trueMax)) {
            if (!trueMax) {
                c.getPlayer().gainMeso(-500000, true, true);
            } else {
                gainGP(-25000);
            }
            sendNext("완료되었습니다.");
        } else if (!trueMax) {
            sendNext("늘릴 수 있는 길드원의 양이 가득 찼습니다. (한계: 100)");
        } else {
            sendNext("늘릴 수 있는 길드원의 양이 가득찼거나 GP를 확인해 주세요.(한계: 200)");
        }
    }

    public void displayGuildRanks() {
        c.getSession().write(GuildPacket.showGuildRanks(id, MapleGuildRanking.getInstance().getRank()));
    }

    public boolean removePlayerFromInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            c.getPlayer().getEventInstance().removePlayer(c.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            return true;
        }
        return false;
    }

    public void changeStat(byte slot, int type, int amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr((short) amount);
                break;
            case 1:
                sel.setDex((short) amount);
                break;
            case 2:
                sel.setInt((short) amount);
                break;
            case 3:
                sel.setLuk((short) amount);
                break;
            case 4:
                sel.setHp((short) amount);
                break;
            case 5:
                sel.setMp((short) amount);
                break;
            case 6:
                sel.setWatk((short) amount);
                break;
            case 7:
                sel.setMatk((short) amount);
                break;
            case 8:
                sel.setWdef((short) amount);
                break;
            case 9:
                sel.setMdef((short) amount);
                break;
            case 10:
                sel.setAcc((short) amount);
                break;
            case 11:
                sel.setAvoid((short) amount);
                break;
            case 12:
                sel.setHands((short) amount);
                break;
            case 13:
                sel.setSpeed((short) amount);
                break;
            case 14:
                sel.setJump((short) amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setEnhance((byte) amount);
                break;
            case 19:
                sel.setPotential1(amount);
                break;
            case 20:
                sel.setPotential2(amount);
                break;
            case 21:
                sel.setPotential3(amount);
                break;
            case 22:
                sel.setPotential4(amount);
                break;
            case 23:
                sel.setPotential5(amount);
                break;
            case 24:
                sel.setOwner(getText());
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
        c.getPlayer().fakeRelog();
    }

    public void openDuey() {
        c.getPlayer().setConversation(2);
        c.getSession().write(EtcPacket.sendDuey((byte) 8, null, null));
    }    

    public void openMerchantItemStore() {
        c.getPlayer().setConversation(3);
        HiredMerchantHandler.displayMerch(c);
    }

    public void sendPVPWindow() {
        c.getSession().write(UIPacket.openUI(50));
        c.getSession().write(EtcPacket.sendPVPMaps());
    }

    public void sendRepairWindow() {
        c.getSession().write(UIPacket.sendRepairWindow(id));
    }

    public void sendProfessionWindow() {
        c.getSession().write(UIPacket.openUI(42));
    }

    public final int getDojoPoints() {
        return dojo_getPts();
    }

    public final int getDojoRecord() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO_RECORD);
    }

    public void setDojoRecord(final boolean reset) {
        if (reset) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData("0");
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData("0");
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) + 1));
        }
    }
    
     public int getTorysAllconnection() {//동접
        int count = 0;
        
        
        for (ChannelServer csrv : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : csrv.getPlayerStorage().getAllCharacters()) {
                if (chr != null /*&& !chr.isGM()*/) {
                    count++;
                }
            }
        }
        
        return count;
    }
     
     public void 후원하기(String giftp, String giftc, String dd) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO support (name, type, giftp, giftc, giftt, gifty, accn, accp, data, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, getPlayer().getName());
            ps.setString(2, "상품권");
            ps.setString(3, giftp);
            ps.setString(4, giftc);
            ps.setString(5, "문화상품권");
            ps.setString(6, "");
            ps.setString(7, "");
            ps.setString(8, "");
            ps.setString(9, dd);
            ps.setString(10, "미확인");
            ps.execute();
            return;
        } catch (SQLException localSQLException) {
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception localException4) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception localException5) {
                }
            }
        }
    }

    public void 후원내역() {
        List<SupportData> supportData = new ArrayList();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM support WHERE name = ?");
            ps.setString(1, getPlayer().getName());
            rs = ps.executeQuery();
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                String giftp = rs.getString("giftp");
                String giftc = rs.getString("giftc");
                String giftt = rs.getString("giftt");
                String gifty = rs.getString("gifty");
                String accn = rs.getString("accn");
                String accp = rs.getString("accp");
                String data = rs.getString("data");
                String status = rs.getString("status");
                supportData.add(new SupportData(name, type, giftp, giftc, giftt, gifty, accn, accp, data, status));
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception localException) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception localException1) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception localException2) {
                }
            }
        } catch (SQLException localSQLException) {
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception localException6) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception localException7) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception localException8) {
                }
            }
        }
        String display = "";
        for (SupportData support : supportData) {
            display += "- #d" + support.getDateData() + " #b" + support.getGiftPrice() + "원 #e#r[" + support.getStatus() + "]#k#n\r\n";
        }
        if (display == "") {
            sendNext("[ " + getPlayer().getName() + "님의 후원내역 ]\r\n\r\n" + getPlayer().getName() + "님은 현재 후원내역이 없습니다.");
        } else {
            sendNext("[ " + getPlayer().getName() + "님의 후원내역 ]\r\n\r\n" + display);
        }
    }
    
    class SupportData {

        private String name;
        private String type;
        private String giftp;
        private String giftc;
        private String giftt;
        private String gifty;
        private String accn;
        private String accp;
        private String data;
        private String status;

        public SupportData(String name, String type, String giftp, String giftc, String giftt, String gifty, String accn, String accp, String data, String status) {
            this.name = name;
            this.type = type;
            this.giftp = giftp;
            this.giftc = giftc;
            this.giftt = giftt;
            this.gifty = gifty;
            this.accn = accn;
            this.accp = accp;
            this.data = data;
            this.status = status;
        }

        public final String getName() {
            return this.name;
        }

        public final String getGiftPrice() {
            return this.giftp;
        }

        public final String getGiftCode() {
            return this.giftc;
        }

        public final String getDateData() {
            return this.data;
        }

        public final String getStatus() {
            return this.status;
        }
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party) {
        if (dojo) {
            return Event_DojoAgent.warpStartDojo(c.getPlayer(), party);
        }
        return Event_DojoAgent.warpStartAgent(c.getPlayer(), party);
    }

    public boolean start_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpStartPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpStartSubway(c.getPlayer());
    }

    public boolean bonus_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpBonusPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpBonusSubway(c.getPlayer());
    }

    public final short getKegs() {
        return c.getChannelServer().getFireWorks().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        c.getChannelServer().getFireWorks().giveKegs(c.getPlayer(), kegs);
    }

    public final short getSunshines() {
        return c.getChannelServer().getFireWorks().getSunsPercentage();
    }

    public void addSunshines(final int kegs) {
        c.getChannelServer().getFireWorks().giveSuns(c.getPlayer(), kegs);
    }

    public final short getDecorations() {
        return c.getChannelServer().getFireWorks().getDecsPercentage();
    }

    public void addDecorations(final int kegs) {
        try {
            c.getChannelServer().getFireWorks().giveDecs(c.getPlayer(), kegs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final MapleCarnivalParty getCarnivalParty() {
        return c.getPlayer().getCarnivalParty();
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return c.getPlayer().getNextCarnivalRequest();
    }

    public final MapleCarnivalChallenge getCarnivalChallenge(MapleCharacter chr) {
        return new MapleCarnivalChallenge(chr);
    }

    public void maxStats() {
        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
        c.getPlayer().getStat().str = (short) 32767;
        c.getPlayer().getStat().dex = (short) 32767;
        c.getPlayer().getStat().int_ = (short) 32767;
        c.getPlayer().getStat().luk = (short) 32767;

        int overrDemon = GameConstants.isDemon(c.getPlayer().getJob()) ? GameConstants.getMPByJob(c.getPlayer().getJob()) : 99999;
        c.getPlayer().getStat().maxhp = 99999;
        c.getPlayer().getStat().maxmp = overrDemon;
        c.getPlayer().getStat().setHp(99999, c.getPlayer());
        c.getPlayer().getStat().setMp(overrDemon, c.getPlayer());

        statup.put(MapleStat.STR, Integer.valueOf(32767));
        statup.put(MapleStat.DEX, Integer.valueOf(32767));
        statup.put(MapleStat.LUK, Integer.valueOf(32767));
        statup.put(MapleStat.INT, Integer.valueOf(32767));
        statup.put(MapleStat.HP, Integer.valueOf(99999));
        statup.put(MapleStat.MAXHP, Integer.valueOf(99999));
        statup.put(MapleStat.MP, Integer.valueOf(overrDemon));
        statup.put(MapleStat.MAXMP, Integer.valueOf(overrDemon));
        c.getPlayer().getStat().recalcLocalStats(c.getPlayer());
        c.getSession().write(MaplePacketCreator.updatePlayerStats(statup, c.getPlayer()));
    }

    public Triple<String, Map<Integer, String>, Long> getSpeedRun(String typ) {
        final ExpeditionType type = ExpeditionType.valueOf(typ);
        if (SpeedRunner.getSpeedRunData(type) != null) {
            return SpeedRunner.getSpeedRunData(type);
        }
        return new Triple<String, Map<Integer, String>, Long>("", new HashMap<Integer, String>(), 0L);
    }

    public boolean getSR(Triple<String, Map<Integer, String>, Long> ma, int sel) {
        if (ma.mid.get(sel) == null || ma.mid.get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.mid.get(sel));
        return true;
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
            } else {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
            }
        }
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof Item) {
            final Item it = (Item) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner()) && MapleInventoryManipulator.addFromDrop(getClient(), it, false);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        Item item = getPlayer().getInventory(inv).getItem((byte) slot);
        if (item == null || statsSel instanceof Item) {
            item = (Item) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
                if (eq.getExpiration() == -1) {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.LOCK.getValue()));
                } else {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr() + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex() + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt() + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk() + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp() + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp() + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk() + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk() + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef() + offset));
            } else if (type.equalsIgnoreCase("MDEF")) {
                eq.setMdef((short) (eq.getMdef() + offset));
            } else if (type.equalsIgnoreCase("ACC")) {
                eq.setAcc((short) (eq.getAcc() + offset));
            } else if (type.equalsIgnoreCase("Avoid")) {
                eq.setAvoid((short) (eq.getAvoid() + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands() + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed() + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump() + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration((long) (eq.getExpiration() + offset));
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag((byte) (eq.getFlag() + offset));
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean isCash(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public int getTotalStat(final int itemId) {
        return MapleItemInformationProvider.getInstance().getTotalStat((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
    }

    public int getReqLevel(final int itemId) {
        return MapleItemInformationProvider.getInstance().getReqLevel(itemId);
    }

    public MapleStatEffect getEffect(int buff) {
        return MapleItemInformationProvider.getInstance().getItemEffect(buff);
    }

    public void buffGuild(final int buff, final int duration, final String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            final MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, null, duration);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    public boolean createAlliance(String alliancename) {
        MapleParty pt = c.getPlayer().getParty();
        MapleCharacter otherChar = c.getChannelServer().getPlayerStorage().getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == c.getPlayer().getId()) {
            return false;
        }
        try {
            return World.Alliance.createAlliance(alliancename, c.getPlayer().getId(), otherChar.getId(), c.getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            re.printStackTrace();
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.changeAllianceCapacity(gs.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public boolean disbandAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId() && World.Alliance.disbandAlliance(gs.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
            re.printStackTrace();
        }
        return false;
    }

    public byte getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final byte last) {
        this.lastMsg = last;
    }

    public final void maxAllSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<Skill, SkillEntry>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void maxSkillsByJob() {
        HashMap<Skill, SkillEntry> sa = new HashMap<Skill, SkillEntry>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getPlayer().getJob())) { //no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void resetStats(int str, int dex, int z, int luk) {
        c.getPlayer().resetStats(str, dex, z, luk);
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        return MapleInventoryManipulator.drop(c, inv, (short) slot, (short) quantity, true);
    }

    public final List<Integer> getAllPotentialInfo() {
        List<Integer> list = new ArrayList<Integer>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().keySet());
        Collections.sort(list);
        return list;
    }

    public final List<Integer> getAllPotentialInfoSearch(String content) {
        List<Integer> list = new ArrayList<Integer>();
        for (Entry<Integer, List<StructItemOption>> i : MapleItemInformationProvider.getInstance().getAllPotentialInfo().entrySet()) {
            for (StructItemOption ii : i.getValue()) {
                if (ii.toString().contains(content)) {
                    list.add(i.getKey());
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    public final String getPotentialInfo(final int id) {
        final List<StructItemOption> potInfo = MapleItemInformationProvider.getInstance().getPotentialInfo(id);
        final StringBuilder builder = new StringBuilder("#b#ePOTENTIAL INFO FOR ID: ");
        builder.append(id);
        builder.append("#n#k\r\n\r\n");
        int minLevel = 1, maxLevel = 10;
        for (StructItemOption item : potInfo) {
            builder.append("#eLevels ");
            builder.append(minLevel);
            builder.append("~");
            builder.append(maxLevel);
            builder.append(": #n");
            builder.append(item.toString());
            minLevel += 10;
            maxLevel += 10;
            builder.append("\r\n");
        }
        return builder.toString();
    }

    public final void sendRPS() {
        c.getSession().write(EtcPacket.getRPSMode((byte) 8, -1, -1, -1));
    }

    public final void setQuestRecord(Object ch, final int questid, final String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public void giveBuff(int skill, int level) {
        SkillFactory.getSkill(skill).getEffect(level).applyTo(c.getPlayer());
    }

    public final void TimeMoveMap(final int destination, final int movemap, final int time) {
        warp(movemap, 0);
        getClient().getSession().write(EtcPacket.getClock(time));
        CloneTimer tMan = CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (getPlayer() != null) {
                    if (getPlayer().getMapId() == movemap) {
                        warp(destination);
                        cancelBuff(80001027);
                        cancelBuff(80001028);
                    }
                }
            }
        };
        tMan.schedule(r, time * 1000);
    }

    public final void timeMoveMap(final int destination, final int movemap, final int time) {
        warp(movemap, 0);
        getClient().getSession().write(EtcPacket.getClock(time));
        CloneTimer tMan = CloneTimer.getInstance();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (getPlayer() != null) {
                    if (getPlayer().getMapId() == movemap) {
                        warp(destination);
                        cancelBuff(80001027);
                        cancelBuff(80001028);
                    }
                }
            }
        };
        tMan.schedule(r, time * 1000);
    }

    public void cancelBuff(int skill) {
        c.getPlayer().cancelEffect(SkillFactory.getSkill(skill).getEffect(1), false, -1);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        final MapleCharacter player = getPlayer();
        getMap().broadcastMessage(MaplePacketCreator.yellowChat(player.getName() + " 은(는) 검은머리가 예티의 털처럼 하얗게 변할때까지 " + chr.getName() + " 을(를) 사랑하시겠습니까?"));
        CloneTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (chr == null || player == null) {
                    warpMap(680000500, 0);
                } else {
                    chr.getMap().broadcastMessage(MaplePacketCreator.yellowChat(chr.getName() + " 은(는) 엘나스산의 만년설이 모두 녹아 니할사막처럼 될 때까지 " + player.getName() + " 을(를) 사랑하시겠습니까?"));
                }
            }
        }, 10000);
        CloneTimer.getInstance().schedule(new Runnable() {

            public void run() {
                if (chr == null || player == null) {
                    if (player != null) {
                        setQuestRecord(player, 160001, "3");
                        setQuestRecord(player, 160002, "0");
                    } else if (chr != null) {
                        setQuestRecord(chr, 160001, "3");
                        setQuestRecord(chr, 160002, "0");
                    }
                    warpMap(680000500, 0);
                } else {
                    setQuestRecord(player, 160001, "2");
                    setQuestRecord(chr, 160001, "2");
                    sendNPCText(player.getName() + " 와 " + chr.getName() + ", 지금 이 순간부터 " + chr.getClient().getChannelServer().getServerName() + " 에서의 여행을 같이 즐길 수 있도록 기원합니다!", 9201002);
                    chr.getMap().startExtendedMapEffect("신랑은 신부에게 키스해도 좋습니다.", 5120006);
                    if (chr.getGuildId() > 0) {
                        World.Guild.guildPacket(chr.getGuildId(), MaplePacketCreator.sendMarriage(false, chr.getName()));
                    }
                    if (chr.getFamilyId() > 0) {
                        World.Family.familyPacket(chr.getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), chr.getId());
                    }
                    if (player.getGuildId() > 0) {
                        World.Guild.guildPacket(player.getGuildId(), MaplePacketCreator.sendMarriage(false, player.getName()));
                    }
                    if (player.getFamilyId() > 0) {
                        World.Family.familyPacket(player.getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), player.getId());
                    }
                }
            }
        }, 20000); //10 sec 10 sec

    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().getSession().write(EtcPacket.getKeymap(getPlayer().getKeyLayout()));
    }

    public void logDonator(String log, int previous_points) {
        final StringBuilder logg = new StringBuilder();
        logg.append(MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
        logg.append(" [CID: ").append(getPlayer().getId()).append("] ");
        logg.append(" [Account: ").append(MapleCharacterUtil.makeMapleReadable(getClient().getAccountName())).append("] ");
        logg.append(log);
        logg.append(" [Previous: " + previous_points + "] [Now: " + getPlayer().getPoints() + "]");
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO donorlog VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, MapleCharacterUtil.makeMapleReadable(getClient().getAccountName()));
            ps.setInt(2, getClient().getAccID());
            ps.setString(3, MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
            ps.setInt(4, getPlayer().getId());
            ps.setString(5, log);
            ps.setString(6, FileoutputUtil.CurrentReadable_Time());
            ps.setInt(7, previous_points);
            ps.setInt(8, getPlayer().getPoints());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void doRing(final String name, final int itemid) {
        PlayersHandler.DoRing(getClient(), name, itemid);
    }

    public int getNaturalStats(final int itemid, final String it) {
        Map<String, Integer> eqStats = MapleItemInformationProvider.getInstance().getEquipStats(itemid);
        if (eqStats != null && eqStats.containsKey(it)) {
            return eqStats.get(it);
        }
        return 0;
    }

    public boolean isEligibleName(String t) {
        return MapleCharacterUtil.canCreateChar(t, getPlayer().isGM()) && (!LoginInformationProvider.getInstance().isForbiddenName(t) || getPlayer().isGM());
    }

    public String checkDrop(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId = 0, ch = 0;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (num == 0) {
                        name.append("#o" + mobId + "# 드랍정보\r\n");
                        name.append("--------------------------------------\r\n");
                    }
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * getClient().getChannelServer().getMesoRate()) + " ~ " + (de.Maximum * getClient().getChannelServer().getMesoRate()) + " 메소";
                    }
                    ch = de.chance * getClient().getChannelServer().getDropRate();
                    name.append((num + 1) + ") #v" + itemId + "#" + namez + " - " + (Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0) + "% 확률 " + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? (MapleQuest.getInstance(de.questid).getName() + " 퀘스트 수행중 획득가능") : "") + "\r\n");
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "드롭정보가 없습니다.";
    }

    public List<PokedexEntry> getAllPokedex() {
        return BattleConstants.getAllPokedex();
    }

    public String getLeftPadded(final String in, final char padchar, final int length) {
        return StringUtil.getLeftPaddedStr(in, padchar, length);
    }

    public void preparePokemonBattle(List<Integer> npcTeam, int restrictedLevel) {
        final int theId = MapleLifeFactory.getRandomNPC();
        final PokemonBattle wild = new PokemonBattle(getPlayer(), npcTeam, theId, restrictedLevel);
        getPlayer().changeMap(wild.getMap(), wild.getMap().getPortal(0));
        getPlayer().setBattle(wild);
        wild.initiate(getPlayer(), MapleLifeFactory.getNPC(theId));
    }

    public List<Integer> makeTeam(int lowRange, int highRange, int neededLevel, int restrictedLevel) { //easy = 10 lvls below you to your lvl, normal = 5 lvls below you to 5 lvls above, hard = your lvl to 10 lvls above, hell = bosses that are lower than you
        // easy/norm/hard = min lvl 10, hell = min lvl 100
        final List<Integer> ret = new ArrayList<Integer>();
        int averageLevel = 0, numBattlers = 0;
        for (Battler b : getPlayer().getBattlers()) {
            if (b != null) {
                if (b.getLevel() > averageLevel) {
                    averageLevel = b.getLevel();
                }
                numBattlers++;
            }
        }
        final boolean hell = lowRange == highRange;
        if (numBattlers < 3 || averageLevel < neededLevel) {
            return null;
        }
        if (averageLevel > restrictedLevel) {
            averageLevel = restrictedLevel; //cap it
        }
        final List<PokedexEntry> pokeEntries = new ArrayList<PokedexEntry>(getAllPokedex());
        Collections.shuffle(pokeEntries);
        while (ret.size() < numBattlers) {
            for (PokedexEntry d : pokeEntries) {
                if ((d.dummyBattler.getStats().isBoss() && hell) || (!d.dummyBattler.getStats().isBoss() && !hell)) {
                    if (!hell) {
                        if (d.dummyBattler.getLevel() <= (averageLevel + highRange) && d.dummyBattler.getLevel() >= (averageLevel + lowRange) && Randomizer.nextInt(numBattlers) == 0) {
                            ret.add(d.id);
                            if (ret.size() >= numBattlers) {
                                break;
                            }
                        }
                    } else if (d.dummyBattler.getFamily().type != MobExp.EASY && d.dummyBattler.getLevel() >= neededLevel && d.dummyBattler.getLevel() <= averageLevel && Randomizer.nextInt(numBattlers) == 0) {
                        ret.add(d.id);
                        if (ret.size() >= numBattlers) {
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public BattleConstants.HoldItem[] getAllHoldItems() {
        return BattleConstants.HoldItem.values();
    }

    public void handleDivorce() {
        if (getPlayer().getMarriageId() <= 0) {
            sendNext("Please make sure you have a marriage.");
            return;
        }
        final int chz = World.Find.findChannel(getPlayer().getMarriageId());
        if (chz == -1) {
            //sql queries
        Connection con = null;
        PreparedStatement ps = null;            
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE queststatus SET customData = ? WHERE characterid = ? AND (quest = ? OR quest = ?)");
                ps.setString(1, "0");
                ps.setInt(2, getPlayer().getMarriageId());
                ps.setInt(3, 160001);
                ps.setInt(4, 160002);
                ps.executeUpdate();
                ps.close();

                ps = con.prepareStatement("UPDATE characters SET marriageid = ? WHERE id = ?");
                ps.setInt(1, 0);
                ps.setInt(2, getPlayer().getMarriageId());
                ps.executeUpdate();
            } catch (SQLException e) {
                outputFileError(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
            return;
        } else if (chz < -1) {
            sendNext("Please make sure your partner is logged on.");
            return;
        }
        MapleCharacter cPlayer = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterById(getPlayer().getMarriageId());
        if (cPlayer != null) {
            cPlayer.dropMessage(1, "Your partner has divorced you.");
            cPlayer.setMarriageId(0);
            setQuestRecord(cPlayer, 160001, "0");
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(cPlayer, 160002, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
        } else {
            sendNext("An error occurred...");
        }
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public void sendUltimateExplorer() {
        getClient().getSession().write(MaplePacketCreator.ultimateExplorer());
    }

    public String getJobRanking(int job) {
        String text = "#b:: " + (job == -1 ? "전체" : GameConstants.getJobName(job)) + "직업 랭킹 ::#k\r\n\r\n";
        StringBuilder sb = new StringBuilder();
        final List<RankingWorker.RankingInformation> ranks = RankingWorker.getRankingInfo(job);
        if (ranks == null || ranks.size() <= 0) {
            sb.append("랭킹 정보가 없습니다.");
        } else {
            int num = 0;
            for (RankingWorker.RankingInformation rank : ranks) {
                if (rank.rank >= 1 && rank.rank <= 20) {
                    if (num == 0) {
                        sb.append(text);
                    }
                    sb.append(rank.toString() + "\r\n");
                    num++;
                }
            }
            if (num == 0) {
                sb.append("오류가 발생했습니다. 다시 시도하세요.");
            }
        }
        return sb.toString();
    }

    public String getPokemonRanking() {
        StringBuilder sb = new StringBuilder();
        for (PokemonInformation pi : RankingWorker.getPokemonInfo()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public String getPokemonRanking_Caught() {
        StringBuilder sb = new StringBuilder();
        for (PokedexInformation pi : RankingWorker.getPokemonCaught()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public String getPokemonRanking_Ratio() {
        StringBuilder sb = new StringBuilder();
        for (PokebattleInformation pi : RankingWorker.getPokemonRatio()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public void sendPendant(boolean b) {
        c.getSession().write(MaplePacketCreator.pendantSlot(b));
    }

    public void newCode(String code, String item, String amount) throws SQLException {
        Connection con = null;
        PreparedStatement ins = null;
        try {
        con = DatabaseConnection.getConnection();
        ins = con.prepareStatement("insert into coupons (code,item,amount) values (?,?,?)");
        ins.setString(1, code);
        ins.setString(2, item);
        ins.setString(3, amount);
        ins.executeUpdate();
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void gainSponserItem(int item, final String name, short allstat, short damage, byte upgradeslot) {
        if (GameConstants.isEquip(item)) {
            Equip Item = (Equip) MapleItemInformationProvider.getInstance().getEquipById(item);
            Item.setOwner(name);
            Item.setStr(allstat);
            Item.setDex(allstat);
            Item.setInt(allstat);
            Item.setLuk(allstat);
            Item.setWatk(damage);
            Item.setMatk(damage);
            Item.setUpgradeSlots(upgradeslot);
            MapleInventoryManipulator.addFromDrop(c, Item, false);
        } else {
            gainItem(item, allstat, damage);
        }
    }

    public boolean checkCode(String code) throws SQLException {
        Connection con = null;
        PreparedStatement ins = null;
        try {
        con = DatabaseConnection.getConnection();
        ins = con.prepareStatement("select COUNT(*) from coupons where code = ?");
        ins.setString(1, code);
        ResultSet eq = ins.executeQuery();
        eq.next();
        if (eq.getInt("count(*)") > 0) {
            return true;
        } else {
            return false;
        }
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public String useCoupon(String code) throws SQLException {
        Connection con = null;
        PreparedStatement del = null;
        try {
        String string = new String();
        if (checkCode(code) == true) {
            con = DatabaseConnection.getConnection();
            del = con.prepareStatement("delete from coupons where code = ?");
            del.setString(1, code);
            del.execute();
            string += "쿠폰을 사용하였습니다.";
        } else {
            string += "쿠폰번호가 잘못되었거나, 이미 사용된 쿠폰입니다.";
        }
        return string.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (del != null) {
                try {
                    del.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void couponReward(String code) throws SQLException {
        Connection con = null;
        PreparedStatement ins = null;
        ResultSet eq = null;
        try {

        if (checkCode(code) == true) {
            con = DatabaseConnection.getConnection();
            ins = con.prepareStatement("select*from coupons where code = ?");
            ins.setString(1, code);
            eq = ins.executeQuery();
            while (eq.next()) {
                gainItem(eq.getInt("item"), eq.getShort("amount"));
            }
        }
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
            if (eq != null) {
                try {
                    eq.close();
                } catch (Exception e) {
                }
            }
        }
    }


    public String printItem() throws SQLException {
        Connection con = null;
        PreparedStatement ins = null;
        ResultSet eq = null;
        try {
        con = DatabaseConnection.getConnection();
        ins = con.prepareStatement("select*from coupons");
        eq = ins.executeQuery();
        String string = new String();
        while (eq.next()) {
            string += "지급아이템 : #b#i" + eq.getInt("item") + "##z" + eq.getInt("item") + "##k 갯수 : #b" + eq.getString("amount") + "#k\r\n코드 : " + eq.getString("code") + "\r\n";
        }
        return string.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
            if (eq != null) {
                try {
                    eq.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public String printReward(String code) throws SQLException {
        Connection con = null;
        PreparedStatement ins = null;
        ResultSet eq = null;
        try {
        con = DatabaseConnection.getConnection();
        ins = con.prepareStatement("select*from coupons where code = ?");
        ins.setString(1, code);
        eq = ins.executeQuery();
        String string = new String();
        while (eq.next()) {
            string += "지급아이템 : #b#i" + eq.getInt("item") + "##z" + eq.getInt("item") + "##k 갯수 : #b" + eq.getString("amount") + "#k\r\n코드 : " + eq.getString("code") + "\r\n";
        }
        return string.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (Exception e) {
                }
            }
            if (eq != null) {
                try {
                    eq.close();
                } catch (Exception e) {
                }
            }
        }
    }
    public String levelrank(int limit) throws SQLException {
        Connection con = null;
        PreparedStatement rank = null;
        ResultSet ranking = null;
        try {
        con = DatabaseConnection.getConnection();
        rank = con.prepareStatement("SELECT * FROM characters WHERE gm = 0 ORDER BY level DESC");
        String print = new String();
        String job = new String();
        int i = 1;
        while (i <= limit && ranking.next()) {
            switch (ranking.getInt("job")) {
                case 2001:
                case 2200:
                case 2210:
                case 2211:
                case 2212:
                case 2213:
                case 2214:
                case 2215:
                case 2216:
                case 2217:
                case 2218:
                    job = "에반";
                    break;
                case 3001:
                case 3100:
                case 3110:
                case 3111:
                case 3112:
                    job = "데몬슬레이어";
                    break;
                case 2002:
                case 2300:
                case 2310:
                case 2311:
                case 2312:
                    job = "메르세데스";
                    break;
                case 430:
                case 431:
                case 432:
                case 433:
                case 434:
                    job = "듀얼블레이드";
                    break;
                case 2003:
                case 2400:
                case 2410:
                case 2411:
                case 2412:
                    job = "팬텀";
                    break;
                case 3500:
                case 3510:
                case 3511:
                case 3512:
                    job = "메카닉";
                    break;
                case 1500:
                case 1510:
                case 1511:
                case 1512:
                    job = "스트라이커";
                    break;
                case 1300:
                case 1310:
                case 1311:
                case 1312:
                    job = "윈드브레이커";
                    break;
                case 3300:
                case 3310:
                case 3311:
                case 3312:
                    job = "와일드헌터";
                    break;
                case 100:
                    job = "검사";
                    break;
                case 200:
                    job = "마법사";
                    break;
                case 300:
                    job = "궁수";
                    break;
                case 400:
                    job = "도적";
                    break;
                case 500:
                    job = "해적";
                    break;
                case 110:
                case 111:
                case 112:
                    job = "파이터";
                    break;
                case 120:
                case 121:
                case 122:
                    job = "페이지";
                    break;
                case 130:
                case 131:
                case 132:
                    job = "스피어맨";
                    break;
                case 210:
                case 211:
                case 212:
                    job = "마법사(불, 독)";
                    break;
                case 220:
                case 221:
                case 222:
                    job = "마법사(썬, 콜)";
                    break;
                case 230:
                case 231:
                case 232:
                    job = "클레릭";
                    break;
                case 310:
                case 311:
                case 312:
                    job = "헌터";
                    break;
                case 320:
                case 321:
                case 322:
                    job = "사수";
                    break;
                case 410:
                case 411:
                case 412:
                    job = "어쎄신";
                    break;
                case 420:
                case 421:
                case 422:
                    job = "시프";
                    break;
                case 510:
                case 511:
                case 512:
                    job = "인파이터";
                    break;
                case 520:
                case 521:
                case 522:
                    job = "건슬링거";
                    break;
                case 530:
                case 531:
                case 532:
                    job = "캐논슈터";
                    break;
                case 0:
                case 1000:
                case 2000:
                case 3000:
                    job = "초보자";
                    break;
                default:
                    job = "미확인";
            }
            if (i == 1) {
                print += "#r#e1위#k. " + ranking.getString("name") + " / Lv." + ranking.getInt("level") + " / #b" + job + " #n\r\n";
            } else if (i == 2) {
                print += "#b#e2위#k. " + ranking.getString("name") + " / Lv." + ranking.getInt("level") + " / #b" + job + " #n\r\n";
            } else if (i == 3) {
                print += "#d#e3위#k. " + ranking.getString("name") + " / Lv." + ranking.getInt("level") + " / #b" + job + "#k #n\r\n";
            } else {
                print += i + "위.  " + ranking.getString("name") + " / Lv." + ranking.getInt("level") + " / " + job + " \r\n";
            }
            i++;
        }
        return print.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (rank != null) {
                try {
                    rank.close();
                } catch (Exception e) {
                }
            }
            if (ranking != null) {
                try {
                    ranking.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public void manageItem(String type, int item, int quality) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.gainItem(item, (short) quality);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageMeso(String type, int meso) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.gainMeso(meso, true);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public int manageCheckMeso(String type) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            return squad.getMeso();
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
        return 0;
    }

    public void manageJob(String type, int job) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.changeJob(job);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageLevel(String type, short level) {
        int ch = World.Find.findChannel(type);
        int i;
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            if (squad.getLevel() < level){
            for(i=squad.getLevel(); i < level; i++){
            squad.levelUp();
            }
            }else{
                squad.setLevel(level);
                squad.levelUp();
            }
            if (squad.getExp() < 0) {
                squad.gainExp(-squad.getExp(), false, false, true);
            }
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageMeso2(String type, Long meso) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.gainPMeso(meso);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageSp(String type, int sp) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.gainSP(sp);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageAp(String type, int ap) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.gainAp(ap);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public void manageFame(String type, int fame) {
        int ch = World.Find.findChannel(type);
        final MapleCharacter squad = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(type);
        if (squad != null) {
            squad.setFame(fame);
        } else {
            c.getPlayer().dropMessage(-1, "현재 접속 중인 사람에게만 관리기능을 사용 할 수 있습니다.");
        }
    }

    public Triple<Integer, Integer, Integer> getCompensation() {
        Triple<Integer, Integer, Integer> ret = null;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM compensationlog_confirmed WHERE chrname LIKE ?");
            ps.setString(1, getPlayer().getName());
            rs = ps.executeQuery();
            if (rs.next()) {
                ret = new Triple<Integer, Integer, Integer>(rs.getInt("value"), rs.getInt("taken"), rs.getInt("donor"));
            }

            return ret;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e);
            return ret;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public int getRecord(int num) {
        return c.getPlayer().getIntNoRecord(num);
    }
    
    public void setRecord(int num, String data) {
        c.getPlayer().getQuestNAdd(MapleQuest.getInstance(num)).setCustomData(data);
    }

    public int getRebirth() {
        return c.getPlayer().getIntNoRecord(GameConstants.REBORN_POINT);
    }
    
    public void setRebirth(String data) {
        c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.REBORN_POINT)).setCustomData(data);
    }
    
    public boolean deleteCompensation(int taken) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE compensationlog_confirmed SET taken = ? WHERE chrname LIKE ?");
            ps.setInt(1, taken);
            ps.setString(2, getPlayer().getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e);
            return false;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /*Start of Custom Features*/
    public void gainAPS(int gain) {
        getPlayer().gainAPS(gain);
    }
    /*End of Custom Features*/

    public void sendAndroidStyle(String text, int styles[]) {
        if (lastMsg > -1) {
            return;
        }
        c.getSession().write(NPCPacket.getAndroidTalkStyle(id, text, styles));
        lastMsg = 10;
    }

    public void setAndroidHair(int hair) {
        getPlayer().getAndroid().setHair(hair);
        getPlayer().getAndroid().saveToDb();
        c.getPlayer().setAndroid(c.getPlayer().getAndroid());
    }

    public void setAndroidFace(int face) {
        getPlayer().getAndroid().setFace(face);
        getPlayer().getAndroid().saveToDb();
        c.getPlayer().setAndroid(c.getPlayer().getAndroid());
    }

    public void showDojoRanking() {
        c.getSession().write(InfoPacket.getDojoRanking(DojoRank.getDojoRank()));
    }
}
