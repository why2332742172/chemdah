package ink.ptms.chemdah.module.ui

import ink.ptms.chemdah.api.ChemdahAPI
import ink.ptms.chemdah.core.PlayerProfile
import ink.ptms.chemdah.core.quest.AcceptResult
import ink.ptms.chemdah.core.quest.Template
import ink.ptms.chemdah.core.quest.addon.AddonUI.Companion.ui
import ink.ptms.chemdah.core.quest.meta.MetaLabel.Companion.label
import ink.ptms.chemdah.util.colored
import io.izzel.taboolib.util.item.Items
import org.bukkit.configuration.ConfigurationSection
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Chemdah
 * ink.ptms.chemdah.module.ui.UI
 *
 * @author sky
 * @since 2021/3/11 9:03 上午
 */
class UI(val config: ConfigurationSection) {

    val name = config.getString("name")?.colored().toString()
    val menuQuestRows = config.getInt("menu.quest.rows")
    val menuQuestSlot: List<Int> = config.getIntegerList("menu.quest.slot")
    val menuQuestSlotInfo = config.getInt("menu.quest.methods.info")
    val menuQuestSlotFilter = config.getInt("menu.quest.methods.filter")
    val menuFilterRows = config.getInt("menu.filter.rows")
    val menuFilterSlot: List<Int> = config.getIntegerList("menu.filter.slot")
    val include = ArrayList<Include>()
    val exclude: List<String> = config.getStringList("exclude")
    val items = HashMap<ItemType, Item>()

    val playerFilters = ConcurrentHashMap<UUID, MutableList<String>>()

    init {
        config.getConfigurationSection("include")?.getKeys(false)?.forEach {
            val active = config.getConfigurationSection("include.$it.active") ?: return@forEach
            val normal = config.getConfigurationSection("include.$it.normal") ?: return@forEach
            include.add(Include(it, Items.loadItem(active)!!, Items.loadItem(normal)!!))
        }
        items[ItemType.INFO] = Item(config.getConfigurationSection("item.info")!!)
        items[ItemType.FILTER] = ItemFilter(config.getConfigurationSection("item.filter")!!)
        items[ItemType.QUEST_STARTED] = ItemQuest(config.getConfigurationSection("item.quest.started")!!)
        items[ItemType.QUEST_CAN_START] = ItemQuest(config.getConfigurationSection("item.quest.can-start")!!)
        items[ItemType.QUEST_CANNOT_START] = ItemQuestNoIcon(config.getConfigurationSection("item.quest.cannot-start")!!)
        items[ItemType.QUEST_COMPLETE] = ItemQuestNoIcon(config.getConfigurationSection("item.quest.completed")!!)
        items[ItemType.QUEST_UNAVAILABLE] = ItemQuestNoIcon(config.getConfigurationSection("item.quest.unavailable")!!)
    }

    /**
     * 打开任务页面
     */
    fun open(playerProfile: PlayerProfile) {
        collectQuests(playerProfile).thenAccept { UIMenu(this, playerProfile, it).open() }
    }

    /**
     * 获取所有被收录的有效任务列表
     * 并根据任务状态排序
     */
    fun collectQuests(playerProfile: PlayerProfile): CompletableFuture<List<UITemplate>> {
        val completableFuture = CompletableFuture<List<UITemplate>>()
        // 临时容器
        val collect = ArrayList<UITemplate>()
        // 玩家筛选列表
        val includePlayer = playerFilters.computeIfAbsent(playerProfile.uniqueId) { ArrayList() }
        val include = include.map { it.id }.filter { it in includePlayer || includePlayer.isEmpty() }
        // 筛选任务列表
        val quests = ChemdahAPI.quest.filter { (_, v) -> v.label().any { it in include } && v.label().none { it in exclude } }.values.toList()
        fun process(cur: Int) {
            if (cur < quests.size) {
                val quest = quests[cur]
                val ui = quest.ui()
                // 正在进行该任务
                if (playerProfile.getQuestById(quest.id) != null) {
                    collect.add(UITemplate(quest, ItemType.QUEST_STARTED))
                    process(cur + 1)
                } else {
                    // 任务接受条件判断
                    quest.checkAccept(playerProfile).thenAccept { cond ->
                        // 任务可以接受
                        if (cond == AcceptResult.SUCCESSFUL) {
                            collect.add(UITemplate(quest, ItemType.QUEST_CAN_START))
                        } else {
                            // 任务已完成
                            if (playerProfile.isQuestCompleted(quest.id)) {
                                // 任务允许显示完成状态
                                if (ui?.visibleComplete == true) {
                                    collect.add(UITemplate(quest, ItemType.QUEST_COMPLETE))
                                }
                            } else {
                                collect.add(UITemplate(quest, ItemType.QUEST_CANNOT_START))
                            }
                        }
                        process(cur + 1)
                    }
                }
            } else {
                completableFuture.complete(collect.sortedByDescending { it.itemType.priority })
            }
        }
        process(0)
        return completableFuture
    }
}