# Localize.To Android Client

This module allows you to get localization strings from [Localize.to](https://localize.to) service.
It's nice and flexible replacement for native Android localization files.

## Localize.To REST API

GET /language/{language}

GET /languages/{language1,language2}

GET /snapshots

GET /snapshot/latest/info

GET /snapshot/latest

GET /snapshot/{version}

GET /snapshot/{version}/language/{language}

GET /snapshot/{version}/languages/{language1,language2}

## Currently, this module implements only these API calls:

    GET /languages/{language1,language2}
    GET /snapshot/{version}
    GET /snapshot/{version}/languages/{language1,language2}

this is enough for the most cases.

## Installation

With jcenter:

    repositories {
        jcenter()
    }

    dependencies {
        implementation 'com.github.whitetown:localize-to:*'
        //    implementation 'com.google.code.gson:gson:2.8.5'
        //    implementation 'com.github.salomonbrys.kotson:kotson:2.5.0'
    }


## Initialize the module with an API key

```kotlin
    import localizeto.*

    LocalizeTo.shared.configure(PROJECT_API_KEY)

    //additional parameters:
    LocalizeTo.shared.configure(PROJECT_API_KEY,
        "es",           //currentLanguageCode, by default "en"
        "en",           //defaultLanguageCode, could be null, by default "en"
        "LocalizeTo",   // folder for downloadee translations "LocalizeTo"
        assets          //folder with initial translations
        )
```

## Load earlier downloaded languages from 'LocalizeTo' folder or Application Assets

```kotlin
    LocalizeTo.load(["en", "de", "es", ..., ])

```

## Set current and/or default language

```swift
    LocalizeTo.setCurrentLanguageCode("fr")

    LocalizeTo.setDefaultLanguageCode("en")
```

The default language is used if there is no translation for current language.
i.e.
let value = "my_key".localized()
first will try to find French translation, then English translation


## Get localization strings

```kotlin
    //for current language
    let value = LocalizeTo.localize("localization_key")

    //for particular language
    let value = LocalizeTo.localize("localization_key", "de")
```

It's more convenient to use String extensions:

```kotlin
    //for current language
    let value = "localization_key".localized()

    //for particular language
    let value = "localization_key".localize("de")
```

## Special unlocalized() extension

```kotlin
    //let value = "localization_key".unlocalized()
```

It does nothing.
Use it when you do not know localization keys yet and then you can easily find all of them in your project.

## Download new localization strings from the service

    Get localized strings for particular languages

```kotlin
    LocalizeTo.download(arrayOf("en", "fr", "de", ...)) { (errors) in
        if (errors.isNullOrEmpty()) {
            LocalizeTo.reload(arrayOf("en", "fr", ...))
        } else {
            Log.e("LocalizeTo", errors.toString())
        }
    }

```

Get localized strings for a snapshot (all languages)

```kotlin
    LocalizeTo.downloadByVersion("v1.0.0") { (errors) in
        if (errors.isNullOrEmpty()) {
            LocalizeTo.reload(this.languages, version = "v1.0.0")
        } else {
            Log.e("LocalizeTo", errors.toString())
        }
    }

```

Get localized strings for a snapshot (particular languages)

```kotlin
    LocalizeTo.downloadByVersion("v1.0.0", "en") { (errors) in
        if (errors.isNullOrEmpty()) {
            LocalizeTo.reload(["en"], version = "v1.0.0")
        } else {
            Log.e("LocalizeTo", errors.toString())
        }
    }

    LocalizeTo.downloadByVersion("v1.0.0", arrayOf("en", "de")) { (errors) in
        if (errors.isNullOrEmpty()) {
            LocalizeTo.reload(arrayOf("en", "de"), version = "v1.0.0")
        } else {
            Log.e("LocalizeTo", errors.toString())
        }
    }
```

