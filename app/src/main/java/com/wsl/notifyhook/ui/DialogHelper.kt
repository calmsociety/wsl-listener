package com.wsl.notifyhook.ui

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.wsl.notifyhook.Prefs
import com.wsl.notifyhook.utils.formatAppList

fun showAppsPopup(context: Context, prefs: Prefs, tvAppList: TextView) {
    val apps = context.packageManager.getInstalledApplications(0)
        .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
        .map { UiApp(it.loadLabel(context.packageManager).toString(), it.packageName) }
        .sortedBy { it.label.lowercase() }

    val selectedSet = prefs.selectedPackages.toMutableSet()

    val allLabels = apps.map { "${it.label} (${it.pkg})" }
    val allPkgs = apps.map { it.pkg }

    var filteredLabels = allLabels.toMutableList()
    var filteredPkgs = allPkgs.toMutableList()

    val dialogLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(30, 30, 30, 30)
    }

    val searchBox = EditText(context).apply {
        hint = "🔍 Cari aplikasi..."
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF888888.toInt())
        setBackgroundColor(0xFF1E1E1E.toInt())
        setPadding(20, 20, 20, 20)
    }
    dialogLayout.addView(searchBox)

    val listView = ListView(context).apply {
        setBackgroundColor(0xFF121212.toInt())
    }
    val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_multiple_choice, filteredLabels)
    listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
    listView.adapter = adapter

    fun refreshChecks() {
        for (i in filteredPkgs.indices) {
            listView.setItemChecked(i, selectedSet.contains(filteredPkgs[i]))
        }
    }
    refreshChecks()

    listView.setOnItemClickListener { _, _, position, _ ->
        val pkg = filteredPkgs[position]
        if (listView.isItemChecked(position)) selectedSet.add(pkg) else selectedSet.remove(pkg)
    }

    dialogLayout.addView(listView)

    val btnRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    val btnAll = modernMiniButton(context, "✅ All").apply {
        setOnClickListener {
            selectedSet.addAll(allPkgs)
            refreshChecks()
        }
    }
    val btnNone = modernMiniButton(context, "❌ None").apply {
        setOnClickListener {
            selectedSet.clear()
            refreshChecks()
        }
    }
    btnRow.addView(btnAll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    btnRow.addView(btnNone, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    dialogLayout.addView(btnRow)

    searchBox.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val query = s?.toString()?.lowercase() ?: ""
            filteredLabels = mutableListOf()
            filteredPkgs = mutableListOf()
            apps.forEachIndexed { idx, app ->
                if (app.label.lowercase().contains(query) || app.pkg.lowercase().contains(query)) {
                    filteredLabels.add(allLabels[idx])
                    filteredPkgs.add(allPkgs[idx])
                }
            }
            adapter.clear()
            adapter.addAll(filteredLabels)
            adapter.notifyDataSetChanged()
            refreshChecks()
        }
        override fun afterTextChanged(s: Editable?) {}
    })

    AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle("🎯 Pilih aplikasi")
        .setView(dialogLayout)
        .setPositiveButton("💾 Save") { _, _ ->
            prefs.selectedPackages = selectedSet
            tvAppList.text = formatAppList(prefs)
            Toast.makeText(context, "✅ Saved ${selectedSet.size} apps", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("❌ Cancel", null)
        .show()
}

fun showSettingsPopup(context: Context, prefs: Prefs) {
    val layout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 40, 40, 40)
        setBackgroundColor(0xFF121212.toInt())
    }

    val etSecret = EditText(context).apply {
        hint = "Kode Akses (wajib)"
        setText(prefs.accessCode)
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF888888.toInt())
    }

    layout.addView(etSecret)

    AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle("⚙️ Pengaturan")
        .setView(layout)
        .setPositiveButton("💾 Save") { _, _ ->
            val kode = etSecret.text.toString().trim()
            if (kode.isEmpty()) {
                Toast.makeText(context, "❌ Kode Akses wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            prefs.accessCode = kode
            Toast.makeText(context, "✅ Pengaturan tersimpan!", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("❌ Cancel", null)
        .show()
}

data class UiApp(val label: String, val pkg: String)
