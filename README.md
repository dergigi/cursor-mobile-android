# Cursor Mobile for Android

A native Android companion app for controlling Cursor AI coding agents from your phone. Built with Kotlin, Jetpack Compose, and Hilt.

> **⚠️ Unofficial project.** This is an independent, community-built client for the
> [Cursor Background Agents API](https://docs.cursor.com/). It is **not affiliated with,
> endorsed by, or sponsored by Anysphere, Inc. or Cursor.** "Cursor" and the Cursor logo
> are trademarks of Anysphere — this repository does not bundle or redistribute them. You
> need your own Cursor account and API key to use this app.

## Features

- **Cloud Agents**: Create, monitor, and chat with Cursor agents running in the cloud.
- **Remote Control**: Continue directing agents running on your local machine (`/remote-control`).
- **Live Activity**: Ongoing progress notifications keep you updated while agents run.
- **Pull Request Review**: Inspect diffs, commits, deployments, review threads, and merge PRs directly from your phone.
- **Slash Commands, Skills & Automations**: Use `/` commands, skills, and automations from the mobile chat interface.
- **Voice Input**: Dictate prompts using Android speech recognition.
- **Screenshot Annotation**: Attach and annotate images for visual feedback.
- **Biometric Lock**: Secure the app with fingerprint or face unlock.
- **Dark Mode**: Follows the system theme automatically.

## Requirements

- Android 8.0+ (API 26)
- Cursor API key (from Cursor Dashboard)
- Active Cursor paid plan for cloud agents

## Setup

1. Open the project in Android Studio.
2. Copy `local.properties.template` to `local.properties` and configure the SDK path.
3. Build and run the app.
4. On first launch, enter your Cursor API key on the auth screen.

## Architecture

- **UI Layer**: Jetpack Compose screens with Hilt ViewModels.
- **Data Layer**: `AgentRepository` + `CursorApiService` (Ktor) + `SseClient` (OkHttp SSE).
- **Security**: Encrypted DataStore for API key; optional biometric lock.
- **Background**: WorkManager polling + foreground service for live agent progress.

## Key Screens

| Screen | Purpose |
|--------|---------|
| Auth | Connect with Cursor API key |
| Inbox | List of agents |
| Create Agent | Launch a new agent with model/repo/worker selection |
| Chat | Talk to an agent, run slash commands, attach images |
| Agent Detail | Runs, usage, artifacts, and linked pull requests |
| PR Review | Review diffs and merge/squash/auto-merge PRs |
| Settings | Account info, biometric toggle, disconnect |

## Remote Control

To control an agent on your local machine:
1. Create or open an agent.
2. Type `/remote-control` in chat or select a local worker when creating the agent.
3. Choose your machine from the dialog.
4. Keep your computer awake. The agent loop runs in Cursor's cloud while tools execute locally.

## Testing

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## License

Released under the [MIT License](LICENSE) — applies to the source code in this repository.

This project is **not affiliated with Anysphere, Inc. or Cursor**. "Cursor" and related
marks are trademarks of their respective owner and are used here only nominatively to
describe interoperability with the Cursor API. No Cursor brand assets (logos, icons) are
included in this repository; the app ships with its own original launcher icon.
