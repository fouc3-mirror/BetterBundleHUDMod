package betterbundle.gui;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.registry.Registries;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.BundleContentsComponent;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import betterbundle.util.BundleContentsComponentHelper;
import betterbundle.config.BetterBundleConfig;

public final class BundlePanelRenderer {

    public static final int SLOT_SIZE = 18;
    public static final int COLUMNS = 6;
    public static final int VISIBLE_ROWS = 8;
    public static final int SLOT_SPACING = 1;
    public static final int PADDING = 3;
    public static final int SCROLL_BAR_WIDTH = 4;
    public static final int CAT_BUTTON_SIZE = 23;
    public static final int CAT_BAR_WIDTH = CAT_BUTTON_SIZE;
    public static final int SEARCH_BAR_HEIGHT = 14;

    private static int scrollOffset = 0;
    public static boolean visible = true;

    public static String searchQuery = "";
    public static boolean searchFocused = false;
    private static int searchCursorTick = 0;
    private static int hoveredBundleSlot = -1;

    public static BundleCategory currentCategory = BundleCategory.ALL;

    private BundlePanelRenderer() {}

    /** bundleSlot = container slot index (for clickSlot packets) */
    public record BundleSlotEntry(int bundleSlot, ItemStack bundleStack, BundleContentsComponent contents) {}

    public static int panelWidth() {
        return CAT_BAR_WIDTH + 2 + SCROLL_BAR_WIDTH + 2
                + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING + PADDING * 2;
    }

    public static int getScrollOffset() { return scrollOffset; }
    public static void scrollToTop() { scrollOffset = 0; }

    public static void scrollBy(int delta) {
        List<BundleSlotEntry> bundles = getBundles();
        if (bundles.isEmpty()) { scrollOffset = 0; return; }
        List<FlatItem> items = buildFlatItemList(bundles);
        int totalRows = (items.size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollOffset = Math.clamp(scrollOffset + delta, 0, maxScroll);
    }

    public record FlatItem(int bundleSlot, int itemIndex, ItemStack stack) {}

    public static List<FlatItem> buildFlatItemList(List<BundleSlotEntry> bundles) {
        List<FlatItem> result = new ArrayList<>();
        for (BundleSlotEntry entry : bundles) {
            if (entry.contents() == null) continue;
            List<ItemStack> items = entry.contents().stream().map(ItemStack::copy).toList();
            for (int i = 0; i < items.size(); i++) {
                result.add(new FlatItem(entry.bundleSlot(), i, items.get(i)));
            }
        }
        return result;
    }

    public static List<FlatItem> filterItems(List<FlatItem> items, String query) {
        List<FlatItem> filtered = new ArrayList<>();
        for (FlatItem fi : items) {
            String key = Registries.ITEM.getId(fi.stack().getItem()).toString();
            if (currentCategory.matches(key)) filtered.add(fi);
        }
        if (query.isEmpty()) return filtered;
        String q = query.toLowerCase(Locale.ROOT);
        List<FlatItem> sorted = new ArrayList<>(filtered);
        sorted.sort(Comparator.comparing((FlatItem fi) -> matchesSearch(fi, q) ? 0 : 1));
        return sorted;
    }

    private static boolean matchesSearch(FlatItem fi, String q) {
        String name = fi.stack().getName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(q)) return true;
        if (toPinyin(name).contains(q)) return true;
        var key = Registries.ITEM.getId(fi.stack().getItem());
        String fullId = key.toString().toLowerCase(Locale.ROOT);
        String path = key.getPath().toLowerCase(Locale.ROOT);
        return fullId.contains(q) || path.contains(q);
    }

    private static String toPinyin(String text) {
        try {
            HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
            fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            StringBuilder sb = new StringBuilder();
            for (char c : text.toCharArray()) {
                String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                if (arr != null && arr.length > 0) sb.append(arr[0]);
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            return "";
        }
    }

    public static List<BundleSlotEntry> getBundles() { return findBundles(false); }
    public static List<BundleSlotEntry> getAllBundles() { return findBundles(true); }

    private static List<BundleSlotEntry> findBundles(boolean includeEmpty) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return List.of();
        List<BundleSlotEntry> result = new ArrayList<>();
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            boolean matches = includeEmpty
                    ? BundleContentsComponentHelper.isBundle(stack)
                    : BundleContentsComponentHelper.isNonEmptyBundle(stack);
            if (matches) {
                // Convert inventory index to container slot index
                int containerSlot = findContainerSlot(player, inv, i);
                result.add(new BundleSlotEntry(containerSlot, stack, BundleContentsComponentHelper.getContents(stack)));
            }
        }
        return result;
    }

    /** Convert player inventory index (0-35) to container menu slot index. */
    private static int findContainerSlot(PlayerEntity player, PlayerInventory inv, int inventoryIndex) {
        for (net.minecraft.screen.slot.Slot slot : player.currentScreenHandler.slots) {
            if (slot.inventory == inv && slot.getIndex() == inventoryIndex) {
                return slot.id;
            }
        }
        return inventoryIndex; // fallback
    }

    public static boolean isRecipeBookOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof RecipeBookScreen<?> screen) return screen.recipeBook.isOpen();
        return false;
    }

    public static int getHoveredBundleSlot() { return visible ? hoveredBundleSlot : -1; }
    public static boolean isEffectivelyVisible() { return visible && !isRecipeBookOpen(); }
    public static void toggleVisible() { visible = !visible; }

    // --- category button layout ---

    /** Shared button layout: returns Y position of category button i. */
    private static int catButtonY(int i, int panelY) {
        return panelY + PADDING - 3 + i * CAT_BAR_WIDTH;
    }

    public static BundleCategory getCategoryAt(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int baseCatX = panelX + PADDING - 10;
        int panelY = topPos;
        int searchH = SEARCH_BAR_HEIGHT + 3;
        int gridH = PADDING * 2 + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING;
        int panelHeight = Math.min(imageHeight, searchH + gridH) + 24;

        BundleCategory[] cats = BundleCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int by = catButtonY(i, panelY);
            if (by + CAT_BAR_WIDTH > panelY + panelHeight) break;
            int bx = baseCatX;
            int bw = CAT_BAR_WIDTH;
            if (cats[i] == currentCategory) { bx -= 5; bw += 5; }
            if (mouseX >= bx && mouseX < bx + bw
                    && mouseY >= by && mouseY < by + CAT_BAR_WIDTH) {
                return cats[i];
            }
        }
        return null;
    }

    // --- search ---

    public static boolean isInsideSearchBar(double mouseX, double mouseY, int leftPos, int topPos, int imageHeight) {
        if (currentCategory != BundleCategory.ALL) return false; // Only ALL mode has interactive search
        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int sbx = panelX + PADDING + CAT_BAR_WIDTH + 2;
        int sby = topPos + 2;
        int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
        return mouseX >= sbx && mouseX <= sbx + sbw && mouseY >= sby && mouseY <= sby + SEARCH_BAR_HEIGHT;
    }

    public static void onCharTyped(char c) {
        if (!searchFocused || currentCategory != BundleCategory.ALL) return;
        if (c >= 32 && c != 127) { searchQuery += c; scrollOffset = 0; }
    }

    public static void onSearchKeyPress(int key) {
        if (!searchFocused || currentCategory != BundleCategory.ALL) return;
        if (key == 259) {
            if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); scrollOffset = 0; }
        } else if (key == 256) {
            searchQuery = ""; searchFocused = false; scrollOffset = 0;
        } else if (key == 257 || key == 335) {
            searchFocused = false;
        }
    }

    // --- render ---

    public static void render(DrawContext graphics, int leftPos, int topPos, int imageHeight, int mouseX, int mouseY) {
        if (!isEffectivelyVisible()) return;
        List<BundleSlotEntry> bundles = getBundles();
        if (bundles.isEmpty()) { scrollOffset = 0; return; }

        List<FlatItem> allItems = buildFlatItemList(bundles);
        if (allItems.isEmpty()) { scrollOffset = 0; return; }

        List<FlatItem> items = filterItems(allItems, searchQuery);
        if (items.isEmpty()) scrollOffset = 0;

        int pw = panelWidth();
        int panelX = leftPos - pw - 4;
        int panelY = topPos;

        boolean isAllMode = currentCategory == BundleCategory.ALL;
        int searchH = SEARCH_BAR_HEIGHT + 3;

        int gridH = PADDING * 2 + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING;
        int panelHeight = Math.min(imageHeight, searchH + gridH) + 24;

        // Panel background (left edge inset 16px)
        graphics.fill(panelX + 16, panelY, panelX + pw, panelY + panelHeight, 0xC0101010);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        int totalRows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        // Category buttons always at panel top (no searchH offset)
        int catTop = panelY;
        // Grid starts below search bar
        int gridTop = panelY + searchH;
        int gridContentH = panelHeight - searchH;

        // Category buttons
        BundleCategory[] cats = BundleCategory.values();
        int catX = panelX + PADDING - 10;
        int catAreaH = panelHeight - PADDING * 2;

        for (int i = 0; i < cats.length; i++) {
            int by = catButtonY(i, catTop);
            if (by + CAT_BAR_WIDTH > catTop + panelHeight) break;

            boolean selected = cats[i] == currentCategory;
            int bx = catX;
            int bw = CAT_BAR_WIDTH;
            if (selected) { bx -= 5; bw += 5; }
            boolean hovered = mouseX >= bx && mouseX < bx + bw
                    && mouseY >= by && mouseY < by + CAT_BAR_WIDTH;
            int bg = selected ? 0xC0101010 : (hovered ? 0xFF555555 : 0xFF2D2D2D);
            graphics.fill(bx, by, bx + bw, by + CAT_BAR_WIDTH, bg);
            int iconOff = (CAT_BAR_WIDTH - 16) / 2;
            graphics.drawItem(cats[i].getIcon(), bx + iconOff, by + iconOff);
        }

        // Scroll bar
        int sbX = panelX + PADDING + CAT_BAR_WIDTH + 2;
        int sbY = gridTop + PADDING;
        int sbH = gridContentH - PADDING * 2;

        graphics.fill(sbX, sbY, sbX + SCROLL_BAR_WIDTH, sbY + sbH, 0xFF2D2D2D);
        if (maxScroll > 0) {
            int thumbH = Math.max(12, sbH * VISIBLE_ROWS / totalRows);
            int thumbY = sbY + (sbH - thumbH) * scrollOffset / maxScroll;
            graphics.fill(sbX, thumbY, sbX + SCROLL_BAR_WIDTH, thumbY + thumbH, 0xFF888888);
        }

        // Item grid - first pass: draw items
        int gridX = sbX + SCROLL_BAR_WIDTH + 2;
        int gridY = gridTop + PADDING;
        int startRow = scrollOffset;
        int hoveredFlatIndex = -1;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int flatIndex = (startRow + row) * COLUMNS + col;
                if (flatIndex >= items.size()) break;
                int sx = gridX + col * (SLOT_SIZE + SLOT_SPACING);
                int sy = gridY + row * (SLOT_SIZE + SLOT_SPACING);

                graphics.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF373737);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xFFC6C6C6);

                FlatItem fi = items.get(flatIndex);
                ItemStack stack = fi.stack();
                graphics.drawItem(stack, sx + 1, sy + 1);
                // Draw stack overlay (includes count and durability bar) based on config
                if (BetterBundleConfig.getInstance().showStackOverlay()) {
                    graphics.drawStackOverlay(font, stack, sx + 1, sy + 1);
                }

                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                    hoveredFlatIndex = flatIndex;
                }
            }
        }

        // Item grid - second pass: draw item counts (after all items are drawn)
        // Note: We need to manually draw the count overlay since DrawContext.drawItem doesn't include it
        // The count is drawn using drawText with shadow for visibility

        if (hoveredFlatIndex >= 0) {
            int hRow = hoveredFlatIndex / COLUMNS - startRow;
            int hCol = hoveredFlatIndex % COLUMNS;
            int hx = gridX + hCol * (SLOT_SIZE + SLOT_SPACING);
            int hy = gridY + hRow * (SLOT_SIZE + SLOT_SPACING);
            graphics.fill(hx, hy, hx + SLOT_SIZE, hy + SLOT_SIZE, 0x80FFFFFF);
            graphics.drawItemTooltip(client.textRenderer, items.get(hoveredFlatIndex).stack(), mouseX, mouseY);
            hoveredBundleSlot = items.get(hoveredFlatIndex).bundleSlot();
        } else {
            hoveredBundleSlot = -1;
        }

        // Search bar (always visible, only interactive in ALL mode)
        {
            int sbx = panelX + PADDING + CAT_BAR_WIDTH + 2;
            int sby = panelY + 2;
            int sbw = pw - PADDING - CAT_BAR_WIDTH - 2 - PADDING - 10;
            boolean active = isAllMode && searchFocused;
            int bg = isAllMode ? (active ? 0xFF000000 : 0xFF2D2D2D) : 0xFF1A1A1A;
            graphics.fill(sbx, sby, sbx + sbw, sby + SEARCH_BAR_HEIGHT, bg);
            if (active) graphics.fill(sbx + 1, sby + 1, sbx + sbw - 1, sby + SEARCH_BAR_HEIGHT - 1, 0xFF3D3D3D);
            int textY = sby + (SEARCH_BAR_HEIGHT - font.fontHeight) / 2;
            if (isAllMode && searchQuery.isEmpty() && !searchFocused) {
                graphics.drawText(font, "Search...", sbx + 3, textY, 0xFF666666, false);
            } else if (isAllMode && !searchQuery.isEmpty()) {
                graphics.drawText(font, searchQuery, sbx + 3, textY, 0xFFFFFFFF, false);
                searchCursorTick = (searchCursorTick + 1) % 40;
                if (searchFocused && searchCursorTick < 20) {
                    int cursorX = sbx + 3 + font.getWidth(searchQuery);
                    graphics.fill(cursorX, textY, cursorX + 1, textY + font.fontHeight, 0xFFFFFFFF);
                }
            }
        }

        // Category title (on top of search bar)
        if (currentCategory != BundleCategory.ALL) {
            String label = currentCategory.getDisplayName();
            graphics.drawText(font, label, panelX + 16 + 3, panelY + 2, 0xFFCCCCCC, false);
        }

        // Bundle count display (bottom-right of grid)
        int[] stats = getBundleStats();
        String countText = stats[0] + "/" + stats[1];
        int textW = font.getWidth(countText);
        int countX = gridX + COLUMNS * (SLOT_SIZE + SLOT_SPACING) - SLOT_SPACING - textW;
        int countY = gridY + VISIBLE_ROWS * SLOT_SIZE + (VISIBLE_ROWS - 1) * SLOT_SPACING + 7;
        graphics.fill(countX - 2, countY, countX + textW + 2, countY + font.fontHeight, 0xC0101010);
        graphics.drawText(font, countText, countX, countY, 0xFFAAAAAA, false);

    }

    private static int[] getBundleStats() {
        List<BundleSlotEntry> all = getAllBundles();
        int totalItems = 0;
        Fraction totalWeight = Fraction.ZERO;
        for (BundleSlotEntry entry : all) {
            BundleContentsComponent c = entry.contents();
            if (c != null && !c.isEmpty()) {
                totalItems += c.stream().map(ItemStack::copy).mapToInt(ItemStack::getCount).sum();
                totalWeight = totalWeight.add(c.getOccupancy());
            }
        }
        // remaining weight → how many more "standard" items (weight 1/64) would fit
        Fraction maxWeight = Fraction.getFraction(all.size(), 1);
        Fraction remaining = maxWeight.subtract(totalWeight);
        int effectiveMax = totalItems + remaining.multiplyBy(Fraction.getFraction(64, 1)).intValue();
        return new int[] { totalItems, effectiveMax };
    }
}