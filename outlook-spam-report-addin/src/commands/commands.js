/*
 * Function-command host for the Report as Spam add-in.
 *
 * Phase 1 uses only the task pane (ShowTaskpane), so there is nothing to run
 * here yet. This file exists because the XML manifest's <FunctionFile> element
 * points at commands.html, and it gives Phase 2 a place to register a ribbon
 * function command that automates the send (e.g. via getAsync / EWS / Graph).
 */

Office.onReady(function () {
    // Intentionally empty for Phase 1.
});

// Example scaffold for a future Phase 2 function command:
//
// function reportSpamAutomatically(event) {
//     // ...capture item, build the alert, and send it without a compose window...
//     event.completed();
// }
//
// // Register so the manifest can reference it by name:
// if (typeof Office !== "undefined") {
//     Office.actions.associate("reportSpamAutomatically", reportSpamAutomatically);
// }
