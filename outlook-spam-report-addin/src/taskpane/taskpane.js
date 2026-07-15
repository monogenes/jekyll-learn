/*
 * Report as Spam — task pane logic (Phase 1: human-in-the-loop)
 *
 * Flow:
 *   1. When the pane opens, read the sender and subject of the selected message
 *      from Office.context.mailbox.item and show them for review.
 *   2. When the user clicks the button, build a "Spam Alert" draft and open it in
 *      a compose window via Office.context.mailbox.displayNewMessageForm().
 *   3. We deliberately STOP at the compose window. The user (Monogenes) reviews
 *      and presses Send. Phase 2 will automate the send itself.
 */

// --- Configuration --------------------------------------------------------

// Distribution list the alert is addressed to.
// NOTE: displayNewMessageForm resolves this against the address book. If your
// tenant needs a full SMTP address, change it to e.g. "thg.all@yourdomain.com".
var REPORT_RECIPIENT = "thg.all";

// Prefix prepended to the original subject for the alert.
var SUBJECT_PREFIX = "Spam Alert: ";

// --- State ----------------------------------------------------------------

var currentSender = null; // { displayName, emailAddress }
var currentSubject = "";

// --- Office bootstrap -----------------------------------------------------

Office.onReady(function (info) {
    if (info.host !== Office.HostType.Outlook) {
        setStatus("This add-in only runs in Outlook.", "error");
        return;
    }

    var button = document.getElementById("reportButton");
    button.addEventListener("click", onReportClick);

    loadSelectedMessage();
});

// --- Read the selected message -------------------------------------------

function loadSelectedMessage() {
    var item = Office.context.mailbox.item;

    if (!item) {
        setStatus("No message is selected.", "error");
        return;
    }

    // In read mode, item.subject is a plain string and item.from is an
    // EmailAddressDetails object ({ displayName, emailAddress }).
    currentSubject = item.subject || "(no subject)";
    currentSender = item.from || item.sender || null;

    var senderText = currentSender
        ? formatSender(currentSender)
        : "(unknown sender)";

    document.getElementById("senderValue").textContent = senderText;
    document.getElementById("subjectValue").textContent = currentSubject;

    // Enable the action once we have something to report.
    document.getElementById("reportButton").disabled = false;
    setStatus("");
}

function formatSender(sender) {
    if (sender.displayName && sender.emailAddress &&
        sender.displayName !== sender.emailAddress) {
        return sender.displayName + " <" + sender.emailAddress + ">";
    }
    return sender.emailAddress || sender.displayName || "(unknown sender)";
}

// --- Draft the Spam Alert -------------------------------------------------

function onReportClick() {
    // displayNewMessageForm requires the Mailbox 1.6 requirement set.
    if (!isDisplayNewMessageFormSupported()) {
        setStatus(
            "This Outlook version does not support opening a new message form " +
            "(needs Mailbox requirement set 1.6).",
            "error"
        );
        return;
    }

    var senderEmail = currentSender ? (currentSender.emailAddress || "") : "";
    var senderLabel = currentSender ? formatSender(currentSender) : "(unknown sender)";

    var alertSubject = SUBJECT_PREFIX + currentSubject;
    var htmlBody = buildAlertBody(senderLabel, senderEmail, currentSubject);

    try {
        Office.context.mailbox.displayNewMessageForm({
            toRecipients: [REPORT_RECIPIENT],
            subject: alertSubject,
            htmlBody: htmlBody
        });

        setStatus(
            "Draft opened. Review it and press Send in the compose window.",
            "ok"
        );
    } catch (err) {
        setStatus("Could not open the draft: " + (err && err.message ? err.message : err), "error");
    }
}

function buildAlertBody(senderLabel, senderEmail, subject) {
    // Simple HTML body. escapeHtml keeps message-supplied text from breaking markup.
    var senderLine = senderEmail && senderEmail !== senderLabel
        ? escapeHtml(senderLabel)
        : escapeHtml(senderLabel);

    return [
        "<p>Team,</p>",
        "<p>A suspected spam / phishing message has been reported:</p>",
        "<ul>",
        "<li><strong>Sender:</strong> " + senderLine + "</li>",
        "<li><strong>Subject:</strong> " + escapeHtml(subject) + "</li>",
        "</ul>",
        "<p>Please <strong>do not</strong> reply to it, click any links, or open any " +
        "attachments. If this message is in your inbox, delete it.</p>",
        "<p>Thank you,<br/>Reported via the Report as Spam add-in</p>"
    ].join("");
}

// --- Helpers --------------------------------------------------------------

function isDisplayNewMessageFormSupported() {
    try {
        return Office.context.requirements.isSetSupported("Mailbox", "1.6");
    } catch (e) {
        // If we can't tell, let the call attempt proceed.
        return true;
    }
}

function escapeHtml(text) {
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function setStatus(message, kind) {
    var el = document.getElementById("status");
    el.textContent = message || "";
    el.className = "status" + (kind ? " status--" + kind : "");
}
