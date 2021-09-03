package ink.ptms.chemdah.core.quest.objective.bukkit

import ink.ptms.chemdah.core.quest.objective.Dependency
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import taboolib.platform.util.attacker

/**
 * Chemdah
 * ink.ptms.chemdah.core.quest.objective.bukkit.IPlayerDeath
 *
 * @author sky
 * @since 2021/3/2 5:09 下午
 */
@Dependency("minecraft")
object IPlayerDeath : AEntityDeath<PlayerDeathEvent>() {

    override val name = "player death"
    override val event = PlayerDeathEvent::class

    init {
        handler {
            entity
        }
        addCondition("weapon") { e ->
            val el = e.entity.lastDamageCause as? EntityDamageByEntityEvent ?: return@addCondition false
            toInferItem().isItem(el.attacker?.equipment?.itemInMainHand ?: AIR)
        }
        addCondition("attacker") { e ->
            val el = e.entity.lastDamageCause as? EntityDamageByEntityEvent ?: return@addCondition false
            toInferEntity().isEntity(el.attacker ?: return@addCondition false)
        }
        addCondition("message") { e ->
            toString() in e.deathMessage.toString()
        }
        addConditionVariable("message") { e ->
            e.deathMessage.toString()
        }
    }
}