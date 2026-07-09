# Local Remote Setup — CursorRemote + CursorMobile

This guide explains how to connect **CursorMobile** to a local **CursorRemote** relay running on your Mac, so you can monitor and control your local Cursor AI agent from your Android phone.

The connection is made over a private **Tailscale** VPN. The relay is never exposed to the public internet.

---

## What you need

- A Mac with **Cursor IDE** and the **CursorRemote** extension or standalone server
- An Android phone with **CursorMobile** and **Tailscale** installed
- A Tailscale account (free personal plan supports up to 100 devices)
- A valid **CursorRemote license key**

---

## 1. Install CursorRemote on your Mac

### Option A: VS Code / Cursor extension (recommended)

1. Download the latest `.vsix` from the [CursorRemote releases page](https://github.com/len5ky/CursorRemote/releases).
2. Install it in Cursor: open Command Palette (`Cmd+Shift+P`) → **Extensions: Install from VSIX...**
3. Open the **CursorRemote** panel in the activity bar and enter your license key.

### Option B: Standalone server

```bash
git clone https://github.com/len5ky/CursorRemote.git
cd CursorRemote
npm install
cp .env.example .env
# edit .env, then:
npm run dev
```

---

## 2. Launch Cursor with CDP enabled

Cursor must be started with the Chrome DevTools Protocol port open.

```bash
open -a Cursor --args --remote-debugging-port=9222
```

**Important:** fully quit Cursor first (`Cmd+Q`), then run the command above. Do not just close the window.

Verify CDP is active:

```bash
curl http://127.0.0.1:9222/json
```

You should see a JSON list of debug targets.

---

## 3. Install and configure Tailscale

### On your Mac

```bash
brew install tailscale
sudo tailscale up
```

Get your Tailscale IP:

```bash
tailscale ip -4
# example output: 100.64.1.23
```

### On your Android phone

1. Install Tailscale from the [Play Store](https://play.google.com/store/apps/details?id=com.tailscale.ipn).
2. Sign in with the **same Tailscale account** as your Mac.
3. Turn Tailscale on.

Verify both devices show as connected in the Tailscale admin console.

---

## 4. Bind CursorRemote to your Tailscale IP only

This is the most important security step. Do **not** use `0.0.0.0` unless you understand the risks.

### Extension users

1. Open the **CursorRemote** sidebar in Cursor.
2. Click **Open Setup Panel**.
3. Under **Networking**, select **Specific address (Tailscale / custom)**.
4. Enter your Tailscale IP, e.g. `100.64.1.23`.
5. Click **Save & Restart**.

### Standalone users

Edit `.env`:

```env
SERVER_HOST=100.64.1.23
WEBAPP_PASSWORD=a-strong-password
```

Then restart the server.

---

## 5. Get the web client password

- **Extension:** the password is auto-generated on first install. Find it in the CursorRemote Setup Panel or in Cursor Settings (`cursorRemote.webappPassword`).
- **Standalone:** set `WEBAPP_PASSWORD` in `.env`.

Copy this password — you will paste it into CursorMobile.

---

## 6. Configure CursorMobile

1. Open CursorMobile on your Android phone.
2. Make sure Tailscale is active on the phone.
3. On the **Auth** screen, switch the segmented control from **Cloud Agents** to **Local Remote**.
4. Enter the **Relay URL**:
   - Use your Mac's Tailscale IP and the CursorRemote port: `http://100.64.1.23:3000`
   - If you enabled MagicDNS, you can also use: `http://macbook.tailnet.ts.net:3000`
5. Enter the **Web Client Password** from step 5.
6. Tap **Test Connection** to verify the relay responds.
7. Tap **Connect to Relay**.

You will land on the **Local Remote** home screen showing Cursor windows and chat tabs.

---

## 7. Daily usage

- Keep Tailscale active on both devices.
- Keep Cursor running with `--remote-debugging-port=9222`.
- The CursorRemote extension auto-starts the relay when Cursor launches.

---

## Security checklist

| Do | Don't |
|---|---|
| Bind the relay to your Tailscale IP only | Bind to `0.0.0.0` on a public network |
| Use a strong web client password | Share the password or store it in plain text |
| Keep Tailscale on | Disable Tailscale while using local remote |
| Use Tailscale Funnel only for temporary sharing | Leave Funnel running permanently |
| Verify `http://127.0.0.1:9222/json` only responds locally | Expose CDP port 9222 to the internet |

---

## Troubleshooting

### "Connection refused" or "Failed to connect to relay"

1. Confirm both devices are signed into the **same Tailscale account**.
2. Run `tailscale status` on your Mac and check that the phone is listed.
3. Verify CursorRemote is running: open `http://100.64.1.23:3000/health` in a browser on the phone.
4. Make sure Cursor was launched with `--remote-debugging-port=9222`.
5. Check that the relay is bound to the correct IP, not `127.0.0.1`.

### "Invalid relay password"

- Re-copy the password from the CursorRemote Setup Panel.
- Standalone users: verify `WEBAPP_PASSWORD` is set in `.env` and the server was restarted.

### "CDP disconnected" in the app

- Cursor is running but CDP is not enabled. Fully quit and relaunch Cursor with `--remote-debugging-port=9222`.
- Another process may be using port 9222. Check with `lsof -i :9222`.

### CDP works but chat state is stale

- Cursor may have updated its DOM layout. This affects the web client too. Check the [CursorRemote issues page](https://github.com/len5ky/CursorRemote/issues) for known DOM selector updates.

---

## Network security note

CursorMobile allows cleartext HTTP only for the Tailscale CGNAT range (`100.64.0.0/10`) and localhost. Public IP addresses require HTTPS. This is configured in `res/xml/network_security_config.xml`.

Because Tailscale uses WireGuard encryption end-to-end, HTTP over a Tailscale IP is safe within your private network.
