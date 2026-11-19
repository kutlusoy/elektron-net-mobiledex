# Gleec notification: distribution of a modified work

- **Status:** DRAFT, not sent. Every `<...>` is a placeholder.
- **Duty:** condition (c) of the copyright notice of the Komodo DeFi Framework, copied to [`NOTICE-komodo-defi-framework.md`](./NOTICE-komodo-defi-framework.md). It must be met **before the first public artifact** that contains `libkdfbin.so`.
- **Why a notification at all:** the notice allows distributing a modified work under GPL-3.0 only if Gleec Holding OÜ is notified of the distribution. The app is a work that includes the framework, so the notification belongs to the release checklist (M4 in [`concept-elektron-dex.md`](./concept-elektron-dex.md)).

## Recipient

- Company: **Gleec Holding OÜ** (named in `LEGAL/LICENSE-COPYRIGHT-NOTICE` of the framework).
- Address: `<postal address from the company imprint or the signed CLA, to be filled in>`
- Channel: `<email address or contact form; keep the delivery receipt>`

## Subject

Notification of distribution of a work that includes the Komodo DeFi Framework (GPL-3.0), condition (c) of the copyright notice

## Body

> Dear Gleec Holding OÜ,
>
> We distribute a free Android application named **Elektron DEX**, published by the Elektron Net Developers (`<legal entity or team name, if any>`). It includes a build of the Komodo DeFi Framework at commit `367e4dd4d77dd873ab2a5b9d38a998b36e9efe3c` (`kdf-upstream-20261001-86-g367e4dd4d`), bundled as `libkdfbin.so` inside the app package `org.elektronnet.mobiledex`.
>
> This message is the notification required by condition (c) of the framework's copyright notice (`LEGAL/LICENSE-COPYRIGHT-NOTICE`).
>
> 1. **Distribution:** the app and its updates are offered **free of charge** (`<Google Play listing, APK download page, or both>`).
> 2. **Source:** the complete corresponding source of every release is published under the same version tag at `<source URL and tag of the first release>`, together with `SHA256SUMS.txt`.
> 3. **Modifications:** `<none beyond build configuration, or a list>`. The sections marked "license protected" and/or `Dex_Fee`/`DEX_FEE`, and the files listed under condition (f), are **not modified**.
> 4. **Acknowledgment:** copies that are posted separately state that they contain material copyrighted by Gleec Holding OÜ, and the app's About screen tells users that the build is not the original Komodo software (condition (d)).
> 5. **Contact:** `<name, role, email>`.
>
> Please confirm receipt at your convenience. We will record the date and channel of this notification in the release notes.
>
> Kind regards,
> `<name>`, Elektron Net Developers

## Checklist before sending

- [ ] First public artifact is scheduled, and the source tag it will carry is known.
- [ ] The About screen shows the notice, the source link and "not the original Komodo software".
- [ ] No protected section is modified (`git diff` against `367e4dd4d` in the build recipe).
- [ ] The app is free of charge at the moment of the first release.
- [ ] Subject, body and placeholders are complete.
- [ ] Delivery channel can be proven (email receipt or ticket number).

## Log

| Date sent | Channel | Recipient | Reference | Notes |
|---|---|---|---|---|
| (not sent) | | | | |
