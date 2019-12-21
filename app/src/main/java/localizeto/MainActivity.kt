package localizeto.androidexample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import localizeto.*

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val apiKey = "787847642e3b9c47c773921261d490e8"
    private val languages = arrayOf("en", "de", "es", "fr", "pl", "sk", "uk", "ru", "cs")

    private var language = "en"
    private var languageIdx = 0

    private var keys = arrayOf(
        "ok",
        "cancel",
        "error",
        "choose_language",
        "yes",
        "no",
        "retry",
        "abort"
        )

    private var adapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initializeLocalize()

        adapter = MyAdapter(this.keys)

        recycler_view.itemAnimator = DefaultItemAnimator()
        recycler_view.layoutManager = LinearLayoutManager(applicationContext)
        recycler_view.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_latest -> {
                getLatestLanguages()
                true
            }
            R.id.action_select_lang -> {
                changeLanguage()
                true
            }
            R.id.action_v1 -> {
                getSnapshot()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeLocalize() {
        LocalizeTo.configure(this.apiKey, application.filesDir, assets)
        LocalizeTo.load(this.languages)
    }

    private fun getLatestLanguages() {
        LocalizeTo.download(this.languages) {
            if (it.isNullOrEmpty()) {
                LocalizeTo.reload(this.languages)
            } else {
                Log.e("LocalizeTo", it.toString())
            }
        }
        adapter?.updateData()
    }

    private fun changeLanguage() {
        val fragment = LanguageSelectDialog(languages, languageIdx)
        fragment.show(supportFragmentManager, "language_selection_dialog")
    }

    fun doChangeLanguage(languageIdx: Int) {
        if (languageIdx >= 0 && languageIdx < this.languages.size) {
            this.languageIdx = languageIdx
            this.language = this.languages[languageIdx]

            LocalizeTo.setCurrentLanguageCode(this.language)
            adapter?.updateData()
        }
    }

    private fun getSnapshot() {
        LocalizeTo.downloadByVersion("v1.0.0") {
            if (it.isNullOrEmpty()) {
                LocalizeTo.reload(this.languages, version = "v1.0.0")
            } else {
                Log.e("LocalizeTo", it.toString())
            }
        }
        adapter?.updateData()
    }
}
