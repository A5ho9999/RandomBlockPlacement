package de.guntram.mcmod.randomblockplacement;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;

public class RandomBlockPlacement implements ClientModInitializer {
    static final String MODID = "randomblockplacement";
    static final String MODNAME = "RandomBlockPlacement";

    private static RandomBlockPlacement instance;
    private boolean isActive;
    private int minSlot, maxSlot;
    private KeyBinding onOff;

    @Override
    public void onInitializeClient() {
        instance = this;
        setKeyBindings();
        registerCommands();
        isActive = false;
        minSlot = 0;
        maxSlot = PlayerInventory.getHotbarSize() - 1;
    }

    private void setKeyBindings() {
        final String category = "key.categories.randomblockplacement";
        KeyBindingHelper.registerKeyBinding(onOff = new KeyBinding("key.randomblockplacement.toggle", InputUtil.Type.KEYSYM, GLFW_KEY_R, category));
        ClientTickEvents.END_CLIENT_TICK.register(e -> processKeyBinds());
    }

    private void processKeyBinds() {
        if (onOff.wasPressed()) {
            if (isActive) {
                instance.setInactive();
            } else {
                instance.setActive();
            }
        }
    }

    public static RandomBlockPlacement getInstance() {
        return instance;
    }

    public void setInactive() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(Text.translatable("msg.inactive"), false);
        }
        isActive = false;
    }

    public void setActive(int first, int last) {
        minSlot = first - 1;
        maxSlot = last - 1;
        if (maxSlot >= PlayerInventory.getHotbarSize()) {
            maxSlot = PlayerInventory.getHotbarSize() - 1;
        }
        setActive();
    }

    public void setActive() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            player.sendMessage(Text.translatable("msg.active", minSlot + 1, maxSlot + 1), false);
        }
        isActive = true;
    }

    public void onPlayerInteract() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        int index = inventory.selectedSlot;
        if (maxSlot >= PlayerInventory.getHotbarSize()) {
            maxSlot = PlayerInventory.getHotbarSize() - 1;
        }
        if (isActive && index >= minSlot && index <= maxSlot
                && (inventory.getStack(index).getItem() == Items.AIR
                || inventory.getStack(index).getItem() instanceof BlockItem)) {
            int totalBlocks = 0;
            for (int i = minSlot; i <= maxSlot; i++) {
                totalBlocks += getBlockCount(inventory, i);
            }
            int targetCount = (int) (Math.random() * totalBlocks);
            int targetSlot = minSlot;
            while (targetSlot < maxSlot && targetCount >= getBlockCount(inventory, targetSlot)) {
                targetCount -= getBlockCount(inventory, targetSlot);
                targetSlot++;
            }
            inventory.selectedSlot = targetSlot;
        }
    }

    private int getBlockCount(PlayerInventory inventory, int targetSlot) {
        ItemStack stack = inventory.getStack(targetSlot);
        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            return stack.getCount();
        } else {
            return 0;
        }
    }

    public void registerCommands() {
        int size = PlayerInventory.getHotbarSize();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("rblock")
                            .then(
                                    literal("off").executes(c -> {
                                        instance.setInactive();
                                        return 1;
                                    })
                            )
                            .then(
                                    argument("b1", integer(1, size)).then(
                                            argument("b2", integer(1, size)).executes(c -> {
                                                instance.setActive(getInteger(c, "b1"), getInteger(c, "b2"));
                                                return 1;
                                            })
                                    )
                            )
            );
        });
    }
}
