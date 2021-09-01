package ink.ptms.chemdah.core.quest.objective.marriagemaster

import at.pcgamingfreaks.MarriageMaster.Bukkit.API.Events.HugEvent
import ink.ptms.chemdah.core.quest.objective.Dependency
import ink.ptms.chemdah.core.quest.objective.ObjectiveCountableI

@Dependency("MarriageMaster")
object MMarriageHug : ObjectiveCountableI<HugEvent>() {

    override val name = "marriage hug"
    override val event =  HugEvent::class

    init {
        handler {
            player.playerOnline
        }
        addCondition("position") {
            toPosition().inside(it.player.playerOnline!!.location)
        }
    }
}