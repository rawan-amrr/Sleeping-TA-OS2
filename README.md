# Sleeping TA (OS Project #1)

Java simulation of the Sleeping Teaching Assistant problem using **threads, a mutex lock, and semaphores**, with a Swing GUI.

## Problem
- Students are threads; TAs and waiting chairs are modeled with a mutex / semaphores.
- A student arriving finds a sleeping TA -> wakes him. All TAs busy -> sits on a free chair. No chair -> leaves and comes back later, repeating the same logic.
- Inputs: number of students, chairs, TAs.
- Outputs (real time): TAs working, TAs sleeping, students waiting on chairs, students that will come later.

## Project structure
```
src/
├── Main.java
├── core/            OfficeMonitor, StateListener, Student, TeachingAssistant
├── simulation/      SimulationController, SimulationStatistics
├── gui/             MainFrame
├── utils/           SimulationLogger
└── testing/         SimulationTest
```

## Team
| Member | Role | Files |
|---|---|---|
| 1 | Synchronization & OfficeMonitor | `core/OfficeMonitor.java`, `core/StateListener.java` |
| 2 | Student & TA threads | `core/Student.java`, `core/TeachingAssistant.java` |
| 3 | Simulation controller | `simulation/SimulationController.java`, `Main.java` |
| 4 | GUI | `gui/MainFrame.java` |
| 5 | Testing & logging | `testing/SimulationTest.java`, `utils/SimulationLogger.java` |
| 6 | Statistics & documentation | `simulation/SimulationStatistics.java`, docs |

## OfficeMonitor API (the contract between members)
| Method | Called by | Purpose |
|---|---|---|
| `requestHelp(id)` | Student | Returns a `Ticket` with result `WOKE_TA`, `SEATED` or `NO_CHAIR` |
| `ticket.awaitDone()` | Student | Wait until the TA finishes with this student |
| `studentBackFromLeaving(id)` | Student | After coming back from `NO_CHAIR` |
| `waitForStudent()` | TA | Sleep until a student wakes the TA |
| `finishAndGetNext(taId, ticket)` | TA | Finish a student; returns the next waiting one, or `null` (TA sleeps) |
| `setListener(StateListener)` | Controller | Connect the GUI |
| `getTotalHelped()`, `getTotalTurnedAway()` | Statistics | Cumulative numbers |

`StateListener` callbacks come from a background thread: the GUI must use `SwingUtilities.invokeLater`.

## Synchronization design
- `ReentrantLock mutex`: protects all shared counters and queues.
- `Semaphore chairs`: free waiting chairs (`tryAcquire`, never blocks).
- `Semaphore wake`: sleeping TAs wait on it; a student wakes one with `release()`.
- `Ticket.done` (one semaphore per student): the student waits until *his* TA finishes.
- No thread blocks on a semaphore while holding the mutex, so no deadlock.

## Build & run
Easiest: open the folder in IntelliJ / Eclipse / VS Code (Extension Pack for Java) and run `Main`.

From a terminal (Linux/Mac):
```
javac -d out $(find src -name "*.java")
java -cp out Main
```
From a terminal (Windows):
```
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out Main
```

## Git workflow
- Each member works on his own files only (see table) to avoid merge conflicts.
- `git pull` before starting, commit small, push often.
