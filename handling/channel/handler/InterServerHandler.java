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
package handling.channel.handler;

import client.*;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterIdChannelPair;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.NPCScriptManager;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.packet.EtcPacket;
import tools.Pair;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.MaplePacketCreator;
import tools.packet.MaplePacketCreator.BuddylistPacket;
import tools.packet.MaplePacketCreator.ExpeditionPacket;
import tools.packet.MaplePacketCreator.FamilyPacket;
import tools.packet.MaplePacketCreator.GuildPacket;
import tools.packet.MTSCSPacket;

public class InterServerHandler {

    public static final void EnterCS(final MapleClient c, final MapleCharacter chr, final boolean mts, final boolean npc) {
        c.setCs(true);
        if (!chr.isAlive()) {
            c.getSession().write(EtcPacket.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (npc) {
            if (ChannelServer.cshopNpc != 0) {
                NPCScriptManager.getInstance().start(c, ChannelServer.cshopNpc);
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                c.getPlayer().dropMessage(1, "캐시샵 엔피시가 정해져있지 않습니다.");
            }
            return;
        }

        if (mts && chr.getLevel() < 50) {
            chr.dropMessage(1, "You may not enter the Maple Trading System until level 50.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a minute or less.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final ChannelServer ch = ChannelServer.getInstance(c.getChannel());

        chr.changeRemoval();

        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), mts ? -20 : -10);
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);
        String[] socket = c.getChannelServer().getIP().split(":");
        try {
            c.getSession().write(EtcPacket.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(CashShopServer.getIP().split(":")[1])));
        } catch (UnknownHostException ex) {
            Logger.getLogger(InterServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static final void EnterMTS(final MapleClient c, final MapleCharacter chr) {
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null || c.getChannelServer() == null) {
            chr.dropMessage(1, "지금은 이용하실수없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getLevel() < 9) {
            chr.dropMessage(1, "10레벨 이전에는 트레이드 기능을 사용하실수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getMapId() == GameConstants.STARTMAP) {
            chr.dropMessage(1, "지금은 이용하실수없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        NPCScriptManager.getInstance().start(c, 9900000);
        c.getSession().write(MaplePacketCreator.enableActions());
        //NPCScriptManager.getInstance().start(c, 9900000);
        //c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void Loggedin(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        MapleCharacter player;
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);

        if (transfer == null) { // Player isn't in storage, probably isn't CC
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            Pair<String, String> ip = LoginServer.getLoginAuth(playerid);
            String s = c.getSessionIPAddress();
            if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                if (ip != null) {
                    LoginServer.putLoginAuth(playerid, ip.left, ip.right);
                }
                c.getSession().close();
                return;
            }
            c.setTempIP(ip.right);
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }

        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());

        channelServer.addPlayer(player);

        player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
        player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
        player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));

        c.getSession().write(EtcPacket.getCharInfo(player));
        //c.getSession().write(MaplePacketCreator.getPCRoomCheck((byte)2, 74074032));
        c.getSession().write(MTSCSPacket.enableCSUse());

        /*if (player.isGM()) {
         SkillFactory.getSkill(GameConstants.GMS ? 9101004 : 9001004).getEffect(1).applyTo(player);
         }*/
        c.getSession().write(MaplePacketCreator.temporaryStats_Reset()); // .
        player.getMap().addPlayer(player);

        int 동접 = 0; // 팁 수정
        for (ChannelServer chn : ChannelServer.getAllInstances()) {
        동접 += chn.getPlayerStorage().getConnectedClients();
        }
        c.getSession().write(MaplePacketCreator.yellowChat("[TIP] @명령어를 입력시 플레이어 도움말이 출력됩니다!"));
        c.getSession().write(MaplePacketCreator.yellowChat("[TIP] @저장 명령어를 자주 사용 하시기바랍니다. 서버의 모든 데이터가 저장 됩니다."));
        c.getSession().write(MaplePacketCreator.yellowChat("[TIP] 플레이스타 경험치:7배 드롭율:3배 메소율:3배"));
        c.getSession().write(MaplePacketCreator.yellowChat("[알림] 현재 "+ ServerConstants.serverName +" 서버내 "+ 동접 +"명이 플레이 중입니다."));
        c.getSession().write(MTSCSPacket.enableCSUse());
        
        try {
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));

                if (party != null && party.getExpeditionId() > 0) {
                    final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.getSession().write(ExpeditionPacket.expeditionStatus(me, false));
                    }
                }
            }

            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.getSession().write(BuddylistPacket.updateBuddylist(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(GuildPacket.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { //guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }

            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyData());
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Login_Error, e);
        }
        player.getClient().getSession().write(MaplePacketCreator.serverMessage(channelServer.getServerMessage()));
        player.sendMacros();
        player.showNote();
        player.sendImp();
        player.updatePartyMemberHP();
        //player.startFairySchedule(false);
        player.baseSkills(); //fix people who've lost skills.
        c.getSession().write(MaplePacketCreator.getFamiliarInfo(player));   
        c.getSession().write(EtcPacket.getKeymap(player.getKeyLayout()));
        player.spawnSavedPets();
        //player.setPetAutoHP(player.getPetAutoHP());     
        //player.setPetAutoMP(player.getPetAutoMP());      
        player.expirationTask(true, transfer == null);
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        player.LoginCoin();

/*        if (player.getPets().get(0).getPetItemId() == 5000130 && player.getPets().get(1).getPetItemId() == 5000131 && player.getPets().get(2).getPetItemId() == 5000132) {
            player.dropMessage(6, "마족 메투스 세트 장착으로 인해 버프가 발동됩니다.");
            final Map<Skill, SkillEntry> sData = new HashMap<Skill, SkillEntry>();
            sData.put(SkillFactory.getSkill(80001131), new SkillEntry((byte) 1, (byte) 1, -1));
            player.changeSkillLevel_Skip(sData, allowLogin);
        }*/
        /*Calendar cl =Calendar.getInstance();
         int nowHour = cl.get(Calendar.HOUR_OF_DAY);
         if ((nowHour >=  1 && nowHour < 2) || (nowHour >=  17 && nowHour < 18)  || (nowHour >=  22 && nowHour< 23)) {
         player.dropMessage(1, "알림" + nowHour + " ~ " + (nowHour + 1) +"시까지 진행되는 버닝 타임 이벤트가 진행중입니다.");
         player.GmText(8, "[이벤트] " + nowHour + " ~ " + (nowHour + 1) +"시까지 진행되는 버닝 타임 이벤트가 진행중입니다. 사실 구라임");
         if (player.getClient().getChannelServer().isBurning()) {
         for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
         //ChannelServer.getInstance(i).setBurning(true);
         }
         } else {
         for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
         ////ChannelServer.getInstance(i).setBurning(false);
         } 
         }
         }*/
        /*if (c.getPlayer().getJob() == 434 || c.getPlayer().getJob() == 2112 || c.getPlayer().getJob() == 3312) {
         c.getPlayer().maxSkillJob();
         player.dropMessage(-1, "와일드 헌터, 아란, 듀얼 블레이드는 현재 오류로 인해 모든스킬이 마스터됩니다.");
         }*/
        Skill skil;
        Map<Skill, SkillEntry> list = new HashMap<Skill, SkillEntry>();
        final int[] ss50 = {30011024, 30001024, 20021024, 20011024, 20000024, 10000018, 8};
        for (int i : ss50) {
            skil = SkillFactory.getSkill(i);
            if (skil != null) {
               System.out.println("Skill is not NULL. Skill ID: "+skil.getId()+" Skill Name: "+skil.getName());
                if (c.getPlayer().getSkillLevel(skil) <= 0 && skil.canBeLearnedBy(player.getJob())) { // no total
                    list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        }
        if (!list.isEmpty()) {
            player.changeSkillsLevel(list);
            player.dropMessage(-1, "멀티펫 스킬이 지급되었습니다. 이제부터 멀티펫 사용이 가능합니다.");
        }
        if (player.getPC() - System.currentTimeMillis() > 0) {
            player.dropMessage(5, "PC방 정량제 잔여시간이 " + ((player.getPC() - System.currentTimeMillis()) / 60000) + "분 남았습니다. 잔여시간이 모두 소진될 경우 특수효과가 종료됩니다.");
        }
       // if (player.isElf()){
        // player.changeElf();
        // player.changeElf();
        // }
        player.GmText(1, " ");
        player.spawnClones();
        
        if (player.getStat().equippedSummon > 0) {
            SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player);
        }
        if (player.getInventory(MapleInventoryType.EQUIPPED).findById(1122121) != null) { //오토루팅 아이템 코드
            if (player.getAutoLoot() == false) {
                player.setAutoLoot(true);
            }
           player.dropMessage(6, "오토루팅 아이템 착용으로 오토루팅이 활성화됩니다.");
        }
        if (player.getInventory(MapleInventoryType.EQUIPPED).findById(1122121) != null) {
            player.equipPendantOfSpirit();
        }
        MapleQuestStatus stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        if(stat != null && player.pendants==true){
        c.getSession().write(MaplePacketCreator.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        player.pendants=false;
        }
        c.getSession().write(MaplePacketCreator.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        stat = player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT));
        c.getSession().write(EtcPacket.quickSlot(stat != null && stat.getCustomData() != null ? stat.getCustomData() : null));
        player.updatePetAuto();
        player.setPetAutoHP(player.getPetAutoHP());     
        player.setPetAutoMP(player.getPetAutoMP());   
        player.saveToDB(true,true);  
    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean room) {
        if (chr == null || chr.hasBlockedInventory() || chr.getEventInstance() != null || chr.getMap() == null || chr.isInBlockedMap() || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getMapId() == chr.CMap) {
            chr.dropMessage(1, "이 곳에선 채널을 변경할수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        } 

    if (World.getPendingCharacterSize () 
        >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a less than a minute.");
        c.getSession().write(MaplePacketCreator.enableActions());
        return;
    }
    final int chc = slea.readByte() + 1;
    int mapid = 0;
    if (room

    
        ) {
            mapid = slea.readInt();
    }

    chr.updateTick (slea.readInt

    ());
        if (!World.isChannelAvailable (chc) 
        ) {
            chr.dropMessage(1, "The channel is full at the moment.");
        c.getSession().write(MaplePacketCreator.enableActions());
        return;
    }
    if (room && (mapid< 910000001 || mapid

    
        > 910000022)) {
            chr.dropMessage(1, "The channel is full at the moment.");
        c.getSession().write(MaplePacketCreator.enableActions());
        return;
    }
    if (room

    
        ) {
            if (chr.getMapId() == mapid) {
            if (c.getChannel() == chc) {
                chr.dropMessage(1, "You are already in " + chr.getMap().getMapName());
                c.getSession().write(MaplePacketCreator.enableActions());
            } else { // diff channel
                chr.changeChannel(chc);
            }
        } else { // diff map
            if (c.getChannel() != chc) {
                chr.changeChannel(chc);
            }
            final MapleMap warpz = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
            if (warpz != null) {
                chr.changeMap(warpz, warpz.getPortal("out00"));
            } else {
                chr.dropMessage(1, "The channel is full at the moment.");
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        }
    }

    
        else {
        MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        if(stat != null && chr.pendants==true){
        c.getSession().write(MaplePacketCreator.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        chr.pendants=false;
        }
            chr.changeChannel(chc);
    }
}
}
