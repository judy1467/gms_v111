/*
 * SpiritProject
 * 팬더 spirit_m@nate.com
 * 이창용 dlckdyd93@nate.com
 * 우비 wellcome__v@nate.com
 * 동민 agneshyo-@nate.com
 * 고영익 rhduddlr6996@nate.com
 * 다루 dev_pay@nate.com
 * 아이유 iureal@nate.com
 * 박종원 dev_app1e@nate.com
 * 이관호 le554400@nate.com
 * 최주원 sch2307@nate.com
 * 이시혁 star_r2@nate.com
 */

package client.inventory;

public enum EquipStats {

    UPGRADE(0x1),
    LEVEL(0x2),
    STR(0x4),
    DEX(0x8),
    INT(0x10),
    LUK(0x20),
    HP(0x40),
    MP(0x80),
    WATK(0x100),
    MATK(0x200),
    WDEF(0x400),
    MDEF(0x800),
    ACC(0x1000),
    AVOID(0x2000),
    HANDS(0x4000),
    SPEED(0x8000),
    JUMP(0x10000),
    FLAG(0x20000),
    ITEMLEVEL(0x80000),
    ITEMEXP(0x100000),
    DURABILITY(0x200000),
    HAMMER(0x400000),
    PVPDAMAGE(0x800000),
    DOWNLEVEL(0x1000000),
    UNKNOWN(0x4000000),
    BOSSDAMAGE(0x40000000),
    IGNOREWDEF(0x80000000),
    ;
    private final int i;

    private EquipStats(int i) {
	this.i = i;
    }

    public int getValue() {
	return i;
    }

    public static final EquipStats getByValue(final int value) {
	for (final EquipStats stat : EquipStats.values()) {
	    if (stat.i == value) {
		return stat;
	    }
	}
	return null;
    }
    
    
}
