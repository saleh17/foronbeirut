package com.foronbeirut.manakich.engine

import kotlin.math.pow

/**
 * The shop.
 *
 * Every entry has to answer one question: which of the seven taps does it delete,
 * or which ceiling does it raise? Labour upgrades make the same work easier;
 * ceiling upgrades make the same work worth more. Alternating them is what keeps
 * a tree from going boring (labour only) or exhausting (ceiling only).
 */
public enum class Upgrade(
    public val label: String,
    public val arabic: String,
    public val blurb: String,
    public val costs: List<Int>,
) {
    /**
     * Bake time down AND the window up, always. A faster oven that did not widen
     * the window would be a stealth difficulty increase the player pays for and
     * cannot articulate — so the two move together, by design, at every level.
     */
    OVEN(
        "Better furn", "فرن أفضل",
        "Bakes faster and holds the window open longer",
        listOf(350, 900, 2200, 5000),
    ),
    PEEL(
        "Wider peel", "رفش أعرض",
        "Carry more into the furn in one go",
        listOf(500, 1400),
    ),
    BURN_GUARD(
        "Burn guard", "حارس الحرق",
        "Holds at sellable for longer before it turns",
        listOf(600, 1800, 4500),
    ),
    INGREDIENTS(
        "Better ingredients", "مونة أفخر",
        "Stone-ground zaatar and fresh akkawi — everything is worth more",
        listOf(700, 1900, 4800),
    ),
    AWNING(
        "Awning and chairs", "تنده وكراسي",
        "Somewhere to stand out of the sun, so they wait longer",
        listOf(450, 1200, 3000),
    ),
    /**
     * Sold last and expensive on purpose. Manual collection is the best pressure
     * in the game; the moment it goes the game relaxes permanently, so it has to
     * read as the reward for mastery rather than an early convenience.
     */
    TIP_JAR(
        "Tip jar", "صندوق البقشيش",
        "Coins find their own way into the till",
        listOf(7000, 15000),
    );

    public val maxLevel: Int get() = costs.size
}

/** What the player owns. Levels are 0-based: 0 means the starting equipment. */
public data class Upgrades(private val levels: Map<Upgrade, Int> = emptyMap()) {

    public fun level(upgrade: Upgrade): Int = (levels[upgrade] ?: 0).coerceIn(0, upgrade.maxLevel)

    public fun isMaxed(upgrade: Upgrade): Boolean = level(upgrade) >= upgrade.maxLevel

    /** What the next level costs, or null when there is nothing left to buy. */
    public fun priceOf(upgrade: Upgrade): Int? =
        if (isMaxed(upgrade)) null else upgrade.costs[level(upgrade)]

    public fun bought(upgrade: Upgrade): Upgrades =
        if (isMaxed(upgrade)) this else Upgrades(levels + (upgrade to level(upgrade) + 1))

    /**
     * The whole point of keeping tuning in [GameParams]: upgrades compile into a
     * snapshot of it, and the engine never learns that upgrades exist.
     */
    public fun compile(day: Int): GameParams {
        val oven = level(Upgrade.OVEN)
        val bake = listOf(6.0, 5.2, 4.4, 3.6, 3.0)[oven]
        val window = listOf(1.4, 1.4, 1.5, 1.6, 1.8)[oven]
        val grace = listOf(0.8, 0.9, 1.1, 1.4, 2.0)[oven] + 0.35 * level(Upgrade.BURN_GUARD)
        val richer = 1.15.pow(level(Upgrade.INGREDIENTS))

        // Jibneh keeps its character at every oven level: akkawi always wants a
        // quarter longer than zaatar, and is always a little more forgiving.
        val recipes = mapOf(
            Topping.ZAATAR to Recipe(bake, window, grace, price = (8 * richer).toInt()),
            Topping.JIBNEH to Recipe(bake * 1.25, window * 1.07, grace * 1.375, price = (14 * richer).toInt()),
        )

        return GameParams(
            recipes = recipes,
            peelSlots = 2 + level(Upgrade.PEEL),
            patienceSeconds = 30.0 + 4.0 * level(Upgrade.AWNING),
            // A shop that is getting better known gets busier. This is the pressure
            // curve — not a shorter day and not a shorter bake, both of which would
            // punish the player for buying things.
            spawnEvery = (8.0 - 0.32 * (day - 1)).coerceAtLeast(4.4) - 0.4 * level(Upgrade.AWNING),
            menu = menuOn(day),
            // Day 1 teaches one clock and one drag. From there the tickets lengthen.
            khodraOne = when (day) {
                1 -> 0.0
                2 -> 0.18
                else -> (0.18 + 0.05 * (day - 2)).coerceAtMost(0.55)
            },
            khodraTwo = if (day < 4) 0.0 else (0.04 * (day - 3)).coerceAtMost(0.22),
            autoCollectAfter = when (level(Upgrade.TIP_JAR)) {
                0 -> null
                1 -> 2.0
                else -> 0.0
            },
        )
    }

    public companion object {
        /** Cheese arrives on day 2, so day 1 teaches one clock before asking for two. */
        public fun menuOn(day: Int): List<Topping> =
            if (day >= 2) listOf(Topping.ZAATAR, Topping.JIBNEH) else listOf(Topping.ZAATAR)
    }
}

/**
 * Everything that survives a day, and the only thing that has to be written to
 * disk. Kept as plain data with its own encoding so the engine stays free of any
 * platform storage API.
 */
public data class Profile(
    val day: Int = 1,
    val purse: Int = 0,
    val upgrades: Upgrades = Upgrades(),
    val bestDay: Int = 0,
) {
    public fun encode(): String = buildList {
        add("v=1")
        add("day=$day")
        add("purse=$purse")
        add("best=$bestDay")
        Upgrade.entries.forEach { u -> if (upgrades.level(u) > 0) add("${u.name}=${upgrades.level(u)}") }
    }.joinToString(";")

    public companion object {
        /** Never throws: a corrupt or half-written save reads as a fresh start. */
        public fun decode(raw: String?): Profile {
            if (raw.isNullOrBlank()) return Profile()
            val map = raw.split(";").mapNotNull {
                val i = it.indexOf('=')
                if (i <= 0) null else it.substring(0, i) to it.substring(i + 1)
            }.toMap()
            if (map["v"] != "1") return Profile()
            val levels = Upgrade.entries.mapNotNull { u ->
                map[u.name]?.toIntOrNull()?.takeIf { it > 0 }?.let { u to it.coerceAtMost(u.maxLevel) }
            }.toMap()
            return Profile(
                day = map["day"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                purse = map["purse"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                upgrades = Upgrades(levels),
                bestDay = map["best"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            )
        }
    }
}
