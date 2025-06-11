package games.negative.lce.listener;

import games.negative.alumina.util.Tasks;
import games.negative.lce.CombatPlugin;
import games.negative.lce.config.PhysicsConfig;
import games.negative.lce.util.CombatCheck;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener to handle global hit delay removal
 */
public class HitDelayListener implements Listener {

    // A dedicated UUID for the hit delay attribute modifier
    private static final UUID HIT_DELAY_MODIFIER_UUID = UUID.fromString("f31c7628-f176-4ae8-93fc-6c9a2307e3e5");
    
    /**
     * Get the PhysicsConfig for easy access
     */
    private PhysicsConfig physics() {
        return CombatPlugin.configs().physics();
    }
    
    /**
     * Apply attack speed modifier to a player's held item when they join
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!physics().isRemoveHitDelay()) return;
        
        Player player = event.getPlayer();
        if (!CombatCheck.checkCombat(player.getLocation())) return;
        
        // Run on the next tick to ensure the player is fully loaded
        Tasks.run(() -> updatePlayerWeapon(player));
    }
    
    /**
     * Clean up attack speed modifiers when a player leaves
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeAttackSpeedModifier(player);
    }
    
    /**
     * Update attack speed modifier when a player switches items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemSwitch(PlayerItemHeldEvent event) {
        if (!physics().isRemoveHitDelay()) return;
        
        Player player = event.getPlayer();
        if (!CombatCheck.checkCombat(player.getLocation())) return;
        
        // Run on the next tick to ensure the item is properly held
        Tasks.run(() -> updatePlayerWeapon(player));
    }
    
    /**
     * Update attack speed modifier when a player clicks in their inventory
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!physics().isRemoveHitDelay()) return;
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!CombatCheck.checkCombat(player.getLocation())) return;
        
        // Run on the next tick to ensure inventory is updated
        Tasks.run(() -> updatePlayerWeapon(player));
    }
    
    /**
     * Update attack speed modifier when a player picks up an item
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!physics().isRemoveHitDelay()) return;
        
        if (!(event.getEntity() instanceof Player player)) return;
        if (!CombatCheck.checkCombat(player.getLocation())) return;
        
        // Run on the next tick to ensure inventory is updated
        Tasks.run(() -> updatePlayerWeapon(player));
    }
    
    /**
     * Check if the item is a weapon (swords, axes, tridents, etc.)
     */
    private boolean isWeapon(Material material) {
        return material.name().endsWith("_SWORD") || 
               material.name().endsWith("_AXE") || 
               material == Material.TRIDENT;
    }
    
    /**
     * Update the player's weapon with the attack speed modifier
     */
    private void updatePlayerWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // First remove any existing modifier to avoid duplicates
        removeAttackSpeedModifier(player);
        
        // If player is not holding a weapon, we don't need to apply the modifier
        if (item == null || item.getType().isAir() || !isWeapon(item.getType())) return;
        
        // Apply the attack speed modifier directly to the player
        player.getAttribute(Attribute.ATTACK_SPEED).addModifier(
            new AttributeModifier(
                HIT_DELAY_MODIFIER_UUID,
                "LCE No Hit Delay",
                20.0, // Very high value effectively removes cooldown
                AttributeModifier.Operation.ADD_NUMBER
            )
        );
    }
    
    /**
     * Remove the attack speed modifier from the player
     */
    private void removeAttackSpeedModifier(Player player) {
        if (player.getAttribute(Attribute.ATTACK_SPEED) != null) {
            player.getAttribute(Attribute.ATTACK_SPEED)
                .getModifiers()
                .stream()
                .filter(modifier -> modifier.getUniqueId().equals(HIT_DELAY_MODIFIER_UUID))
                .forEach(modifier -> 
                    player.getAttribute(Attribute.ATTACK_SPEED).removeModifier(modifier)
                );
        }
    }
} 