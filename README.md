# TrackMyFit

An offline Android gym tracker built with Kotlin, Jetpack Compose and Material 3. There's no account, no cloud and no database.
All data is kept in a single JSON file in the app's private storage (`files/trackmyfit_data.json`, plus a `.bak` copy of the previous save).
Progress photos are stored separately in `files/photos/`.

## Features

### v1.0
- **Dashboard:**
  - today's workout, streak and weekly volume
  - BMI with a colour gauge, and weight gained or lost
  - graphs: weight trend with a goal line, workouts per week, training volume, strength (estimated 1RM)
  - muscle split and personal records
- **Daily log:**
  - day labels (Chest, Back, Legs, Push, Pull, Rest and your own)
  - sets with weight × reps, last session's numbers, PR badges and a rest timer
  - notes, copy or repeat a workout
- **Exercises:** 143 built-in exercises, plus your own custom ones.
- **History:** calendar view.
- **Body:** height, BMI, BMR and weight log.
- **Settings:** kg/lb, light or dark theme, JSON backup export and import.

### v1.1
1. **Cardio and timed exercises:** distance + time with pace, for treadmill, bike and running; time, for plank and dead hang. Custom exercises let you pick how they're tracked.
2. **Routines:** save templates such as "Chest Day A" with target sets × reps @ weight, start them in one tap, or save a logged day as a routine.
3. **Weight suggestions:** if you hit every rep last time it suggests adding weight, otherwise it says to stay at the same weight.
4. **Rest timer in the background:** a countdown notification while you use other apps, plus a "Rest over" alert.
5. **Body measurements** (body fat %, waist, chest, arms and more) with graphs, and **progress photos** from the camera or gallery with a before/after compare view.
6. **Set types:** warm-up (excluded from volume, set counts and PRs), drop set, to failure, and an optional RPE.
7. **Workout duration:** the timer starts with your first set, or you can start it yourself or enter the minutes. Duration is shown in History and on the Dashboard.
8. **Weekly workout goal ring** on the Dashboard.
9. **Gym-day reminders:** pick the days and time. They're skipped if you've already trained or marked a rest day.
10. **Plate calculator:** plates per side for 20/15/10 kg bars or no bar.
11. **Home-screen widget:** today's workout, streak, week progress, steps and a "+ Weight" button.
- **Step tracking:** reads your steps (read-only) from Android Health Connect. Shows today's steps on a goal ring and a 7-day and 30-day history.
- **Exercise demos:** an animated start/end demo with step-by-step form instructions, tips and muscles worked for 131 of the 143 exercises. The other 12 have written instructions only.
- **Log several sets at once:** for example "3 sets × 10 reps @ 60 kg" in one step.

Exercise demo photos and most of the instructions come from [free-exercise-db](https://github.com/yuhonas/free-exercise-db), which is public domain (Unlicense).

## Build
```
export JAVA_HOME=~/tools/jdk-17.0.20.1+1 ANDROID_HOME=~/Android/Sdk
./gradlew assembleRelease     # -> app/build/outputs/apk/release/app-release.apk
```
The release build is signed with the local debug key, so you can install it directly on your phone. It also upgrades over v1.0 and keeps your data.
Set up a real keystore before publishing to the Play Store.
