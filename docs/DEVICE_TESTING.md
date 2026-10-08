# Device verification

Use a test phone with an active SIM. Sending real SMS can incur charges.

1. Open the app and confirm there is one **제공자** (Providers) page, with **제공자 추가** at the bottom and no tabs or history button.
2. When updating the old app, confirm each watched number became a provider and the previous receiver list was copied to each provider. Confirm names, local-format numbers, forwarding state, and duplicate protection were preserved.
3. Add Provider A with a name, Korean local number, and Receiver A. Save. Confirm the card shows all details. Use its **수정** button to reopen the full edit page; provider information must appear above the receiver list.
4. Add Provider B with a different number and Receiver B. Send a message from A: only Receiver A should get it, formatted as `[ Provider A ] smscontent`. Send from B: only Receiver B should get `[ Provider B ] smscontent`. Confirm the saved provider name is used, and original content, including line breaks, arrives once.
5. Add a shared receiver to both providers. Confirm it receives messages from either provider, while the providers' other receivers remain isolated.
6. Edit A's name, number, and receivers. Save and restart the app. Confirm persistence and confirm B's settings are unchanged. Removed provider/receiver numbers must stop matching or receiving respectively.
7. Change a provider and press Back. Confirm the discard prompt. Cancel to retain the draft; discard to restore the saved settings. Receiver changes must not affect actual routing before provider save.
8. Rotate the phone while editing a provider or receiver. Confirm draft fields and the receiver list survive.
9. Try an empty provider name, invalid phone number, duplicate provider number, duplicate receiver within one provider, or a provider number as a receiver. Confirm validation rejects each. Sharing a receiver across providers should succeed.
10. Save a provider without receivers. Confirm it does not forward messages. Add receivers and save to make it ready. Delete a provider from its edit page, first canceling the confirmation and then confirming it.
11. Grant SMS access, choose a default SMS SIM, and turn forwarding on. Confirm denied or revoked permissions block forwarding. Verify the selected outbound SIM on dual-SIM phones.
12. Send from an unknown sender or a number with similar trailing digits. Confirm neither forwards. Send a multipart SMS and confirm the complete text arrives once.
13. Turn forwarding off, then send from a provider. Confirm no forwarding. Turn it back on, close the app normally, and test again. Removing the last configured provider/receiver route should switch forwarding off.
14. Disable mobile service and confirm there are no automatic retries after a failed attempt. Reboot, unlock, and test background reception; force-stop should block reception until the app is reopened.

Do not use `adb shell am broadcast` to fake incoming SMS: the receiver requires the system's `BROADCAST_SMS` permission. Emulators can simulate incoming SMS with `adb emu sms send`, but cannot establish real carrier forwarding or delivery.
