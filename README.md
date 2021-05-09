# PerSista

Small library for persisting _single instances_ of kotlin data classes.

_NB: PerSista uses typeOf() internally which is marked as @ExperimentalStdlibApi so is subject to change (it is on its way to [stabilization](https://youtrack.jetbrains.com/issue/KT-45396) though)_

Firstly, what PerSista is not:

- it's not a database
- it's not an ORM
- it's not a generic persistent key-value store
- it's not a replacement for Jetpack DataStore

Its only use is to persistently store single instances of kotlin data classes (so that they can be recreated after process death, for example) and it does that with the lowest setup cost possible.

It's currently being used in a clean architecture sample to persist app state in a domain module

## Positives
- pure Kotlin so can be used in non-android modules
- coroutine based
- very simple API
- tiny (~150 lines of code)

## Restrictions
- can only save one instance per data class: write a second instance of the same class, it will just overwrite the previous one
- not suitable for enormous data classes, hard limit of 2 GB per instance

## How to get it

Copy the PerSista.kt class into your own app, ~~or add this gradle line to your project (you'll also need mavenCentral() in your list of maven repos)~~ (currently requesting a new repo at maven central...)

~~implementation "co.early.persista:persista:0.1.1"~~


## How to use it

Construct PerSista with the folder you want your data saved to, for an android app you'll want to use Application.getFilesDir(). Typically you will share this single PerSista instance, just use some form of DI (manual or otherwise) to get it wherever it's needed.

```
val perSista = PerSista(application.filesDir)

```

Define your data class, mark it serializable (also don't forget to add the kotlin serialization plugin to gradle)

```
@Serializable
data class DashboardState(
    val dashboardId: Int = 0,
    val userName: String,
    val drivers: List<Driver> = emptyList(),
    val error: Error? = null,
    @Transient val isUpdating: Boolean = false
)

@Serializable
data class Driver(
    val driverId: Int,
    val driverName: String,
    val powerLevel: Int,
    val lat: Int,
    val long: Int
)
```

Save an instance like this:

```
val state = DashboardState(dashboardId = 777, userName = "erdo")

// from inside a coroutine scope
perSista.write(state)

// from outside a coroutine scope
perSista.write(state){ savedState ->
}
```

Read an instance like this:
```
val defaultState = DashboardState(userName = "defaultUser")

// from inside a coroutine scope
val state = perSista.read(defaultState)

// from outside a coroutine scope
perSista.read(state){ readState ->
}
```

## How it works

PerSista uses Kotlin's built in Serialization to turn data classes into json, then it writes that json as a text file on the file system.

Because it needs to accept any data class you might throw at it, it depends on typeOf() internally to work out the class type of the item - typeOf() is marked as @ExperimentalStdlibApi at the moment so it could potentially change in future.

It's up to you to ensure that you only pass something which is serializable to the write method. Because typeOf() is one of those things that behaves differently depending on which kotlin platform you are running on, I've taken a conservative approach, and in the worst case scenario when using PerSista if the write() fails: nothing will happen, if the read() fails: you will receive the default value. Pass a logger in to the constructor if you want to see more detail about what happened.

If you DO want to receive exceptions (during development for instance) pass 'strictMode = true' to the constructor, in that case, the only time you won't receive an exception when something goes wrong is when an attempted read fails because it can't find the file (that's most likely because a write was never performed in the first place).

PerSista prioritizes correctness over performance, all the reads and writes are guaranteed to be done sequentially as they are performed using a coroutine dispatcher created from a single-threaded Executor (though you can overide that if you want)

![file storage location](filestorage.png)

You can browse the files that are saved on an emulator using Device Explorer in Android Studio (View -> Windows -> Device File Explorer), for an android device they will typically be in: data/data/your.app.package/files/persista

## Performance
Performance is "ok", and nothing is run on the UI thread anyway. On a 2017 Pixel XL, PerSista seems to have a setup overhead of around 400ms the first time it writes, after that a small instance of a data class takes around 2-30ms to store.

## Versioning
PerSista has no concept of data class versioning, so if you change the definition of your data class (due to an app upgrade for instance) and kotlin serialization is no longer able to decode the old json to the new data class, you will just get your default value back

## Obfuscation
The state of your data class will be saved in a file named after the data class's qualified name e.g. "foo.bar.app.DashboardState". If that data class gets obfuscated however, the qualified name could easily be renamed to something like "a.b.c.a". If you release a new version of the app, this time the qualified name could be renamed to "b.c.d.d" and PerSista won't be able to find the data from your previous installation. Check the example app's proguard configuration for details.

## Permissions
None required on Android, as long as you stick to the internal app directory `Application.getFilesDir()`

## Example App

![example app screenshot](exampleapp.png)

There is a complete mini example android app in this repo if you need something to copy-paste to get started.

## Anything else
The non-suspend APIs invoke a functional parameter when they are complete, if you are using PerSista on Android this will arrive on the main thread. For non UI kotlin platforms (which don't have a Dispatchers.Main) you should overide the mainDispatcher parameter in the constructor to an available dispatcher of your choice.

## License

    Copyright 2015-2021 early.co

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
