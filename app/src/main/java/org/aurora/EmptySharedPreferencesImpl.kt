package org.aurora

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class EmptySharedPreferencesImpl : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any>()

    override fun getString(key: String?, defValue: String?): String? = defValue

    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = defValues

    override fun getInt(key: String?, defValue: Int): Int = defValue

    override fun getLong(key: String?, defValue: Long): Long = defValue

    override fun getFloat(key: String?, defValue: Float): Float = defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue

    override fun contains(key: String?): Boolean = false

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        //there is no content
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        //there is no content
    }

    private class EditorImpl : SharedPreferences.Editor {

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = this

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = this

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this

        override fun remove(key: String?): SharedPreferences.Editor = this

        override fun clear(): SharedPreferences.Editor = this

        override fun commit(): Boolean = true

        override fun apply() {}
    }
}