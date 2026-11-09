package com.musicflow.ui;

import com.musicflow.audio.PlaybackControl;
import com.musicflow.event.PlaybackEvent;
import com.musicflow.model.Playlist;
import com.musicflow.model.Track;
import com.musicflow.playback.strategy.RepeatOneStrategy;
import com.musicflow.playback.strategy.SequentialStrategy;
import com.musicflow.playback.strategy.ShuffleStrategy;
import com.musicflow.service.MusicServiceFacade;
import com.musicflow.storage.PlaylistFile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MusicPlayerFrame extends JFrame {

    private final MusicServiceFacade music;
    private final Runnable cleanup;

    private final ExecutorService playbackWorker =
            Executors.newSingleThreadExecutor();

    private final DefaultListModel<Track> catalogModel =
            new DefaultListModel<>();

    private final DefaultListModel<Track> queueModel =
            new DefaultListModel<>();

    private final DefaultListModel<String> historyModel =
            new DefaultListModel<>();

    private final JList<Track> catalogList = new JList<>(catalogModel);
    private final JList<Track> queueList = new JList<>(queueModel);

    private final JTextField searchField = new JTextField();
    private final JTextField playlistName = new JTextField("My playlist");

    private final JButton addButton = new JButton("Add to queue");
    private final JButton removeButton = new JButton("Remove");
    private final JButton saveButton = new JButton("Save playlist");
    private final JButton loadButton = new JButton("Load playlist");
    private final PlaylistFile playlistFile = new PlaylistFile();

    private String playlistDescription = "Created in the desktop player";

    private final JButton playButton = new JButton("Play selected");
    private final JButton nextButton = new JButton("Next");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton stopButton = new JButton("Stop");

    private final JLabel nowPlaying = new JLabel("No track selected");
    private final JLabel status = new JLabel("Ready");

    private final JProgressBar progressBar = new JProgressBar(0, 1000);
    private final JLabel captionLabel = new JLabel(" ");
    private final Timer progressTimer = new Timer(100, event -> refreshProgress());

    private Track activeTrack;
    private PlaybackControl currentControl;
    private boolean busy;
    private boolean queueChanged = true;
    private boolean skipRequested;
    private boolean closing;

    public MusicPlayerFrame(
            MusicServiceFacade music,
            String sourceName,
            Runnable cleanup
    ) {
        super("MusicFlow");

        this.music = music;
        this.cleanup = cleanup;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1050, 680);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
        setContentPane(content);

        JPanel heading = new JPanel(new BorderLayout());
        JLabel title = new JLabel("MusicFlow");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));

        heading.add(title, BorderLayout.WEST);
        heading.add(new JLabel(sourceName), BorderLayout.EAST);
        content.add(heading, BorderLayout.NORTH);

        catalogList.setCellRenderer(new TrackRenderer());
        queueList.setCellRenderer(new TrackRenderer());

        queueList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        JPanel catalogPanel = new JPanel(new BorderLayout(6, 6));
        catalogPanel.setBorder(
                BorderFactory.createTitledBorder("Music catalog")
        );

        searchField.setToolTipText("Search by title or artist");

        catalogPanel.add(searchField, BorderLayout.NORTH);
        catalogPanel.add(
                new JScrollPane(catalogList),
                BorderLayout.CENTER
        );
        catalogPanel.add(addButton, BorderLayout.SOUTH);

        JPanel queuePanel = new JPanel(new BorderLayout(6, 6));
        queuePanel.setBorder(
                BorderFactory.createTitledBorder("Playlist queue")
        );

        queuePanel.add(playlistName, BorderLayout.NORTH);
        queuePanel.add(
                new JScrollPane(queueList),
                BorderLayout.CENTER
        );
        JPanel queueActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queueActions.add(removeButton);
        queueActions.add(saveButton);
        queueActions.add(loadButton);
        queuePanel.add(queueActions, BorderLayout.SOUTH);

        JSplitPane lists = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                catalogPanel,
                queuePanel
        );

        lists.setResizeWeight(0.5);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(
                BorderFactory.createTitledBorder(
                        "Completed listening history"
                )
        );
        historyPanel.add(new JScrollPane(new JList<>(historyModel)));

        JSplitPane center = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                lists,
                historyPanel
        );

        center.setResizeWeight(0.75);
        content.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JPanel controls = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        controls.add(playButton);
        controls.add(pauseButton);
        controls.add(stopButton);
        controls.add(nextButton);

        JComboBox<String> mode = new JComboBox<>(
                new String[]{"In order", "Shuffle", "Repeat one"}
        );

        controls.add(mode);

        JPanel effects = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        JSlider volume = new JSlider(0, 100, 70);
        volume.setPreferredSize(new Dimension(180, 40));
        volume.setToolTipText("Changes the current playback volume");

        JCheckBox echo = new JCheckBox("Echo on next track");
        echo.setToolTipText(
                "Applied when a track starts, including a replay"
        );

        effects.add(new JLabel("Volume"));
        effects.add(volume);
        effects.add(echo);

        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setString("0:00");

        bottom.add(nowPlaying);
        bottom.add(progressBar);
        bottom.add(captionLabel);
        bottom.add(controls);
        bottom.add(effects);
        bottom.add(status);
        content.add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(event -> addSelectedTracks());
        removeButton.addActionListener(event -> removeSelectedTrack());
        saveButton.addActionListener(event -> savePlaylist());
        loadButton.addActionListener(event -> loadPlaylist());

        playButton.addActionListener(event -> startPlayback(false));

        nextButton.addActionListener(event -> {
            if (busy) {
                skipRequested = true;
                currentControl.stop();
                status.setText("Switching track...");
            } else {
                startPlayback(true);
            }
        });

        pauseButton.addActionListener(event -> {
            if (currentControl == null) {
                return;
            }

            if (currentControl.isPaused()) {
                currentControl.resume();
                pauseButton.setText("Pause");
                status.setText("Playback resumed");
            } else {
                currentControl.pause();
                pauseButton.setText("Resume");
                status.setText("Paused");
            }
        });

        stopButton.addActionListener(event -> {
            if (currentControl != null) {
                skipRequested = false;
                currentControl.stop();
                status.setText("Stopping...");
            }
        });

        mode.addActionListener(event -> {
            switch (mode.getSelectedIndex()) {
                case 1 -> music.setStrategy(new ShuffleStrategy());
                case 2 -> music.setStrategy(new RepeatOneStrategy());
                default -> music.setStrategy(new SequentialStrategy());
            }
        });

        volume.addChangeListener(event ->
                music.setVolume(volume.getValue() / 100.0f)
        );

        echo.addActionListener(event ->
                music.setEchoEnabled(echo.isSelected())
        );

        searchField.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent event) {
                        refreshCatalog();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent event) {
                        refreshCatalog();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent event) {
                        refreshCatalog();
                    }
                }
        );

        music.addPlaybackListener(event ->
                SwingUtilities.invokeLater(() -> handleEvent(event))
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closing = true;
                progressTimer.stop();

                if (currentControl != null) {
                    currentControl.stop();
                }

                playbackWorker.shutdownNow();
                cleanup.run();
                dispose();
            }
        });

        playlistName.addActionListener(event -> queueChanged = true);

        refreshCatalog();
        refreshHistory();
        setBusy(false);
        progressTimer.start();
    }

    private void refreshCatalog() {
        catalogModel.clear();

        for (Track track : music.search(searchField.getText())) {
            catalogModel.addElement(track);
        }
    }

    private void addSelectedTracks() {
        for (Track track : catalogList.getSelectedValuesList()) {
            boolean duplicate = false;

            for (int i = 0; i < queueModel.size(); i++) {
                if (queueModel.get(i).id().equals(track.id())) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                queueModel.addElement(track);
                queueChanged = true;
            }
        }

        if (!queueModel.isEmpty() && queueList.getSelectedIndex() < 0) {
            queueList.setSelectedIndex(0);
        }

        setBusy(false);
    }

    private void removeSelectedTrack() {
        int index = queueList.getSelectedIndex();

        if (index >= 0) {
            queueModel.remove(index);
            queueChanged = true;

            if (!queueModel.isEmpty()) {
                queueList.setSelectedIndex(
                        Math.min(index, queueModel.size() - 1)
                );
            }
        }

        setBusy(false);
    }

    private void preparePlaylist() {
        if (!queueChanged) {
            return;
        }

        music.selectPlaylist(buildQueuePlaylist());
        queueChanged = false;
    }

    private Playlist buildQueuePlaylist() {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < queueModel.size(); i++) {
            ids.add(queueModel.get(i).id());
        }

        return music.createPlaylist(
                playlistName.getText(),
                playlistDescription,
                ids
        );
    }

    private void savePlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save MusicFlow playlist");
        chooser.setSelectedFile(new File("playlist.musicflow"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Replace the existing playlist file?",
                    "Save playlist",
                    JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            playlistFile.save(path, buildQueuePlaylist());
            status.setText("Playlist saved: " + path.getFileName());
        } catch (IOException | IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void loadPlaylist() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load MusicFlow playlist");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            PlaylistFile.SavedPlaylist saved = playlistFile.load(
                    chooser.getSelectedFile().toPath()
            );

            Playlist playlist = music.createPlaylist(
                    saved.name(),
                    saved.description(),
                    saved.trackIds()
            );

            playlistName.setText(playlist.name());
            playlistDescription = playlist.description();
            queueModel.clear();

            for (Track track : playlist.tracks()) {
                queueModel.addElement(track);
            }

            if (!queueModel.isEmpty()) {
                queueList.setSelectedIndex(0);
            }

            music.selectPlaylist(playlist);
            queueChanged = false;

            setBusy(false);
            status.setText("Playlist loaded: " + playlist.name());
        } catch (IOException | IllegalArgumentException exception) {
            showError(exception);
        }
    }

    private void startPlayback(boolean next) {
        if (busy || closing || queueModel.isEmpty()) {
            return;
        }

        try {
            if (!next || queueChanged) {
                preparePlaylist();
            }
        } catch (RuntimeException exception) {
            showError(exception);
            return;
        }

        int selectedIndex = Math.max(
                0,
                queueList.getSelectedIndex()
        );

        PlaybackControl control = new PlaybackControl();
        currentControl = control;
        activeTrack = null;
        refreshProgress();
        skipRequested = false;

        setBusy(true);
        status.setText("Preparing audio...");

        playbackWorker.execute(() -> {
            Optional<Track> result = Optional.empty();
            Exception failure = null;

            try {
                result = next
                        ? music.playNext(control)
                        : music.playAt(selectedIndex, control);
            } catch (Exception exception) {
                failure = exception;
            }

            Optional<Track> completed = result;
            Exception error = failure;

            SwingUtilities.invokeLater(() ->
                    finishPlayback(control, completed, error)
            );
        });
    }

    private void finishPlayback(
            PlaybackControl control,
            Optional<Track> completed,
            Exception failure
    ) {
        if (closing) {
            return;
        }

        boolean advance = skipRequested
                || (failure == null
                && completed.isPresent()
                && !control.isStopped());

        currentControl = null;
        activeTrack = null;
        refreshProgress();
        skipRequested = false;
        setBusy(false);

        if (failure != null) {
            status.setText("Playback failed");
            showError(failure);
            return;
        }

        if (advance) {
            startPlayback(true);
        } else {
            status.setText(
                    control.isStopped()
                            ? "Stopped"
                            : "End of playlist"
            );
        }
    }

    private void handleEvent(PlaybackEvent event) {
        if (closing) {
            return;
        }

        if (event.type() == PlaybackEvent.Type.REQUESTED) {
            activeTrack = event.track();
            nowPlaying.setText(
                    event.track().artist()
                            + " — "
                            + event.track().title()
            );

            for (int i = 0; i < queueModel.size(); i++) {
                if (queueModel.get(i).id().equals(event.track().id())) {
                    queueList.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (event.type() == PlaybackEvent.Type.COMPLETED) {
            refreshHistory();
        }
    }

    private void refreshProgress() {
        if (currentControl == null) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
            progressBar.setString("0:00");
            captionLabel.setText(" ");
            return;
        }

        PlaybackControl.Progress progress = currentControl.progress();
        long position = progress.positionMillis();
        long duration = progress.durationMillis();

        progressBar.setIndeterminate(duration <= 0 && position == 0);

        if (duration > 0) {
            progressBar.setValue((int) (1000.0 * position / duration));
            progressBar.setString(clock(position) + " / " + clock(duration));
        } else {
            progressBar.setValue(0);
            progressBar.setString(clock(position));
        }

        captionLabel.setText(demoCaption(position, duration));
    }

    private String demoCaption(long position, long duration) {
        if (activeTrack == null || duration <= 0
                || !"MusicFlow Demo".equalsIgnoreCase(activeTrack.artist())) {
            return " ";
        }

        if (position < duration / 3) {
            return "Instrumental demo: opening";
        }

        if (position < 2 * duration / 3) {
            return "Instrumental demo: middle";
        }

        return "Instrumental demo: ending";
    }

    private static String clock(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void refreshHistory() {
        historyModel.clear();

        for (var entry : music.historyEntries()) {
            historyModel.addElement(
                    entry.track().artist()
                            + " — "
                            + entry.track().title()
            );
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        playButton.setEnabled(!busy && !queueModel.isEmpty());
        nextButton.setEnabled(!queueModel.isEmpty());
        pauseButton.setEnabled(busy);
        stopButton.setEnabled(busy);

        addButton.setEnabled(!busy);
        removeButton.setEnabled(!busy);
        saveButton.setEnabled(!busy);
        loadButton.setEnabled(!busy);
        queueList.setEnabled(!busy);
        playlistName.setEnabled(!busy);

        if (!busy) {
            pauseButton.setText("Pause");
        }
    }

    private void showError(Exception exception) {
        JOptionPane.showMessageDialog(
                this,
                exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage(),
                "MusicFlow",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static final class TrackRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused
        ) {
            super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    selected,
                    focused
            );

            if (value instanceof Track track) {
                setText(track.artist() + " — " + track.title());
            }

            return this;
        }
    }
}
