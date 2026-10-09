package app;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerApp extends Application {

    // Player state
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private ListView<String> audioListView;
    private ListView<String> videoListView;
    private ObservableList<String> audioItems;
    private ObservableList<String> videoItems;
    private List<File> audioFiles;
    private List<File> videoFiles;
    private Slider volumeSlider;
    private Slider progressSlider;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private Label statusLabel;
    private Label songTitleLabel;
    private Button btnMute;
    private boolean isMuted = false;
    private double previousVolume = 0.7;
    private boolean isCurrentlyVideo = false;

    // Visual state
    private ImageView backgroundView;
    private ImageView circularImageView;
    private StackPane mediaStack;
    private Stage primaryStageRef;

    // Palette
    private static final String ACCENT_LIGHT = "#87CEEB";
    private static final String ACCENT_MID   = "#1E90FF";
    private static final String ACCENT_GLOW  = "#B0E0E6";

    // Constants
    private static final double VOLUME_STEP = 0.02;
    private static final String BACKGROUND_IMAGE_PATH = "music.jpeg";
    private static final String CIRCLE_IMAGE_PATH = "circle.jpeg";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStageRef = primaryStage;
        audioFiles = new ArrayList<>();
        videoFiles = new ArrayList<>();
        audioItems = FXCollections.observableArrayList();
        videoItems = FXCollections.observableArrayList();

        // Background layer
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0a0a0a;");

        backgroundView = createBackgroundImageView();
        backgroundView.fitWidthProperty().bind(root.widthProperty());
        backgroundView.fitHeightProperty().bind(root.heightProperty());

        Region darkOverlay = new Region();
        darkOverlay.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " +
                "rgba(11,37,69,0.55) 0%, rgba(11,37,69,0.75) 50%, rgba(5,15,30,0.95) 100%);"
        );

        // Sidebar lists
        audioListView = buildList(audioItems, "AUD");
        videoListView = buildList(videoItems, "VID");

        audioListView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal.intValue() >= 0 && newVal.intValue() < audioFiles.size()) {
                        videoListView.getSelectionModel().clearSelection();
                        playAudioAtIndex(newVal.intValue());
                    }
                }
        );
        videoListView.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal.intValue() >= 0 && newVal.intValue() < videoFiles.size()) {
                        audioListView.getSelectionModel().clearSelection();
                        playVideoAtIndex(newVal.intValue());
                    }
                }
        );

        Label audioLabel = sectionLabel("AUDIO", "audio");
        Label videoLabel = sectionLabel("VIDEO", "video");

        VBox audioBox = new VBox(4, audioLabel, audioListView);
        VBox.setVgrow(audioListView, Priority.ALWAYS);
        VBox videoBox = new VBox(4, videoLabel, videoListView);
        VBox.setVgrow(videoListView, Priority.ALWAYS);

        VBox sidebar = new VBox(15, audioBox, videoBox);
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(240);
        sidebar.setPadding(new Insets(20, 10, 20, 20));
        VBox.setVgrow(audioBox, Priority.ALWAYS);
        VBox.setVgrow(videoBox, Priority.ALWAYS);

        // Header
        songTitleLabel = new Label("Nothing playing");
        songTitleLabel.setTextFill(Color.WHITE);
        songTitleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        songTitleLabel.setEffect(new DropShadow(20, Color.web(ACCENT_LIGHT)));
        songTitleLabel.setContentDisplay(ContentDisplay.LEFT);
        songTitleLabel.setGraphicTextGap(10);

        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.web("#b3b3b3"));
        statusLabel.setStyle("-fx-font-size: 13px;");

        VBox nowPlayingHeader = new VBox(2, songTitleLabel, statusLabel);
        nowPlayingHeader.setAlignment(Pos.CENTER);

        // Media area
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);
        mediaView.setFitWidth(900);
        mediaView.setFitHeight(500);
        mediaView.setVisible(false);

        circularImageView = createCircularImageView();

        mediaStack = new StackPane(mediaView, circularImageView);
        mediaStack.setAlignment(Pos.CENTER);
        mediaStack.setPadding(new Insets(10));
        mediaStack.setStyle("-fx-background-color: rgba(11,37,69,0.35); -fx-background-radius: 16;");
        mediaStack.setMinHeight(360);
        mediaStack.setPrefHeight(380);
        mediaStack.setMaxWidth(700);

        mediaStack.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                if (mediaPlayer != null && isCurrentlyVideo) {
                    primaryStageRef.setFullScreen(!primaryStageRef.isFullScreen());
                }
            }
        });

        // Progress row
        currentTimeLabel = new Label("00:00");
        currentTimeLabel.setTextFill(Color.web("#b3b3b3"));
        currentTimeLabel.setStyle("-fx-font-size: 12px;");

        totalTimeLabel = new Label("00:00");
        totalTimeLabel.setTextFill(Color.web("#b3b3b3"));
        totalTimeLabel.setStyle("-fx-font-size: 12px;");

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(500);
        progressSlider.setStyle("-fx-control-inner-background: #0B2545; -fx-accent: " + ACCENT_MID + ";");
        progressSlider.setOnMousePressed(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
            }
        });

        HBox progressRow = new HBox(12, currentTimeLabel, progressSlider, totalTimeLabel);
        progressRow.setAlignment(Pos.CENTER);

        // Transport buttons (icon only)
        Button btnPrev  = createIconButton("Previous (P)",  "previous");
        Button btnPlay  = createIconButton("Play (Space)",  "play");
        Button btnPause = createIconButton("Pause (Space)", "pause");
        Button btnStop  = createIconButton("Stop (S)",      "stop");
        Button btnNext  = createIconButton("Next (N)",      "next");
        btnMute         = createIconButton("Mute (M)",      "volume");

        btnPlay.setStyle(btnPlay.getStyle() +
                "-fx-background-color: " + ACCENT_MID + ";");
        btnPlay.setEffect(new DropShadow(18, Color.web(ACCENT_LIGHT)));

        // Volume slider
        volumeSlider = new Slider(0, 1, previousVolume);
        volumeSlider.setPrefWidth(120);
        volumeSlider.setStyle("-fx-control-inner-background: #0B2545; -fx-accent: " + ACCENT_MID + ";");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && !isMuted) {
                mediaPlayer.setVolume(newVal.doubleValue());
            }
        });

        HBox controlRow = new HBox(12, btnPrev, btnPlay, btnPause, btnStop, btnNext,
                new Separator(javafx.geometry.Orientation.VERTICAL), btnMute, volumeSlider);
        controlRow.setAlignment(Pos.CENTER);

        // Add and Remove buttons (these keep their text)
        Button btnAddAudio = createPillButton("Add Audio", "add");
        Button btnAddVideo = createPillButton("Add Video", "add");
        Button btnRemove   = createPillButton("Remove",    "remove");

        HBox fileRow = new HBox(12, btnAddAudio, btnAddVideo, btnRemove);
        fileRow.setAlignment(Pos.CENTER);

        VBox controlsPanel = new VBox(10, progressRow, controlRow, fileRow);
        controlsPanel.setAlignment(Pos.CENTER);
        controlsPanel.setPadding(new Insets(10, 20, 20, 20));

        VBox centerContent = new VBox(14, nowPlayingHeader, mediaStack, controlsPanel);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(15, 20, 15, 10));
        VBox.setVgrow(mediaStack, Priority.ALWAYS);

        BorderPane content = new BorderPane();
        content.setLeft(sidebar);
        content.setCenter(centerContent);
        content.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(backgroundView, darkOverlay, content);

        // Button actions
        btnPlay.setOnAction(e  -> { playCurrentMedia(); pulse(btnPlay); });
        btnPause.setOnAction(e -> { pauseMedia();       pulse(btnPause); });
        btnStop.setOnAction(e  -> { stopMedia();        pulse(btnStop); });
        btnNext.setOnAction(e  -> { playNext();         pulse(btnNext); });
        btnPrev.setOnAction(e  -> { playPrevious();     pulse(btnPrev); });
        btnMute.setOnAction(e  -> { toggleMute();       pulse(btnMute); });

        btnAddAudio.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Add Audio Files");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Audio Files",
                            "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac", "*.ogg"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            List<File> selected = fc.showOpenMultipleDialog(primaryStage);
            if (selected != null) {
                for (File f : selected) {
                    audioFiles.add(f);
                    audioItems.add(f.getName());
                }
            }
        });

        btnAddVideo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Add Video Files");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Video Files",
                            "*.mp4", "*.m4v", "*.mov", "*.mkv", "*.webm", "*.avi", "*.wmv", "*.flv"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            List<File> selected = fc.showOpenMultipleDialog(primaryStage);
            if (selected != null) {
                for (File f : selected) {
                    videoFiles.add(f);
                    videoItems.add(f.getName());
                }
            }
        });

        btnRemove.setOnAction(e -> {
            int aIdx = audioListView.getSelectionModel().getSelectedIndex();
            int vIdx = videoListView.getSelectionModel().getSelectedIndex();
            if (aIdx >= 0 && aIdx < audioFiles.size()) {
                audioFiles.remove(aIdx);
                audioItems.remove(aIdx);
            }
            if (vIdx >= 0 && vIdx < videoFiles.size()) {
                videoFiles.remove(vIdx);
                videoItems.remove(vIdx);
            }
            if (audioFiles.isEmpty() && videoFiles.isEmpty()) stopMedia();
        });

        // Keyboard controls
        Scene scene = new Scene(root, 1280, 850);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case SPACE:  playPauseToggle();          event.consume(); break;
                case S:      stopMedia();                event.consume(); break;
                case N:      playNext();                 event.consume(); break;
                case P:      playPrevious();             event.consume(); break;
                case UP:     adjustVolume(VOLUME_STEP);  event.consume(); break;
                case DOWN:   adjustVolume(-VOLUME_STEP); event.consume(); break;
                case M:      toggleMute();               event.consume(); break;
                case F:
                    if (isCurrentlyVideo) {
                        primaryStageRef.setFullScreen(!primaryStageRef.isFullScreen());
                    }
                    event.consume();
                    break;
                case ESCAPE:
                    if (primaryStageRef.isFullScreen()) {
                        primaryStageRef.setFullScreen(false);
                        event.consume();
                    }
                    break;
                default: break;
            }
        });

        primaryStage.setTitle("JavaFX Media Player");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1050);
        primaryStage.setMinHeight(720);
        primaryStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1200), backgroundView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    // =====================================================
    // Helper methods
    // =====================================================

    /** Loads a PNG icon from /icons/, ./icons/ or ./src/icons/. */
    private ImageView icon(String name, double size) {
        Image img = null;
        String file = "icons/" + name + ".png";
        try {
            var stream = getClass().getResourceAsStream("/" + file);
            if (stream != null) img = new Image(stream);
        } catch (Exception ignored) { }
        if (img == null || img.isError()) {
            File f = new File(file);
            if (f.exists()) img = new Image(f.toURI().toString());
        }
        if (img == null || img.isError()) {
            File f = new File("src/" + file);
            if (f.exists()) img = new Image(f.toURI().toString());
        }
        ImageView iv = new ImageView();
        if (img != null && !img.isError()) iv.setImage(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    private Label sectionLabel(String text, String iconName) {
        Label l = new Label(text);
        l.setTextFill(Color.web(ACCENT_LIGHT));
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        l.setPadding(new Insets(0, 0, 4, 12));
        l.setGraphic(icon(iconName, 14));
        l.setContentDisplay(ContentDisplay.LEFT);
        l.setGraphicTextGap(8);
        return l;
    }

    private ListView<String> buildList(ObservableList<String> items, String tag) {
        ListView<String> lv = new ListView<>(items);
        lv.setStyle(
                "-fx-control-inner-background: rgba(11,37,69,0.55);" +
                "-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-border-color: rgba(135,206,235,0.3);" +
                "-fx-border-radius: 8; -fx-background-radius: 8;"
        );
        lv.setCellFactory(v -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("[" + tag + "]  " + item);
                    setTextFill(Color.WHITE);
                    setStyle("-fx-background-color: transparent; -fx-padding: 6 10;");
                }
            }
        });
        return lv;
    }

    private ImageView createCircularImageView() {
        Image img = null;
        try {
            var stream = getClass().getResourceAsStream("/" + CIRCLE_IMAGE_PATH);
            if (stream != null) img = new Image(stream);
        } catch (Exception ignored) { }
        if (img == null || img.isError()) {
            File f = new File(CIRCLE_IMAGE_PATH);
            if (f.exists()) img = new Image(f.toURI().toString());
        }
        if (img == null || img.isError()) {
            File f = new File("src/" + CIRCLE_IMAGE_PATH);
            if (f.exists()) img = new Image(f.toURI().toString());
        }

        ImageView iv = new ImageView();
        if (img != null && !img.isError()) {
            iv.setImage(img);
            iv.setFitWidth(300);
            iv.setFitHeight(300);
            iv.setPreserveRatio(false);
            Circle clip = new Circle(150, 150, 150);
            iv.setClip(clip);
            iv.setEffect(new DropShadow(40, Color.web(ACCENT_GLOW)));
        } else {
            iv.setVisible(false);
        }
        return iv;
    }

    private ImageView createBackgroundImageView() {
        Image img = null;
        try {
            var stream = getClass().getResourceAsStream("/" + BACKGROUND_IMAGE_PATH);
            if (stream != null) img = new Image(stream);
        } catch (Exception ignored) { }
        if (img == null || img.isError()) {
            File f = new File(BACKGROUND_IMAGE_PATH);
            if (f.exists()) img = new Image(f.toURI().toString());
        }
        if (img == null || img.isError()) {
            File f = new File("src/" + BACKGROUND_IMAGE_PATH);
            if (f.exists()) img = new Image(f.toURI().toString());
        }
        ImageView iv = new ImageView();
        if (img != null && !img.isError()) iv.setImage(img);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        return iv;
    }

    /** Round, icon-only button with a tooltip (used for transport controls). */
    private Button createIconButton(String tooltip, String iconName) {
        Button b = new Button();
        b.setGraphic(icon(iconName, 20));
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setTooltip(new Tooltip(tooltip));
        String baseStyle =
                "-fx-background-color: rgba(11,37,69,0.85);" +
                "-fx-background-radius: 50;" +
                "-fx-padding: 12;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(135,206,235,0.6);" +
                "-fx-border-radius: 50;" +
                "-fx-border-width: 1.5;";
        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> {
            b.setStyle(baseStyle
                    .replace("rgba(11,37,69,0.85)", "rgba(30,144,255,0.9)")
                    .replace("rgba(135,206,235,0.6)", "rgba(176,224,230,1.0)"));
            b.setEffect(new DropShadow(14, Color.web(ACCENT_GLOW)));
        });
        b.setOnMouseExited(e -> {
            b.setStyle(baseStyle);
            b.setEffect(null);
        });
        return b;
    }

    /** Pill button with icon and text (used for Add / Remove). */
    private Button createPillButton(String text, String iconName) {
        Button b = new Button(text);
        b.setGraphic(icon(iconName, 14));
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setGraphicTextGap(8);
        String baseStyle =
                "-fx-background-color: " + ACCENT_MID + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 25;" +
                "-fx-padding: 9 22 9 22;" +
                "-fx-cursor: hand;";
        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> {
            b.setStyle(baseStyle.replace(ACCENT_MID, ACCENT_LIGHT));
            b.setEffect(new DropShadow(15, Color.web(ACCENT_GLOW)));
        });
        b.setOnMouseExited(e -> {
            b.setStyle(baseStyle);
            b.setEffect(null);
        });
        return b;
    }

    private void pulse(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.92);  st.setToY(0.92);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void playAudioAtIndex(int index) {
        if (index < 0 || index >= audioFiles.size()) return;
        startPlayback(audioFiles.get(index), false);
    }

    private void playVideoAtIndex(int index) {
        if (index < 0 || index >= videoFiles.size()) return;
        startPlayback(videoFiles.get(index), true);
    }

    private void startPlayback(File file, boolean isVideo) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        isCurrentlyVideo = isVideo;

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.setVolume(volumeSlider.getValue());

        circularImageView.setVisible(!isVideo);
        mediaView.setVisible(true);

        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        songTitleLabel.setText(name);
        songTitleLabel.setGraphic(icon(isVideo ? "video" : "audio", 24));

        mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newT.toSeconds());
            }
            currentTimeLabel.setText(formatTime(newT));
        });

        mediaPlayer.setOnReady(() -> {
            totalTimeLabel.setText(formatTime(mediaPlayer.getTotalDuration()));
            progressSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
            mediaView.setFitWidth(mediaStack.getWidth() - 20);
            mediaView.setFitHeight(mediaStack.getHeight() - 20);
            statusLabel.setText(isVideo ? "Now Playing (Video)" : "Now Playing (Audio)");
            mediaPlayer.play();
        });

        mediaPlayer.setOnEndOfMedia(this::playNext);

        mediaPlayer.setOnError(() -> {
            String msg = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "unknown";
            statusLabel.setText("Error: " + msg);
        });
    }

    private void playCurrentMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        } else if (!audioFiles.isEmpty()) {
            playAudioAtIndex(0);
        } else if (!videoFiles.isEmpty()) {
            playVideoAtIndex(0);
        }
    }

    private void pauseMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            statusLabel.setText("Paused");
        }
    }

    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            statusLabel.setText("Stopped");
        }
    }

    private void playPauseToggle() {
        if (mediaPlayer == null) { playCurrentMedia(); return; }
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            statusLabel.setText("Paused");
        } else {
            mediaPlayer.play();
            statusLabel.setText(isCurrentlyVideo ? "Now Playing (Video)" : "Now Playing (Audio)");
        }
    }

    private void playNext() {
        int aIdx = audioListView.getSelectionModel().getSelectedIndex();
        int vIdx = videoListView.getSelectionModel().getSelectedIndex();

        if (aIdx >= 0 && aIdx < audioFiles.size() - 1) {
            audioListView.getSelectionModel().select(aIdx + 1);
        } else if (aIdx == audioFiles.size() - 1 && !videoFiles.isEmpty()) {
            audioListView.getSelectionModel().clearSelection();
            videoListView.getSelectionModel().select(0);
        } else if (vIdx >= 0 && vIdx < videoFiles.size() - 1) {
            videoListView.getSelectionModel().select(vIdx + 1);
        } else if (vIdx == videoFiles.size() - 1 && !audioFiles.isEmpty()) {
            videoListView.getSelectionModel().clearSelection();
            audioListView.getSelectionModel().select(0);
        }
    }

    private void playPrevious() {
        int aIdx = audioListView.getSelectionModel().getSelectedIndex();
        int vIdx = videoListView.getSelectionModel().getSelectedIndex();

        if (aIdx > 0) {
            audioListView.getSelectionModel().select(aIdx - 1);
        } else if (vIdx > 0) {
            videoListView.getSelectionModel().select(vIdx - 1);
        } else if (aIdx == 0 && !videoFiles.isEmpty()) {
            audioListView.getSelectionModel().clearSelection();
            videoListView.getSelectionModel().select(videoFiles.size() - 1);
        } else if (vIdx == 0 && !audioFiles.isEmpty()) {
            videoListView.getSelectionModel().clearSelection();
            audioListView.getSelectionModel().select(audioFiles.size() - 1);
        }
    }

    private void adjustVolume(double delta) {
        double newVal = Math.max(0, Math.min(1, volumeSlider.getValue() + delta));
        volumeSlider.setValue(newVal);
        if (mediaPlayer != null) mediaPlayer.setVolume(isMuted ? 0 : newVal);
        statusLabel.setText(String.format("Volume: %d%%", (int) Math.round(newVal * 100)));
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;
        isMuted = !isMuted;
        if (isMuted) {
            previousVolume = mediaPlayer.getVolume();
            mediaPlayer.setVolume(0);
            volumeSlider.setValue(0);
            btnMute.setGraphic(icon("mute", 20));
            btnMute.getTooltip().setText("Unmute (M)");
            statusLabel.setText("Muted");
        } else {
            mediaPlayer.setVolume(previousVolume);
            volumeSlider.setValue(previousVolume);
            btnMute.setGraphic(icon("volume", 20));
            btnMute.getTooltip().setText("Mute (M)");
            statusLabel.setText("Unmuted");
        }
    }

    private String formatTime(Duration d) {
        if (d == null) return "00:00";
        int m = (int) d.toMinutes();
        int s = (int) (d.toSeconds() % 60);
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}