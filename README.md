# MusicFlow

MusicFlow is a Java desktop player for PCM WAV tracks. It reads a local
catalog or streams demo audio from a loopback HTTP server.

## Requirements

- JDK 26
- Maven
- An audio output device

Run all commands from the project directory that contains `pom.xml`
and `demo-audio`.

## Build and run

```sh
mvn clean package
java -cp target/classes com.musicflow.Main
```

To use the HTTP source:

```sh
java -cp target/classes com.musicflow.Main --http
```

In IntelliJ IDEA, open the Maven project, select JDK 26, and run
`com.musicflow.Main`. Set `--http` in **Program arguments** for HTTP mode.

## Use the player

1. Search the catalog and add tracks to the queue.
2. Select a track and click **Play selected**.
3. Choose **In order**, **Shuffle**, or **Repeat one**.
4. Use **Pause**, **Resume**, **Next**, and **Stop** to control playback.
5. Adjust the volume during playback or enable echo for the next track.
6. Use **Save playlist** and **Load playlist** to keep a queue between runs.

The player shows elapsed time and timed section labels for the
instrumental demo tracks. Completed listens are saved in `data/history.tsv`.

## Design patterns

| Pattern | Implementation |
| --- | --- |
| Builder | `Playlist.Builder` validates and constructs playlists. |
| Adapter | `LocalCatalogAdapter` and `HttpCatalogAdapter` expose catalog access through `MusicSource`. |
| Facade | `MusicServiceFacade` coordinates the catalog, playlists, playback, and history. |
| Strategy | `PlaybackStrategy` selects the next track in sequential, shuffle, or repeat-one mode. |
| Decorator | `VolumeDecorator` and `EchoDecorator` wrap the audio processor. |
| Observer | `ObservablePlayer` publishes playback events to listeners, including the interface and listening history. |

## Demo audio

The player supports signed PCM WAV at 44,100 Hz, 16-bit mono,
little-endian. If the bundled demo WAV files are missing, build the project
and regenerate them with:

```sh
java -cp target/classes com.musicflow.tools.DemoAudioGenerator
```