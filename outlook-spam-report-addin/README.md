# Report as Spam — Outlook add-in

A small Outlook **task-pane** add-in that adds a **Report as Spam** button to the
ribbon when you're reading a message. It reads the selected message's sender and
subject, then opens a pre-filled **Spam Alert** compose window addressed to
**THG.all** so the team can be warned.

> **Phase 1 — human-in-the-loop.** The add-in stops at the compose window on
> purpose. You (Monogenes) review the draft and press **Send** yourself. Nothing
> is sent automatically. Automating the send is **Phase 2**.

## How it works

1. You read a message in Outlook and click **Report as Spam** on the ribbon
   (Message-Read surface).
2. The task pane opens and reads the current item via
   `Office.context.mailbox.item` — the sender (`item.from`) and the subject
   (`item.subject`) — and shows them for review.
3. You click **Draft Spam Alert to THG.all**. The add-in calls
   `Office.context.mailbox.displayNewMessageForm(...)` to open a new compose
   window that is:
   - **To:** `THG.all`
   - **Subject:** `Spam Alert: ` + the original subject
   - **Body:** a short note listing the sender and telling recipients to ignore
     and delete the message.
4. You review the draft and hit **Send** manually.

Why a task pane (and not a one-click ribbon function)? Phase 1 is about review.
The pane surfaces exactly what will be reported *before* anything is drafted,
which is the whole point of the human-in-the-loop step. The generated compose
window is a second review gate before the message actually goes out.

## Files

| Path | Purpose |
| --- | --- |
| `manifest.xml` | Add-in manifest: ribbon button on the Message-Read surface, task-pane action, icons, permissions. |
| `src/taskpane/taskpane.html` | Task-pane markup. |
| `src/taskpane/taskpane.css` | Task-pane styling. |
| `src/taskpane/taskpane.js` | Reads the item and calls `displayNewMessageForm`. |
| `src/commands/commands.{html,js}` | Function-command host required by the manifest schema; a scaffold for Phase 2. |
| `assets/icon-*.png` | Ribbon / add-in icons (placeholder red badges — swap in real art). |

## Configuration

Open `src/taskpane/taskpane.js` and adjust the constants at the top if needed:

```js
var REPORT_RECIPIENT = "thg.all";     // set a full SMTP address if your tenant needs it
var SUBJECT_PREFIX   = "Spam Alert: ";
```

`displayNewMessageForm` resolves `thg.all` against the address book. If it
doesn't resolve in your tenant, use the full address (e.g.
`thg.all@yourdomain.com`).

## Running it locally

Prerequisites: Node.js, and an Outlook client (Outlook on the web, or new/classic
Outlook on Windows/Mac).

```bash
cd outlook-spam-report-addin
npm install

# 1. Trust a local dev certificate so https://localhost:3000 loads inside Outlook.
npm run dev-certs

# 2. Serve the static files over HTTPS on port 3000.
npm start

# 3. (Optional) validate the manifest.
npm run validate
```

Then **sideload** `manifest.xml`:

- **Outlook on the web:** Settings → *Get Add-ins* → *My add-ins* →
  *Custom Add-ins* → *Add from file…* → pick `manifest.xml`.
- **Windows/Mac desktop:** same *Get Add-ins* dialog, or run
  `npm run sideload` which uses `office-addin-debugging` to load it for you.

Open a message, and **Report as Spam** appears on the ribbon.

> The manifest points every URL at `https://localhost:3000`. If you host the
> static files elsewhere, update the URLs in `manifest.xml` (the `IconUrl`,
> `HighResolutionIconUrl`, `SourceLocation`, and the `<bt:Url>` / `<bt:Image>`
> resources) and the `AppDomains` entry.

## Requirement sets

- Base activation uses **Mailbox 1.3** (the floor required by
  `VersionOverrides`).
- `displayNewMessageForm` needs **Mailbox 1.6**; the code feature-detects it
  with `Office.context.requirements.isSetSupported("Mailbox", "1.6")` and shows a
  message if the running Outlook is too old.

## Phase 2 (later)

Automate the send instead of stopping at the compose window — e.g. convert the
ribbon button to a function command in `src/commands/commands.js` and send via
`Office.context.mailbox.item` APIs, EWS, or Microsoft Graph. The scaffold and a
commented example are already in that file.
