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
package constants;

import java.util.List;
import java.util.LinkedList;
import server.ServerProperties;



public class ServerConstants {// 아이피 설정
    
    /*
    public static final int expRate = Integer.parseInt(ServerProperties.getProperty("기본경험치"));   //경험치
    
    public static final int expRate1 = Integer.parseInt(ServerProperties.getProperty("경험치1"));   //1~120
    public static final int expRate2 = Integer.parseInt(ServerProperties.getProperty("경험치2"));   //121~199
    public static final int expRate3 = Integer.parseInt(ServerProperties.getProperty("경험치3"));   //200~251
    
    public static final int dropRate = Integer.parseInt(ServerProperties.getProperty("드롭"));   // 드롭
    public static final int mesoRate = Integer.parseInt(ServerProperties.getProperty("메소"));   // 메소
    
    public static final int cashRate = 1;   // 캐시
    public static final int questRate = 6;  // 퀘스트경험치배율
    public static final int userLimit = Integer.parseInt(ServerProperties.getProperty("최대동접")); // 채널당 동접수
    public static final int channelcount = 1; //채널수
    
    public static final String ip = new String(ServerProperties.getProperty("아이피")); // ththaud.codns.com
    public static String host = ip;
    public static String SQL_URL = "jdbc:mysql://localhost:3306/maplestory?autoReconnect=true&characterEncoding=euckr";
    public static String SQL_USER = new String(ServerProperties.getProperty("쿼리계정"));
    public static String SQL_PASSWORD = new String(ServerProperties.getProperty("쿼리암호")); 

    public static final String cshopNpc = new String(ServerProperties.getProperty("캐시샵엔피시")); //캐시샾 엔피시
    public static final String serverMassage = new String(ServerProperties.getProperty("서버메세지")); 
    public static final String serverName = new String(ServerProperties.getProperty("서버이름")); //서버닉네임
    public static final boolean adminonly = false;
    public static final String event = "cpq,cpq2,TamePig,MonsterPark,Boats,Flight,elevator,Trains,Hak,Geenie,Subway,newCharacter,AirPlane,Olivia,PVP,CygnusBattle,ScarTarBattle,VonLeonBattle,Ghost,MV,OrbisPQ,HenesysPQ,Romeo,Juliet,Pirate,Amoria,Ellin,CWKPQ,AutomatedEvent,DollHouse,BossBalrog_EASY,BossBalrog_NORMAL,HorntailBattle,Nibergen,PinkBeanBattle,ZakumBattle,NamelessMagicMonster,Dunas,Dunas2,tokyo_2095,ZakumPQ,LudiPQ,KerningPQ,ProtectTylus,WitchTower_EASY,WitchTower_Med,WitchTower_Hard,Vergamot,ChaosHorntail,ChaosZakum,CoreBlaze,BossQuestEASY,BossQuestMed,BossQuestHARD,BossQuestHELL,Ravana_EASY,Ravana_HARD,Ravana_MED,GuildQuest,Aufhaven,Dragonica,AutoItemMsg";
    
    public static final String eventMessage = new String(ServerProperties.getProperty("채널메세지")); //채널메세지
    public static final String recomMessage = new String(ServerProperties.getProperty("채널추천메세지"));
    public static final int flag = Byte.parseByte(ServerProperties.getProperty("깃발"));
    public static final int maxCharacters = 6; //캐릭터수*/
    public static final int expRate = 30;   //경험치
    
    public static final int expRate1 = 30;   //1~120
    public static final int expRate2 = 20;   //121~199
    public static final int expRate3 = 10;   //200~251
    
    public static final int dropRate = 5;   // 드롭
    public static final int mesoRate = 10;   // 메소
    
    public static final int cashRate = 1;   // 캐시
    public static final int questRate = 15;  // 퀘스트경험치배율
    public static final int userLimit = 100; // 채널당 동접수
    public static final int channelcount = 4; //채널수
    public static String adminIP, adminIP2, adminIP3, adminIP4;
    
    public static final String ip = "127.0.0.1"; // 루프백ip
    public static String host = "127.0.0.1"; // 루프백ip
    public static String SQL_URL = "jdbc:mysql://localhost:3306/sio1114?autoReconnect=true&characterEncoding=euckr";
    public static String SQL_USER = "root";
    public static String SQL_PASSWORD = "root";

    public static final String cshopNpc = "9900000"; //캐시샾 엔피시
    public static final String serverMassage = ""; 
    public static final String serverName = "플레이스타"; //서버닉네임
    public static final boolean adminonly = false;
    public static final String event = "cpq,cpq2,TamePig,MonsterPark,Flight,elevator,Trains,Hak,Geenie,Subway,newCharacter,AirPlane,Olivia,PVP,CygnusBattle,ScarTarBattle,VonLeonBattle,Ghost,MV,OrbisPQ,HenesysPQ,Romeo,Juliet,Pirate,Amoria,Ellin,CWKPQ,AutomatedEvent,DollHouse,BossBalrog_EASY,BossBalrog_NORMAL,HorntailBattle,Nibergen,PinkBeanBattle,ZakumBattle,NamelessMagicMonster,Dunas,Dunas2,tokyo_2095,ZakumPQ,LudiPQ,KerningPQ,ProtectTylus,WitchTower_EASY,WitchTower_Med,WitchTower_Hard,Vergamot,ChaosHorntail,ChaosZakum,CoreBlaze,BossQuestEASY,BossQuestMed,BossQuestHARD,BossQuestHELL,Ravana_EASY,Ravana_HARD,Ravana_MED,GuildQuest,Aufhaven,Dragonica,AutoItemMsg";
    
    public static final String eventMessage = "플레이스타"; //채널메세지
    public static final String recomMessage = "";
    public static final int flag = 3;
    public static final int maxCharacters = 6; //캐릭터수
    
    public static short text = 90;
    public static boolean TESPIA = false; // true = uses GMS test server, for MSEA it does nothing though
    public static final byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 501:
            case 530:
            case 531:
            case 532:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 800:
            case 900:
            case 910:
                return 10;
        }
        return 0;
    }
    // Start of Poll
    public static final boolean PollEnabled = false;
    public static final String Poll_Question = "Are you mudkiz?";
    public static final String[] Poll_Answers = {"test1", "test2", "test3"};
    // End of Poll
    public static final short MAPLE_VERSION = (short) 111;
    public static final String MAPLE_PATCH = "1";
    
    public static boolean Use_Fixed_IV = false; // true = disable sniffing, false = server can connect to itself
    public static boolean Use_Localhost = false; // true = packets are logged, false = others can connect to server
    public static final int MIN_MTS = 100; //lowest amount an item can be, GMS = 110
    public static final int MTS_BASE = 0; //+amount to everything, GMS = 500, MSEA = 1000
    public static final int MTS_TAX = 5; //+% to everything, GMS = 10
    public static final int MTS_MESO = 10000; //mesos needed, GMS = 5000
    public static final int Equipment_Bonus_EXP = 30; // 아이템레벨?
    public static final int PCRoomPercent = 30;
    public static final String Sbname = "";
    public static final String subVersion = "";
    
    

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        DONATOR('!', 1),
        SUPERDONATOR('!', 2),
        INTERN('!', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1),
        POKEMON(2);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
