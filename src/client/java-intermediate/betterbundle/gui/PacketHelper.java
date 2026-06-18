package betterbundle.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

/**
 * PacketHelper for Minecraft 1.21.5
 * Uses ItemStackHash (modern packet API)
 */
public final class PacketHelper {
    
    private PacketHelper() {}
    
    public static ClickSlotC2SPacket createClickPacket(int syncId, int slot, byte button) {
        return new ClickSlotC2SPacket(
                syncId, -1, (short) slot, button,
                SlotActionType.PICKUP, new Int2ObjectOpenHashMap<>(), ItemStackHash.EMPTY);
    }
}