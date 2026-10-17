// SPDX-License-Identifier: GPL-3.0-only
package dev.readiz.wgtinstaller

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import dev.readiz.wgtinstaller.core.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var scan: Button
    private lateinit var youtube: Button
    private lateinit var custom: Button
    private lateinit var install: Button
    private lateinit var stop: Button
    private lateinit var tvSummary: TextView
    private lateinit var selection: TextView
    private lateinit var status: TextView
    private lateinit var logs: TextView
    private lateinit var progress: ProgressBar
    private lateinit var tvActivity: LinearLayout
    private lateinit var versionActivity: LinearLayout
    private lateinit var activitySection: LinearLayout
    private lateinit var scroll: ScrollView
    private var host = ""
    private var tvName = ""
    private var televisions: List<Television> = emptyList()
    private var assets: List<ReleaseAsset> = emptyList()
    private var selected: ReleaseAsset? = null
    private var local: LocalWidget? = null
    private var customSource = false
    private var customRepository = ""
    private var discoveryMessage: String? = null
    private val worker = Executors.newSingleThreadExecutor()
    private var token = Cancellation()
    private var busy = false
    private var searching = false
    private var importing = false
    private var visible = false
    private var wasRunning = false
    private var pickerDialog: AlertDialog? = null
    private var activePopup: String? = null
    private var popupInput: EditText? = null
    private var loginDialog: AlertDialog? = null
    private var loginDialogUrl: String? = null
    private val ink = Color.rgb(28, 33, 38)
    private val muted = Color.rgb(92, 99, 106)
    private val accent = Color.rgb(16, 108, 87)
    private val observer: (InstallState.Snapshot) -> Unit = { render(it) }
    private fun dp(n: Int) = (resources.displayMetrics.density * n).toInt()
    private fun free() = !busy && !InstallState.snapshot.running

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = getSharedPreferences("setup", MODE_PRIVATE)
        host = savedInstanceState?.getString("ip") ?: saved.getString("ip", "").orEmpty()
        tvName = savedInstanceState?.getString("tvName") ?: saved.getString("tvName", "").orEmpty()
        assets = savedInstanceState?.getStringArrayList("assets")?.mapNotNull { runCatching { ReleaseAsset.decode(it) }.getOrNull() }.orEmpty()
        selected = assets.firstOrNull { it.assetId == savedInstanceState?.getLong("selected") }
        local = savedInstanceState?.getString("local")?.let { runCatching { LocalWidget.decode(it) }.getOrNull() }
        customSource = savedInstanceState?.getBoolean("customSource") ?: false
        customRepository = savedInstanceState?.getString("customRepository").orEmpty()
        televisions = savedInstanceState?.getStringArrayList("televisions")?.mapNotNull {
            val fields = it.split('\t', limit = 3)
            if (fields.size == 3) Television(fields[0], fields[1], fields[2]) else null
        }.orEmpty()
        discoveryMessage = savedInstanceState?.getString("discoveryMessage")

        val root = column().apply { setBackgroundColor(Color.WHITE) }
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(safe.left, safe.top, safe.right, maxOf(safe.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom))
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        val body = column().apply { setPadding(dp(20), dp(12), dp(20), dp(24)); isFocusableInTouchMode = true }
        scroll = ScrollView(this).apply { isFillViewport = true; addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
        val heading = text(getString(R.string.app_name), 24, true).apply {
            maxLines = 2
            setAutoSizeTextTypeUniformWithConfiguration(16, 24, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
        }
        val info = ImageButton(this, null, android.R.attr.borderlessButtonStyle).apply {
            setImageResource(R.drawable.ic_info); imageTintList = ColorStateList.valueOf(muted)
            contentDescription = getString(R.string.about); tooltipText = contentDescription
            setPadding(dp(12), dp(12), dp(12), dp(12)); setOnClickListener { showAbout() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        body.addView(titleRow(heading, info))

        divider(body)
        body.addView(text(getString(R.string.tv_title), 18, true))
        scan = button(R.string.find_tv) { discover() }; body.addView(scan)
        tvActivity = lookupActivity(); body.addView(tvActivity)
        tvSummary = text("", 13).apply { setTextColor(muted) }; body.addView(tvSummary)

        divider(body)
        body.addView(text(getString(R.string.setup_step_title), 18, true))
        body.addView(button(R.string.setup_title) { startActivity(Intent(this, SetupGuideActivity::class.java)) })

        divider(body)
        custom = textButton(R.string.short_apps) { if (free()) showCustom() }.apply {
            actionIcon(R.drawable.ic_download, 16, 4)
        }
        body.addView(titleRow(text(getString(R.string.app_title), 18, true), custom))
        body.addView(text(getString(R.string.version_year_note), 13).apply { setTextColor(muted) })
        youtube = button(R.string.youtube) {
            if (free()) {
                if (!customSource && assets.isNotEmpty()) showVersions()
                else resolve(false)
            }
        }; body.addView(youtube)
        versionActivity = lookupActivity(); body.addView(versionActivity)
        selection = text("", 13).apply { setTextColor(muted) }; body.addView(selection)

        divider(body)
        body.addView(text(getString(R.string.install_title), 18, true))
        body.addView(text(getString(R.string.samsung_account_note), 13).apply { setTextColor(muted) })
        install = button(R.string.install) { confirmInstall() }; body.addView(install)
        activitySection = column(); body.addView(activitySection)
        status = text("", 14, true).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        activitySection.addView(status)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        activitySection.addView(progress, LinearLayout.LayoutParams(-1, dp(6)))
        stop = textButton(R.string.stop) {
            token.cancel()
            if (InstallState.snapshot.running) startService(Intent(this, InstallService::class.java).setAction("cancel"))
        }; activitySection.addView(stop)
        logs = text("", 12).apply { typeface = Typeface.MONOSPACE; setTextIsSelectable(true); visibility = View.GONE }
        activitySection.addView(textButton(R.string.details) { logs.visibility = if (logs.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        activitySection.addView(logs)
        val previous = getSharedPreferences("install-journal", MODE_PRIVATE).getString("phase", null)
        if (previous != null && previous != Phase.COMPLETE.name && !InstallState.snapshot.running && InstallState.snapshot.update.phase == Phase.IDLE)
            InstallState.publish(Update(Phase.IDLE, "Previous installation interrupted"), "interrupted")
        render(InstallState.snapshot)
        val popup = savedInstanceState?.getString("popup")
        val draft = savedInstanceState?.getString("popupInput")
        if (popup != null) root.post {
            if (!isDestroyed && !isFinishing && free()) when (popup) {
                "tv" -> showTelevisions()
                "ip" -> showManualIp(draft ?: host)
                "versions" -> if (assets.isNotEmpty()) showVersions()
                "custom" -> showCustom()
                "repository" -> showRepository(draft ?: customRepository)
                "confirm" -> confirmInstall()
                "about" -> showAbout()
            }
        }
    }

    override fun onStart() { super.onStart(); visible = true; InstallState.observe(observer) }
    override fun onStop() {
        visible = false; InstallState.remove(observer)
        loginDialog?.dismiss(); loginDialog = null; loginDialogUrl = null
        super.onStop()
    }
    override fun onDestroy() { dismissPicker(); token.cancel(); worker.shutdownNow(); super.onDestroy() }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("ip", host); out.putString("tvName", tvName)
        out.putBoolean("customSource", customSource); out.putString("customRepository", customRepository)
        out.putString("popup", activePopup)
        out.putString("popupInput", popupInput?.text?.toString()); out.putString("discoveryMessage", discoveryMessage)
        out.putStringArrayList("televisions", ArrayList(televisions.map { "${it.ip}\t${it.name}\t${it.model}" }))
        out.putStringArrayList("assets", ArrayList(assets.map { it.encode() }))
        selected?.let { out.putLong("selected", it.assetId) }; local?.let { out.putString("local", it.encode()) }
        super.onSaveInstanceState(out)
    }

    private fun dismissPicker() {
        val old = pickerDialog; pickerDialog = null; activePopup = null; popupInput = null; old?.dismiss()
    }
    private fun popup(kind: String, builder: AlertDialog.Builder): AlertDialog {
        dismissPicker()
        val dialog = builder.create()
        pickerDialog = dialog; activePopup = kind
        dialog.setOnDismissListener { if (pickerDialog === dialog) { pickerDialog = null; activePopup = null; popupInput = null } }
        dialog.show()
        return dialog
    }
    private fun show(message: String) {
        if (!isDestroyed && !isFinishing) AlertDialog.Builder(this).setMessage(message).setPositiveButton(R.string.ok, null).show()
    }
    private fun showAbout() {
        popup("about", AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.about_body)
            .setPositiveButton(R.string.ok, null))
    }
    private fun selectTelevision(tv: Television) {
        host = Safety.privateIpv4(tv.ip); tvName = tv.name
        getSharedPreferences("setup", MODE_PRIVATE).edit().putString("ip", host).putString("tvName", tvName).apply()
        render(InstallState.snapshot)
    }
    private fun acceptTelevisions(found: List<Television>, message: String? = null) {
        televisions = found.distinctBy { it.ip }.sortedWith(compareBy({ it.name }, { it.ip }))
        discoveryMessage = message
        showTelevisions()
    }
    private fun televisionAdapter() = object : ArrayAdapter<Television>(this, 0, televisions) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = getItem(position)!!
            val name = TvNames.display(tv.name).ifBlank { getString(R.string.tv_device) }
            val chosen = tv.ip == host
            val row = LinearLayout(this@MainActivity).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = dp(64); setPadding(dp(14), dp(10), dp(14), dp(10))
                isDuplicateParentStateEnabled = true
                background = RippleDrawable(ColorStateList.valueOf(Color.argb(35, 16, 108, 87)), GradientDrawable().apply {
                    setColor(if (chosen) Color.rgb(226, 241, 235) else Color.rgb(238, 241, 242))
                    cornerRadius = dp(14).toFloat()
                    if (chosen) setStroke(dp(2), accent)
                }, null)
            }
            row.addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_tv_action); imageTintList = ColorStateList.valueOf(accent)
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) })
            row.addView(column().apply {
                addView(text(name, 16, true).apply { setPadding(0, 0, 0, dp(3)) })
                addView(text(tv.ip, 13).apply { setPadding(0, 0, 0, 0); setTextColor(muted) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_chevron_right); imageTintList = ColorStateList.valueOf(accent)
            }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(8) })
            return FrameLayout(this@MainActivity).apply {
                setPadding(dp(20), dp(4), dp(20), dp(4)); addView(row)
                contentDescription = "$name, ${tv.ip}"
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        }
    }
    private fun showTelevisions() {
        val builder = AlertDialog.Builder(this).setNegativeButton(R.string.cancel, null)
        if (televisions.isEmpty()) {
            builder.setTitle(R.string.no_tv_title).setMessage(discoveryMessage ?: getString(R.string.no_tv))
                .setPositiveButton(R.string.manual_ip) { _, _ -> showManualIp() }
        } else {
            builder.setTitle(R.string.choose_tv)
                .setAdapter(televisionAdapter()) { _, which -> selectTelevision(televisions[which]) }
                .setNeutralButton(R.string.manual_ip) { _, _ -> showManualIp() }
        }
        popup("tv", builder)
    }
    private fun showManualIp(draft: String = host) {
        val field = input(R.string.tv_address_hint).apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE; setText(draft)
        }
        val content = dialogColumn().apply { addView(field) }
        val dialog = popup("ip", AlertDialog.Builder(this).setTitle(R.string.tv_address).setView(content)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.select, null))
        popupInput = field
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val valid = runCatching { Safety.privateIpv4(field.text.toString()) }.getOrElse {
                field.error = getString(R.string.error_ip); return@setOnClickListener
            }
            selectTelevision(Television(valid, "", "")); dismissPicker()
        }
    }
    private fun showCustom() {
        val content = dialogColumn()
        content.addView(button(R.string.find_app) { showRepository() })
        content.addView(button(R.string.choose_local) {
            dismissPicker()
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 21)
        }.apply {
            contentDescription = getString(R.string.choose_local_description)
            tooltipText = contentDescription
        })
        content.addView(text(getString(R.string.local_note), 12).apply { setTextColor(muted) })
        popup("custom", AlertDialog.Builder(this).setTitle(R.string.other_apps).setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton(R.string.cancel, null))
    }
    private fun showRepository(draft: String = customRepository) {
        val field = input(R.string.repository_hint).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            contentDescription = getString(R.string.repository_title)
            setText(draft)
        }
        val content = dialogColumn().apply {
            addView(text(getString(R.string.repository_note), 12).apply { setTextColor(muted) })
            addView(field)
        }
        val dialog = popup("repository", AlertDialog.Builder(this).setTitle(R.string.repository_title).setView(content)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.find_app, null))
        popupInput = field
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!free()) return@setOnClickListener
            val source = runCatching { RepoRef.parse(field.text.toString()) }.getOrElse {
                field.error = getString(R.string.error_repo); return@setOnClickListener
            }
            customRepository = source.fullName
            dismissPicker(); resolve(true, source.fullName)
        }
    }
    private fun versionLabel(asset: ReleaseAsset): String {
        val version = Regex("(?i)tizen[-_ ]?(\\d+\\.\\d+)(?=[-_.]|$)").find(asset.name)?.groupValues?.get(1)
        return when (version) {
            "5.5" -> getString(R.string.version_modern)
            "5.0" -> getString(R.string.version_2019)
            null -> asset.name
            else -> "Tizen $version"
        }
    }
    private fun showVersions() {
        if (assets.isEmpty() || !free()) return
        val catalog = assets.toList()
        val simple = catalog.map { versionLabel(it) }
        val labels = catalog.mapIndexed { index, asset ->
            when {
                asset.sha256 == null -> getString(R.string.file_unverified, asset.name)
                customSource || simple.count { it == simple[index] } > 1 -> getString(R.string.file_size, asset.name, asset.size / 1024)
                else -> simple[index]
            }
        }
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, labels) {
            override fun areAllItemsEnabled() = false
            override fun isEnabled(position: Int) = catalog[position].sha256 != null
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                super.getView(position, convertView, parent).apply { isEnabled = isEnabled(position); alpha = if (isEnabled) 1f else 0.45f }
        }
        popup("versions", AlertDialog.Builder(this)
            .setTitle(getString(if (customSource) R.string.choose_file else R.string.choose_version) + " · " + catalog.first().tag)
            .setSingleChoiceItems(adapter, catalog.indexOfFirst { it.assetId == selected?.assetId }) { dialog, which ->
                if (catalog[which].sha256 == null) return@setSingleChoiceItems
                selected = catalog[which]; local = null; render(InstallState.snapshot); dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null))
    }
    private fun clearRelease() { assets = emptyList(); selected = null; local = null; render(InstallState.snapshot) }
    private fun resolve(customSelection: Boolean, source: String = getString(R.string.youtube_repo)) {
        if (!free()) return
        customSource = customSelection; clearRelease()
        token.cancel(); token = Cancellation(); val c = token; busy = true; importing = false; render(InstallState.snapshot)
        worker.execute {
            val result = runCatching { GitHubReleases(SafeHttp(c)).inspect(source).also { c.check() } }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                busy = false
                if (runCatching { c.check() }.isSuccess) result.onSuccess { catalog ->
                    assets = catalog.assets; render(InstallState.snapshot); showVersions()
                }.onFailure { show(AppMessages.error(this, AppMessages.errorCode(it as? Exception ?: Exception(it)))) }
                render(InstallState.snapshot)
            }
        }
    }
    @Deprecated("Uses the platform document picker on API 26+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 21 || resultCode != RESULT_OK || !free()) return
        val uri = data?.data ?: return
        clearRelease(); customSource = true
        token.cancel(); token = Cancellation(); val c = token; busy = true; importing = true; render(InstallState.snapshot)
        worker.execute {
            val result = runCatching { LocalWgtStore(applicationContext).import(uri, c) }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                busy = false; importing = false
                if (runCatching { c.check() }.isSuccess) result.onSuccess { local = it }
                    .onFailure { show(getString(R.string.error_local)) }
                render(InstallState.snapshot)
            }
        }
    }
    private fun confirmInstall() {
        if (!free()) return
        val asset = selected; val file = local
        if (asset == null && file == null) { show(getString(R.string.error_selection)); return }
        if (asset != null && asset.sha256 == null) { show(getString(R.string.error_digest)); return }
        val target = runCatching { Safety.privateIpv4(host) }.getOrElse { showManualIp(); return }
        val publisher = asset?.repo?.fullName ?: getString(R.string.local_selected)
        val name = file?.name ?: requireNotNull(asset).name
        val summary = getString(R.string.install_confirmation, name, publisher, target)
        popup("confirm", AlertDialog.Builder(this).setTitle(R.string.confirm_install).setMessage(summary)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.install) { _, _ ->
                // Consent applies only to this exact source, package and TV shown above.
                if (free() && selected == asset && local == file && host == target) launch(asset, file, target)
            })
    }
    private fun launch(asset: ReleaseAsset?, file: LocalWidget?, target: String) {
        val request = Intent(this, InstallService::class.java).putExtra("ip", target).putExtra("trust", true)
        file?.let { request.putExtra("local", it.encode()) } ?: asset?.let { request.putExtra("asset", it.encode()) }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 12)
        try { startForegroundService(request) } catch (_: Exception) { show(getString(R.string.error_service)) }
    }
    private fun discover() {
        if (!free()) return
        token.cancel(); token = Cancellation(); val c = token; busy = true; searching = true; render(InstallState.snapshot)
        worker.execute {
            val found = ConcurrentHashMap<String, Television>()
            val result = runCatching { WifiLan(this).discover({ found[it.ip] = it }, c); c.check() }
            runOnUiThread {
                if (isDestroyed || isFinishing) return@runOnUiThread
                busy = false; searching = false; render(InstallState.snapshot)
                if (runCatching { c.check() }.isSuccess) {
                    val message = result.exceptionOrNull()?.let { AppMessages.error(this, AppMessages.errorCode(it as? Exception ?: Exception(it))) }
                    acceptTelevisions(found.values.toList(), message)
                }
            }
        }
    }
    private fun render(s: InstallState.Snapshot) {
        if (isDestroyed) return
        val available = !busy && !s.running
        scan.isEnabled = available; youtube.isEnabled = available; custom.isEnabled = available
        install.isEnabled = available && (selected?.sha256 != null || local != null) && runCatching { Safety.privateIpv4(host) }.isSuccess
        install.setText(if (s.running) R.string.installing else R.string.install)
        scan.setText(if (searching) R.string.searching else R.string.find_tv)
        youtube.setText(if (busy && !searching) { if (importing) R.string.reading_wgt else R.string.finding_app } else R.string.youtube)
        tvSummary.text = getString(R.string.selected_item, listOf(TvNames.display(tvName), host).filter { it.isNotBlank() }.joinToString(" · "))
        tvSummary.visibility = if (host.isBlank()) View.GONE else View.VISIBLE
        selection.text = getString(R.string.selected_item, local?.name ?: selected?.let { if (customSource) it.name else versionLabel(it) + " · " + it.tag }.orEmpty())
        selection.visibility = if (selected == null && local == null) View.GONE else View.VISIBLE
        tvActivity.visibility = if (busy && searching) View.VISIBLE else View.GONE
        versionActivity.visibility = if (busy && !searching) View.VISIBLE else View.GONE
        activitySection.visibility = if (s.running || s.logs.isNotEmpty()) View.VISIBLE else View.GONE
        status.text = AppMessages.status(this, s.update, s.errorCode)
        status.setTextColor(if (s.update.phase == Phase.ERROR) Color.rgb(164, 44, 40) else ink)
        logs.text = if (s.logs.isEmpty()) getString(R.string.no_details) else s.logs.joinToString("\n\n") { entry ->
            AppMessages.status(this, entry.update, entry.errorCode) + (entry.errorCode?.let { "\n[$it]" } ?: "")
        }
        progress.visibility = if (s.running) View.VISIBLE else View.GONE
        progress.isIndeterminate = s.update.progress < 0
        if (s.update.progress >= 0) progress.progress = s.update.progress
        stop.visibility = if (s.running) View.VISIBLE else View.GONE
        if (s.running && !wasRunning) scroll.post { scroll.smoothScrollTo(0, activitySection.top) }
        wasRunning = s.running
        if (visible) showLoginPrompt()
    }
    private fun showLoginPrompt() {
        val url = InstallState.pendingBrowserUrl()
        if (url == loginDialogUrl) return
        loginDialog?.dismiss(); loginDialog = null; loginDialogUrl = url
        if (url == null || isFinishing) return
        fun cancelLogin() { if (InstallState.takeBrowserUrl(url) != null) stop.performClick() }
        loginDialog = AlertDialog.Builder(this).setTitle(R.string.login_title).setMessage(R.string.install_note)
            .setPositiveButton(R.string.login_continue) { _, _ ->
                InstallState.takeBrowserUrl(url)?.let { pending ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pending))) }.onFailure { show(getString(R.string.error_browser)) }
                }
            }.setNegativeButton(R.string.stop) { _, _ -> cancelLogin() }.setOnCancelListener { cancelLogin() }
            .create().also { it.setCanceledOnTouchOutside(false); it.show(); styleStop(it.getButton(AlertDialog.BUTTON_NEGATIVE)) }
    }
    private fun lookupActivity() = column().apply {
        visibility = View.GONE
        addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
        }, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(4) })
        addView(textButton(R.string.stop) { token.cancel() })
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun dialogColumn() = column().apply { setPadding(dp(24), dp(8), dp(24), dp(8)) }
    private fun text(value: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(ink); letterSpacing = 0f
        setPadding(0, dp(5), 0, dp(7)); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun input(hint: Int) = EditText(this).apply {
        setHint(hint); setSingleLine(true); textSize = 16f; minHeight = dp(52)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
    }
    private fun button(label: Int, action: () -> Unit) = Button(this).apply {
        setText(label); isAllCaps = false; textSize = 15f; letterSpacing = 0f; minHeight = dp(56)
        val primary = label == R.string.install
        val neutral = label == R.string.setup_title
        setTextColor(getColorStateList(if (primary) R.color.primary_button_text else if (neutral) R.color.action_button_text else R.color.tonal_button_text))
        setBackgroundResource(if (primary) R.drawable.bg_primary_button else if (neutral) R.drawable.bg_action_button else R.drawable.bg_tonal_button)
        stateListAnimator = null; elevation = 0f; setPadding(dp(20), dp(12), dp(20), dp(12))
        gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(4) }
        val icon = when (label) {
            R.string.find_tv -> R.drawable.ic_tv_action
            R.string.setup_title -> R.drawable.ic_info
            R.string.youtube -> R.drawable.ic_play
            R.string.find_app -> R.drawable.ic_search
            R.string.choose_local -> R.drawable.ic_folder
            R.string.install -> R.drawable.ic_download
            else -> 0
        }
        if (icon != 0) actionIcon(icon)
        setOnClickListener { action() }
    }
    private fun Button.actionIcon(icon: Int, size: Int = 20, gap: Int = 12) {
        val drawable = getDrawable(icon)?.mutate()?.apply { setBounds(0, 0, dp(size), dp(size)) }
        setCompoundDrawablesRelative(drawable, null, null, null)
        compoundDrawablePadding = dp(gap)
        compoundDrawableTintList = textColors
    }
    private fun textButton(label: Int, action: () -> Unit) = Button(this).apply {
        setText(label); isAllCaps = false; textSize = 13f; letterSpacing = 0f
        setTextColor(getColorStateList(R.color.action_button_text))
        minHeight = dp(48); setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(-2, -2)
        if (label == R.string.stop) styleStop(this)
    }
    private fun styleStop(button: Button) = button.apply {
        setBackgroundResource(R.drawable.bg_stop_button)
        setTextColor(getColorStateList(R.color.stop_button_text))
        setPadding(dp(16), dp(8), dp(16), dp(8))
        actionIcon(R.drawable.ic_stop, 16, 6)
    }
    private fun divider(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(Color.rgb(230, 233, 235)) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(16); bottomMargin = dp(12) })
    }
    private fun titleRow(title: TextView, action: View) = LinearLayout(this).apply {
        gravity = android.view.Gravity.CENTER_VERTICAL
        addView(title, LinearLayout.LayoutParams(0, -2, 1f)); addView(action)
    }
}
