# JavaFX Media Player

A desktop media player built with JavaFX that supports audio and video playback, keyboard controls, playlist management, and a custom themed interface.

This project was developed for the Interactive Multimedia course assignment.

---

## Features

### Core Functionality
- Play, pause, and stop controls for audio and video files
- Video playback through JavaFX MediaView
- Playlist management with add and remove operations
- FileChooser integration for selecting media files

### Keyboard Controls
| Key | Action |
|-----|--------|
| Space | Play / Pause toggle |
| S | Stop playback |
| N | Next item in playlist |
| P | Previous item in playlist |
| Up Arrow | Increase volume by 2 percent |
| Down Arrow | Decrease volume by 2 percent |
| M | Mute / Unmute |
| F | Toggle full-screen video |
| Escape | Exit full-screen |

### Interface and Design
- Custom background image with a dark gradient overlay
- Sky-blue color theme
- Circular image placeholder displayed during audio playback
- Separate panels for audio and video playlists
- Progress slider with seek support
- Volume slider with live adjustment
- Button hover and click animations
- Fade-in transition on application startup

---

## Requirements

- JDK 21 or newer (tested with JDK 25)
- JavaFX SDK 21 or newer (download from https://gluonhq.com/products/javafx/)
- Visual Studio Code with the Extension Pack for Java installed

---

## Setup Instructions

### 1. Clone the repository

    git clone https://github.com/YOUR_USERNAME/MediaPlayerAssignment.git
    cd MediaPlayerAssignment

### 2. Download the JavaFX SDK

Download the JavaFX SDK for your operating system from:

    https://gluonhq.com/products/javafx/

Extract the archive to a location on your computer.

### 3. Populate the lib directory

Copy all JAR files from the extracted SDK's `lib` folder into the project's `lib` folder.

Copy all native library files from the extracted SDK's `bin` folder into the project's `lib` folder as well. On Windows these files have a `.dll` extension. On macOS they use `.dylib`, and on Linux they use `.so`.

### 4. Add image assets

Place the following two images in the project root directory:

- `music.jpeg` — background image, recommended size 1920x1080
- `circle.jpeg` — square cover image, recommended size 600x600

### 5. Verify the VS Code configuration

The `.vscode` folder should contain a `launch.json` with the JavaFX module path and native library path configured, and a `settings.json` with the Java project settings.

The `launch.json` should resemble:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch MediaPlayerApp",
            "request": "launch",
            "mainClass": "app.MediaPlayerApp",
            "cwd": "${workspaceFolder}",
            "classPaths": [
                "${workspaceFolder}/bin"
            ],
            "vmArgs": "--module-path \"${workspaceFolder}/lib\" --add-modules javafx.controls,javafx.media,javafx.fxml \"-Djava.library.path=${workspaceFolder}/lib\""
        }
    ]
}
```

### 6. Run the application

Press F5 in Visual Studio Code.

---

## Project Structure

    MediaPlayerAssignment/
    ├── .vscode/
    │   ├── launch.json
    │   └── settings.json
    ├── lib/                          (JavaFX JARs and native libraries, not committed)
    ├── src/
    │   └── app/
    │       └── MediaPlayerApp.java
    ├── music.jpeg                    (background image, not committed)
    ├── circle.jpeg                   (cover image, not committed)
    ├── .gitignore
    └── README.md

---

## Usage

1. Click the Add Audio button to select audio files (MP3, WAV, M4A, AAC, FLAC, OGG).
2. Click the Add Video button to select video files (MP4, M4V, MOV, MKV, WEBM, AVI, WMV, FLV).
3. Files appear in the appropriate playlist on the left side of the window.
4. Click any item in either playlist to begin playback.
5. Use the keyboard shortcuts listed above for quick control.

---

## Testing Notes

For reliable video playback, use MP4 files encoded with H.264 video and AAC audio. Other codecs may not be supported by the JavaFX media engine on all operating systems.

---

## Author

Your Name
GitHub: https://github.com/YOUR_USERNAME

---

## License

This project was created for educational purposes.