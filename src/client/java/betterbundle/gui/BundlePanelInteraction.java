package betterbundle.gui;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import betterbundle.util.BundleContentsComponentHelper;

public final class BundlePanelInteraction {

    private static final int GLFW_MOD_SHIFT = 0x1;

    private BundlePanelInteraction() {}

    private static int gridX(int leftPos) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        return panelX + BundlePanelRenderer.PADDING
                + BundlePanelRenderer.CAT_BAR_WIDTH + 2
                + BundlePanelRenderer.SCROLL_BAR_WIDTH + 2;
    }

    private static int gridY(int topPos) {
        return topPos + BundlePanelRenderer.SEARCH_BAR_HEIGHT + 3 + BundlePanelRenderer.PADDING;
    }

    private static BundlePanelRenderer.FlatItem getClickedItem(double mouseX, double mouseY,
                                                                int leftPos, int topPos) {
        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getBundles();
        if (bundles.isEmpty()) return null;

        List<BundlePanelRenderer.FlatItem> allItems = BundlePanelRenderer.buildFlatItemList(bundles);
        if (allItems.isEmpty()) return null;

        // Use filtered items to match rendered panel
        List<BundlePanelRenderer.FlatItem> items = BundlePanelRenderer.filterItems(allItems, BundlePanelRenderer.searchQuery);
        if (items.isEmpty()) return null;

        int gx = gridX(leftPos);
        int gy = gridY(topPos);

        int relX = (int) mouseX - gx;
        int relY = (int) mouseY - gy;

        int col = relX / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);
        int row = relY / (BundlePanelRenderer.SLOT_SIZE + BundlePanelRenderer.SLOT_SPACING);

        if (col < 0 || col >= BundlePanelRenderer.COLUMNS) return null;
        if (row < 0 || row >= BundlePanelRenderer.VISIBLE_ROWS) return null;

        int flatIndex = (BundlePanelRenderer.getScrollOffset() + row) * BundlePanelRenderer.COLUMNS + col;
        if (flatIndex >= items.size()) return null;
        return items.get(flatIndex);
    }

    public static boolean handlePanelClick(double mouseX, double mouseY, int button, int modifiers,
                                            int leftPos, int topPos,
                                            net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen) {
        BundlePanelRenderer.FlatItem clicked = getClickedItem(mouseX, mouseY, leftPos, topPos);
        if (clicked == null) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return false;

        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        if (connection == null) return false;

        int bundleSlot = clicked.bundleSlot();
        int syncId = player.currentScreenHandler.syncId;
        boolean shiftDown = (modifiers & GLFW_MOD_SHIFT) != 0;

        if (shiftDown) {
            boolean inContainer = !(screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
            int emptySlot;
            if (button == 0 && inContainer) {
                emptySlot = findEmptyContainerSlot(player) >= 0
                        ? findEmptyContainerSlot(player) : findEmptyPlayerSlot(player);
            } else {
                emptySlot = findEmptyPlayerSlot(player);
            }
            if (emptySlot < 0) return true;

            int count = clicked.stack().getCount();
            for (int i = 0; i < count; i++) {
                connection.sendPacket(new BundleItemSelectedC2SPacket(bundleSlot, clicked.itemIndex()));
                connection.sendPacket(makeClickPacket(syncId, bundleSlot, (byte) 1));
            }
            connection.sendPacket(makeClickPacket(syncId, emptySlot, (byte) 0));
        } else {
            connection.sendPacket(new BundleItemSelectedC2SPacket(bundleSlot, clicked.itemIndex()));
            connection.sendPacket(makeClickPacket(syncId, bundleSlot, (byte) 1));
        }

        return true;
    }

    /** Find an empty slot in the open container (not the player inventory). */
    private static int findEmptyContainerSlot(PlayerEntity player) {
        for (net.minecraft.screen.slot.Slot slot : player.currentScreenHandler.slots) {
            if (!slot.hasStack() && slot.inventory != player.getInventory()) {
                return slot.id;
            }
        }
        return -1;
    }

    public static boolean handleSpaceClick(Slot hoveredSlot) {
        if (hoveredSlot == null || !hoveredSlot.hasStack()) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getWindow() == null) return false;

        long window = client.getWindow().getHandle();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) != GLFW.GLFW_PRESS) return false;

        PlayerEntity player = client.player;
        ItemStack stack = hoveredSlot.getStack();
        if (stack.isEmpty() || BundleContentsComponentHelper.isNonEmptyBundle(stack)) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        int targetBundleSlot = -1;
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (BundleContentsComponentHelper.canFitItem(entry.bundleStack(), stack)) {
                targetBundleSlot = entry.bundleSlot();
                break;
            }
        }
        if (targetBundleSlot < 0) return false;

        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        if (connection == null) return false;

        int syncId = player.currentScreenHandler.syncId;
        int itemSlot = hoveredSlot.id;

        connection.sendPacket(makeClickPacket(syncId, itemSlot, (byte) 0));
        connection.sendPacket(makeClickPacket(syncId, targetBundleSlot, (byte) 0));

        return true;
    }

    private static ClickSlotC2SPacket makeClickPacket(int syncId, int slot, byte button) {
        return PacketHelper.createClickPacket(syncId, slot, button);
    }

    private static int findEmptyPlayerSlot(PlayerEntity player) {
        // search main inventory (getSlotIndex 9-35) then hotbar (getSlotIndex 0-8)
        for (int pass = 0; pass < 2; pass++) {
            int min = (pass == 0) ? 9 : 0;
            int max = (pass == 0) ? 36 : 9;
            for (Slot slot : player.currentScreenHandler.slots) {
                if (slot.inventory == player.getInventory() && !slot.hasStack()) {
                    int idx = slot.getIndex();
                    if (idx >= min && idx < max) return slot.id;
                }
            }
        }
        return -1;
    }

    private static long bulkInsertStart = 0;
    private static final long BULK_INSERT_DELAY = 50; // 0.05s

    /** Start the bulk-insert timer (called on space+left-click inside panel with empty cursor). */
    public static void startBulkInsert() {
        bulkInsertStart = System.currentTimeMillis();
    }

    /** Whether the bulk-insert state is active (left button held > 0.05s). */
    public static boolean isBulkInsertActive() {
        return bulkInsertStart > 0 && (System.currentTimeMillis() - bulkInsertStart) >= BULK_INSERT_DELAY;
    }

    /** Exit bulk-insert state. */
    public static void stopBulkInsert() {
        bulkInsertStart = 0;
    }

    /** Put cursor item into any available bundle.
     *  button 0 = left (insert all), 1 = right (insert one). */
    public static boolean handlePanelInsert(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return false;

        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        if (cursor.isEmpty()) return false;

        List<BundlePanelRenderer.BundleSlotEntry> bundles = BundlePanelRenderer.getAllBundles();
        int targetBundleSlot = -1;
        for (BundlePanelRenderer.BundleSlotEntry entry : bundles) {
            if (BundleContentsComponentHelper.canFitItem(entry.bundleStack(), cursor)) {
                targetBundleSlot = entry.bundleSlot();
                break;
            }
        }
        if (targetBundleSlot < 0) return false;

        ClientPlayNetworkHandler connection = client.getNetworkHandler();
        if (connection == null) return false;
        int syncId = player.currentScreenHandler.syncId;
        connection.sendPacket(makeClickPacket(syncId, targetBundleSlot, (byte) button));
        return true;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double scrollDelta,
                                        int leftPos, int topPos, int imageHeight) {
        if (!BundlePanelRenderer.isEffectivelyVisible()) return false;
        if (!isInsidePanel(mouseX, mouseY, leftPos, topPos, imageHeight)) return false;
        BundlePanelRenderer.scrollBy(scrollDelta > 0 ? -1 : 1);
        return true;
    }

    public static boolean isInsidePanel(double mouseX, double mouseY,
                                         int leftPos, int topPos, int imageHeight) {
        int pw = BundlePanelRenderer.panelWidth();
        int panelX = leftPos - pw - 4;
        int gx = gridX(leftPos);
        if (mouseX < gx || mouseX > panelX + pw - BundlePanelRenderer.PADDING) return false;
        int pTop = gridY(topPos);
        int pH = BundlePanelRenderer.VISIBLE_ROWS * BundlePanelRenderer.SLOT_SIZE
                + (BundlePanelRenderer.VISIBLE_ROWS - 1) * BundlePanelRenderer.SLOT_SPACING;
        if (mouseY < pTop || mouseY > pTop + pH) return false;
        return true;
    }
}