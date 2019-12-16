package com.localize.to

import android.content.res.AssetManager
import android.os.AsyncTask
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


enum class LocalizeToErrorType {
    PARSING_ERROR
}


data class LocalizeToError(val errorType: LocalizeToErrorType) : Error()


private sealed class LocalizeToURL
private data class LocalizeToLanguagesURL(val languages: Array<String>): LocalizeToURL()
private data class LocalizeToSnapshotURL(val version: String): LocalizeToURL()
private data class LocalizeToSnapshotLanguagesURL(val version: String, val languages: Array<String>): LocalizeToURL()


object LocalizeTo {
    const val localizationFolderName = "LocalizeTo"
    const val assetsSubDir = "localizeTo"

    internal const val baseURL = "https://localize.to/api/v1"
    internal var apiKey = ""
    internal var baseDir: File? = null
    internal var assetsManager: AssetManager? = null
    internal var currentLanguage = Locale.getDefault().language
    internal var defaultLanguage: String = "en"
    internal var folderName = localizationFolderName
    internal val jsonMapper = GsonBuilder().create()
    internal val updatedLanguages: MutableMap<String, MutableSet<String?>> = mutableMapOf()

    val translations = mutableMapOf<String, MutableMap<String, String>>()
}

fun LocalizeTo.configure(
    apiKey: String,
    baseDir: File,
    assetsManager: AssetManager? = null,
    currentLanguageCode: String = Locale.getDefault().language,
    defaultLanguageCode: String = "en",
    localizationFolderName: String = LocalizeTo.localizationFolderName
) {
    this.apiKey = apiKey
    this.baseDir = baseDir
    this.assetsManager = assetsManager
    this.currentLanguage = currentLanguageCode
    this.defaultLanguage = defaultLanguageCode
    this.folderName = localizationFolderName
}

fun LocalizeTo.setCurrentLanguageCode(languageCode: String) {
    this.currentLanguage = languageCode
}

fun LocalizeTo.setDefaultLanguageCode(languageCode: String) {
    this.defaultLanguage = languageCode
}

fun LocalizeTo.localize(key: String): String {
    return this.localize(key, this.currentLanguage)
}

fun LocalizeTo.localize(key: String, language: String): String {
    val pairs = this.translations[language]
    if (pairs != null) {
        val result = pairs[key]
        if (result != null) {
            return result
        }
    }

    if (language != this.defaultLanguage) {
        val pairs = this.translations[this.defaultLanguage]
        if (pairs != null) {
            val result = pairs[key]
            if (result != null) {
                return result
            }
        }
    }

    return key
}

fun LocalizeTo.numberOfKeys(language: String): Int {
    val pairs = this.translations[language]
    if (pairs != null) {
        return pairs.keys.size
    }
    return 0
}

fun LocalizeTo.reload(languages: Array<String>, version: String? = null) {
    this.translations.clear()
    this.load(languages, version)
}

fun LocalizeTo.load(languages: Array<String>, version: String? = null) {
    for (language in languages) {
        this.loadLanguage(language, version)
    }
}

fun LocalizeTo.download(language: String, completion: (errors: Array<Error>?) -> Unit) {
    this.download(arrayOf(language), completion)
}

fun LocalizeTo.download(languages: Array<String>, completion: (errors: Array<Error>?) -> Unit) {
    val self = this
    val task = object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg p0: Unit?) {
            self.downloadLanguages(languages) { errors ->
                completion(errors)
            }
        }
    }
    task.execute()
}

fun LocalizeTo.downloadByVersion(version: String, completion: (errors: Array<Error>?) -> Unit) {
    this.downloadByVersion(version, arrayOf(), completion)
}

fun LocalizeTo.downloadByVersion(version: String, language: String, completion: (errors: Array<Error>?) -> Unit) {
    this.downloadByVersion(version, arrayOf(language), completion)
}

fun LocalizeTo.downloadByVersion(version: String, languages: Array<String>, completion: (errors: Array<Error>?) -> Unit) {
    val self = this
    val task = object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg p0: Unit?) {
            self.downloadSnapshot(version, languages) { errors ->
                completion(errors)
            }
        }
    }
    task.execute()
}

private fun LocalizeTo.createOutputFolder(version: String? = null) {
    this.localizationFolder(version).mkdirs()
}

fun LocalizeTo.loadLanguage(language: String, version: String? = null) {
    if (this.loadFromAssets(language, version)) {
        return
    }

    this.loadFromFile(this.fileFromDocuments(language, version), language)
}

fun LocalizeTo.loadFromAssets(language: String, version: String? = null): Boolean {
    val versions = this.updatedLanguages.getOrPut(language, { mutableSetOf() })
    if (version in versions) {
        return false
    }

    val filename = if (version != null) {
        "${this.assetsSubDir}/$version/$language.json"
    } else {
        "${this.assetsSubDir}/$language.json"
    }
    val inputStream = this.assetsManager?.open(filename) ?: return false
    val bufferedReader = BufferedReader(inputStream.reader())
    var inputLine: String?
    val json = StringBuffer()
    while (bufferedReader.readLine().also { inputLine = it } != null) {
        json.append(inputLine)
    }
    inputStream.close()

    return try {
        this.translations[language] = this.jsonMapper.fromJson(json.toString())
        true
    } catch (e: Error) {
        false
    }
}

fun LocalizeTo.fileFromDocuments(language: String, version: String? = null): File = File(
    this.localizationFolder(version), "$language.json")

fun LocalizeTo.localizationFolder(version: String? = null): File {
    val base = File(this.baseDir, this.folderName)
    if (version != null) {
        return File(base, version)
    }
    return base
}

fun LocalizeTo.loadFromFile(filename: File, language: String): Boolean {
    return try {
        this.translations[language] = this.jsonMapper.fromJson(filename.readText())
        true
    } catch (e: Error) {
        false
    }
}

fun LocalizeTo.writeTranslation(content: String, language: String, version: String? = null) {
    this.fileFromDocuments(language, version).writeText(content)
    this.updatedLanguages.getOrPut(language, { mutableSetOf() }).add(version)
}

fun LocalizeTo.downloadLanguages(languages: Array<String>, completion: (errors: Array<Error>?) -> Unit) {
    this.apiCall(LocalizeToLanguagesURL(languages), { json ->
        val errors = mutableListOf<Error>()
        this.createOutputFolder()
        for (language in languages) {
            val pairs = (json as MutableMap<*, *>)[language]
            if (pairs != null) {
                this.writeTranslation(this.jsonMapper.toJson(pairs), language)
            } else {
                errors.add(LocalizeToError(LocalizeToErrorType.PARSING_ERROR))
            }
        }

        if (errors.size > 0) {
            completion(errors.toTypedArray())
        } else {
            completion(null)
        }
    }, { error ->
        completion(arrayOf(error))
    })
}

fun LocalizeTo.downloadSnapshot(version: String, languages: Array<String>, completion: (errors: Array<Error>?) -> Unit) {
    val url = if (languages.isNotEmpty()) {
        LocalizeToSnapshotLanguagesURL(version, languages)
    } else {
        LocalizeToSnapshotURL(version)
    }

    this.apiCall(url, { json ->
        val errors = mutableListOf<Error>()
        this.createOutputFolder(version)
        val snapshotLanguages = if (languages.isNotEmpty()) {
            languages
        } else {
            (json as MutableMap<String, *>).keys.toTypedArray()
        }

        for (language in snapshotLanguages) {
            val pairs = (json as MutableMap<*, *>)[language]
            if (pairs != null) {
                this.writeTranslation(this.jsonMapper.toJson(pairs), language, version)
            } else {
                errors.add(LocalizeToError(LocalizeToErrorType.PARSING_ERROR))
            }
        }

        if (errors.isNotEmpty()) {
            completion(errors.toTypedArray())
        } else {
            completion(null)
        }
    }, { error ->
        completion(arrayOf(error))
    })

}

private fun LocalizeTo.downloadURL(urlString: LocalizeToURL): URL {
    return when (urlString) {
        is LocalizeToLanguagesURL -> {
            URL("${this.baseURL}/languages/${urlString.languages.joinToString(",")}?apikey=${this.apiKey}")
        }
        is LocalizeToSnapshotURL -> {
            URL("${this.baseURL}/snapshot/${urlString.version}?apikey=${this.apiKey}")
        }
        is LocalizeToSnapshotLanguagesURL -> {
            URL("${this.baseURL}/snapshot/${urlString.version}/language/${urlString.languages.joinToString(",")}?apikey=${this.apiKey}")
        }
    }
}

private fun LocalizeTo.apiCall(url: LocalizeToURL, success: (json: Any) -> Unit, failure: (error: Error) -> Unit) {
    val url = this.downloadURL(url)
    val urlConnection = url.openConnection() as HttpsURLConnection
    try {
        urlConnection.setRequestProperty("Accept-Type", "application/json")
        urlConnection.setRequestProperty("Content-Type", "application/json")
        val self = this
        val task = object : AsyncTask<Unit, Unit, Unit>() {
            override fun doInBackground(vararg p0: Unit?) {
                val result = if (urlConnection.responseCode == HttpsURLConnection.HTTP_OK) {
                    val ins = BufferedReader(
                        InputStreamReader(
                            urlConnection.inputStream
                        )
                    )
                    var inputLine: String?
                    val response = StringBuffer()

                    while (ins.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    ins.close()
                    response.toString()
                } else {
                    "{}"
                }
                urlConnection.disconnect()
                success(self.jsonMapper.fromJson<MutableMap<*, *>>(result))
            }
        }
        task.execute()
    } catch (e: Error) {
        failure(e)
    }
}

fun String.localize(): String {
    return LocalizeTo.localize(this)
}

fun String.localize(language: String): String {
    return LocalizeTo.localize(this, language)
}

fun String.unlocalized(): String {
    return this
}
