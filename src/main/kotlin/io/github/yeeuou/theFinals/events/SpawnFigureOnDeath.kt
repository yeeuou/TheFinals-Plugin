package io.github.yeeuou.theFinals.events

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import io.github.yeeuou.theFinals.TeamManager
import io.github.yeeuou.theFinals.getTfTeam
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin

class SpawnFigureOnDeath(val pl: JavaPlugin) : Listener {
    @EventHandler
    fun onEntityDeath(ex: EntityDeathEvent) {
        val entity = ex.entity
        if (!entity.scoreboardTags.contains(TeamManager.TAG_JOINED)) return
        val team = entity.getTfTeam() ?: return
        val color = Color.fromRGB(team.color.value())
        entity.location.world.spawn(entity.location, ArmorStand::class.java) {
            it.setBasePlate(false)
            it.isSmall = true
            it.isInvulnerable = true
            it.headRotations.withY(0.0)
            it.setDisabledSlots(
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
            )
            it.addScoreboardTag("tf_figure")
            it.addScoreboardTag("dummyFigure")
            it.setMetadata("tfTeam", FixedMetadataValue(pl, team))
            it.equipment.run {
                lateinit var leatherMeta: LeatherArmorMeta
                boots = ItemStack(Material.LEATHER_BOOTS).apply {
                    val meta = itemMeta as LeatherArmorMeta
                    meta.setColor(color)
                    itemMeta = meta
                    leatherMeta = meta.clone()
                }
                leggings = ItemStack(Material.LEATHER_LEGGINGS).apply {
                    itemMeta = leatherMeta.clone()
                }
                chestplate = ItemStack(Material.LEATHER_CHESTPLATE).apply {
                    itemMeta = leatherMeta.clone()
                }
                helmet = let {
                    if (team.isDummy(entity))
                        ItemStack(Material.LEATHER_HELMET).apply {
                            itemMeta = leatherMeta.clone()
                        }
                    else ItemStack(Material.PLAYER_HEAD).apply {
                        val meta = (itemMeta as SkullMeta)
                        meta.owningPlayer = entity as Player
                        itemMeta = meta
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun figureDestroy(ev: EntityDeathEvent) {
        if (ev.entity.type == EntityType.ARMOR_STAND
            && ev.entity.scoreboardTags.contains("tf_figure"))
            ev.isCancelled = true
    }
}
