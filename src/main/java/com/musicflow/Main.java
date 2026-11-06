package com.musicflow;

import com.musicflow.audio.PcmAudioPlayer;
import com.musicflow.playback.ObservablePlayer;
import com.musicflow.server.DemoMusicServer;
import com.musicflow.service.MusicServiceFacade;
import com.musicflow.source.HttpCatalogAdapter;
import com.musicflow.source.HttpCatalogClient;
import com.musicflow.source.LocalCatalogAdapter;
import com.musicflow.source.LocalCatalogReader;
import com.musicflow.source.MusicSource;
import com.musicflow.ui.MusicPlayerFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        DemoMusicServer server = null;

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );

            boolean http = Arrays.asList(args).contains("--http");

            MusicSource source;

            if (http) {
                server = new DemoMusicServer(Path.of("demo-audio"));
                server.start();

                source = new HttpCatalogAdapter(
                        new HttpCatalogClient(),
                        server.catalogUri()
                );
            } else {
                source = new LocalCatalogAdapter(
                        new LocalCatalogReader(),
                        Path.of("demo-audio", "catalog.txt")
                );
            }

            MusicServiceFacade music = new MusicServiceFacade(
                    source,
                    new ObservablePlayer(new PcmAudioPlayer())
            );

            music.loadCatalog();

            DemoMusicServer runningServer = server;
            String sourceName = http
                    ? "Source: HTTP streaming"
                    : "Source: local files";

            SwingUtilities.invokeLater(() -> {
                MusicPlayerFrame window = new MusicPlayerFrame(
                        music,
                        sourceName,
                        () -> {
                            if (runningServer != null) {
                                runningServer.close();
                            }
                        }
                );

                window.setVisible(true);
            });
        } catch (Exception exception) {
            if (server != null) {
                server.close();
            }

            exception.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    exception.getMessage(),
                    "MusicFlow startup failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}