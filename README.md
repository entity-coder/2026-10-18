# GymTrack — gym management app

A front-desk Android app for gyms and fitness centers: members, plans, fee
tracking, daily check-ins, and expiry reminders — a replacement for the paper
register. Fully offline; all data stays on the device in a Room database.

## What it does

- **Members** — add/edit/delete members with name, phone, optional photo
  (system photo picker, no storage permission needed), and plan
  (Monthly / Quarterly / Yearly with a per-plan fee).
- **Fee tracking** — every member carries a paid period (`planStartDate` →
  `planEndDate`). Recording a payment extends the paid period by the plan's
  month count (from today if the membership already lapsed, otherwise from
  the current end date — no lost days). Statuses everywhere:
  **OK** / **Expires in N d** / **FEE DUE**.
- **Defaulter list** — members whose paid period ended, sorted by expiry.
- **Daily check-in** — one-tap attendance per member; enforced one-per-day
  by a unique database index, so a double tap can never create a duplicate.
- **Dashboard** — active members, fees collected this month, today's
  check-in count, and an "expiring in the next 7 days" watchlist.
- **CSV backup** — one tap exports `members` and `payments` tables to the
  Downloads folder (MediaStore on Android 10+, app files dir below that).

## Screens

| Tab | Contents |
|---|---|
| Dashboard | 4 stat cards, expiring-soon watchlist, CSV export |
| Members | Searchable roster with status badges, add via FAB |
| Check-in | Today's count + visitor list, search + one-tap check-in |
| Fees | Record payment, defaulter list, full payment history |

Tapping a member opens their detail screen: photo, plan, validity line,
check-in / record-payment / edit / delete actions, and payment history.

## How to run

This project was **not compiled or device-tested** on the machine that
built it (no Android SDK there). To run it:

1. Open the project folder in **Android Studio** (Hedgehog or newer).
2. Let Gradle sync (needs internet on first sync for dependencies).
3. Run on an emulator or a physical device (min SDK 24, target 34).

To install without Android Studio, a GitHub Actions workflow
(`.github/workflows/build-apk.yml`) builds a debug APK on every push to
`main` and attaches it to a rolling `apk-latest` release:

`https://github.com/entity-coder/2026-10-18/releases/download/apk-latest/GymTrack-v1.0.apk`

## Tech

- Kotlin, ViewBinding, Material 3 (DayNight theme)
- Room (3 tables: `members`, `payments`, `check_ins`) with Flow-based
  reactive UI, foreign-key cascades (deleting a member removes their
  payments and check-ins)
- Coroutines + `repeatOnLifecycle` for lifecycle-safe collection
- `ActivityResultContracts.PickVisualMedia` for member photos

### Data model

- `Member`: name, phone, photo URI, plan type, plan fee, paid-period
  start/end (midnight epoch-millis).
- `Payment`: member FK, amount, timestamp, note.
- `CheckIn`: member FK, day (midnight epoch-millis); unique
  `(memberId, day)` index.

All membership math (expiry, status, renewal extension) lives in one pure
object, `logic/MembershipCalculator.kt` — no date logic in the UI layer.

## Interview notes

**What is this project?**
GymTrack is an offline-first Android app that replaces a gym's paper
register: it tracks members, their paid plan periods, fee payments, and
daily attendance, and surfaces fee defaulters plus soon-to-expire
memberships so the front desk can act on them.

**Why these technical decisions?**

- **Room over files/SharedPreferences:** the data is relational (members
  → payments → check-ins) with queries like "sum of this month's payments"
  and "members expiring in 7 days" — SQL expresses those directly, and
  foreign-key cascades keep deletes consistent.
- **Paid period stored as start/end dates, status derived:** instead of a
  boolean "paid" flag, each member stores the paid period's end date and
  the status (OK / expiring / fee due) is computed from it. That makes
  renewals a pure date extension and makes "who owes money" a single
  query (`planEndDate < today`).
- **Unique index on (memberId, day):** double-tap check-ins are a real
  front-desk occurrence; enforcing one-check-in-per-day in the schema
  (with `OnConflictStrategy.IGNORE`) means the UI can never corrupt the
  count, even with a race.
- **Pure `MembershipCalculator` object:** all Calendar math lives outside
  activities/fragments, so the expiry/renewal rules are unit-testable and
  every screen shows the same status.
- **Calendar instead of java.time:** minSdk is 24 and java.time needs API
  26 without desugaring; Calendar avoids adding the desugaring dependency
  for a handful of date operations.
- **Photo picker instead of camera/storage permissions:** the system
  visual-media picker needs no runtime permission and returns a URI we
  persist — simpler and more privacy-friendly.
- **No backend / no accounts:** gyms want a register replacement that
  works during internet outages; the CSV export covers backup without a
  server.

**Trade-offs / limitations**

- No cloud sync or multi-device support — one phone holds the truth. The
  CSV export is the backup story, not a sync story.
- No push/SMS reminders for expiring memberships; the dashboard watchlist
  is the reminder (a `WorkManager` daily notification would be the natural
  next step).
- Fee amounts are simple doubles with whole-rupee formatting; no
  part-payments or per-day pro-rating.
- Not compiled or tested on a device in this environment — correctness
  rests on careful code review, not a green build.

**How I'd extend it**

1. Daily `WorkManager` notification listing memberships expiring in the
   next 3 days.
2. Monthly revenue report screen (group payments by month, chart with a
   lightweight chart library).
3. Optional Google Drive backup/restore of the CSVs (or the Room DB file).
4. Attendance streaks per member ("12-day streak") to nudge retention.
5. Migrate the check-in screen to a QR scan: member QR on their phone,
   front desk scans to check in.
