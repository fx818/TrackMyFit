# TrackMyFit

Offline Android gym tracker (Kotlin + Jetpack Compose, Material 3). No account, no cloud, no database:
all data lives in one JSON file in the app's private storage (`files/trackmyfit_data.json`,
plus a `.bak` copy of the previous save).

## Features
- **Dashboard (home):** today's workout, weekly count, streak, weekly volume, body weight + BMI,
  weight gain/loss over 7 days, 30 days and since you started, goal progress, weight trend line graph (1M/3M/6M/1Y/All, with a goal line),
  workouts-per-week bar chart, weekly training volume graph, strength progress (est. 1RM per exercise),
  muscle-group split over the last 30 days, recent PRs.
- **Daily log:** pick any date, tag the day with labels (Chest, Back, Legs, Triceps, Push, Pull, Rest… plus your own),
  add exercises, log sets with weight × reps, see last session's numbers, PR badges, rest timer with vibration,
  notes per exercise and per day, reorder/remove exercises, copy/repeat a previous workout.
- **Exercise library:** 143 built-in exercises across 12 muscle groups; add, edit and delete your own custom exercises.
  The picker is pre-filtered by the day's labels.
- **Exercise detail:** best weight, est. 1RM, sessions, graph of max weight / 1RM / volume, full history.
- **History:** monthly calendar (workout and rest days highlighted), filter by label, tap a day to open or edit it.
- **Body & BMI:** height, goal weight, age, sex; BMI with a colour gauge, healthy weight range, BMR and maintenance calories,
  BMI trend, weight log.
- **Settings:** kg/lb (cm or ft/in), light, dark or system theme, rest timer, manage labels, JSON backup export and import, erase data.

## Build
```
export JAVA_HOME=~/tools/jdk-17.0.20.1+1 ANDROID_HOME=~/Android/Sdk
./gradlew assembleRelease     # -> app/build/outputs/apk/release/app-release.apk
```
The release build is signed with the local debug key so you can sideload it. If you publish to Play Store, set up a real keystore first.
