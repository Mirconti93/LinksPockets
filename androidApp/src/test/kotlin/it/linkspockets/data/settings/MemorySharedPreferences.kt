package it.linkspockets.data.settings

import android.content.SharedPreferences

/** JVM test double: retains committed values across repository instances. */
internal class MemorySharedPreferences : SharedPreferences {
    val values = mutableMapOf<String?, Any?>()
    var canCommit = true
    var commits = 0

    override fun getAll(): MutableMap<String, *> = values.filterKeys { it != null }.mapKeys { it.key.orEmpty() }.toMutableMap()
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun getString(key: String?, defValue: String?): String? =
        if (contains(key)) values[key] as String? else defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        error("Not used by the theme repository")
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String?, Any?>()
        private val removed = mutableSetOf<String?>()
        private var clear = false
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
            apply { pending[key] = values }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { pending[key] = value }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }
        override fun remove(key: String?): SharedPreferences.Editor = apply { removed += key }
        override fun clear(): SharedPreferences.Editor = apply { clear = true }
        override fun commit(): Boolean {
            commits++
            if (!canCommit) return false
            if (clear) values.clear()
            removed.forEach { values.remove(it) }
            values.putAll(pending)
            return true
        }
        override fun apply() { commit() }
    }
}
