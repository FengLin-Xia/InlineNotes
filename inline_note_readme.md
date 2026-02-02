# InlineNote

**InlineNote** is an Android system‑level AI assistant that explains difficult sentences *in place*.

No copy. No app switching. No disruption.

---

## Why InlineNote exists

Across philosophy, architecture, and AI learning, the same problem keeps appearing:

> Language that should enable understanding is instead used to create distance.

Highly specialized jargon, implicit assumptions, and self‑referential writing make readers pause—not because the ideas are impossible, but because the *path to understanding is obstructed*.

Current solutions (copy–paste, search, switching apps, asking others) break reading flow and add cognitive overhead.

InlineNote does one small thing:

> **When a sentence becomes a barrier, it helps you step over it—without leaving where you are.**

---

## What InlineNote does

InlineNote lets you:

1. Select a sentence in **any Android app**
2. Tap **Explain**
3. Read a short, plain‑language explanation in a floating overlay
4. Close it and continue reading

That’s it.

---

## Core principles

These principles are non‑negotiable:

- **In‑situ** – Explanations appear where you are reading
- **Low friction** – No copy, paste, or app switching
- **User‑triggered** – Nothing happens unless you ask
- **Minimal** – One sentence at a time, no over‑analysis

InlineNote is not trying to be smart everywhere—only helpful at the right moment.

---

## How it works (high level)

- Uses **Android Accessibility Service** to detect selected text
- Shows a lightweight **floating trigger button**
- On tap, sends the selected sentence to an LLM with a tightly constrained prompt
- Displays the explanation in a **temporary overlay window**

No content is modified. Nothing is saved by default.

---

## What InlineNote is *not*

InlineNote intentionally does **not**:

- Automatically explain text
- Analyze long documents
- Replace or annotate original content
- Act autonomously or perform tasks
- Assume the user is a beginner or expert

It is a **tool for understanding**, not automation.

---

## Success criteria

InlineNote is successful if:

- A user can go from confusion to understanding in **under 3 seconds**
- Reading flow resumes immediately after closing the overlay
- No tutorial is needed to understand how it works
- The tool never feels intrusive or patronizing

---

## Philosophy

InlineNote is a response to a recurring experience:

> Being locked out of understanding by language that pretends complexity equals depth.

This project does not aim to simplify ideas—only to simplify access to them.

It is a small, careful intervention meant to stay out of the way once it has helped.

---

## Status

This project is in early development. The focus is on getting the *experience* right before expanding features.

Future evolution may include richer explanation modes or optional agent‑like assistance, but never at the cost of the core principles above.

---

**InlineNote**

> Understand the sentence. Keep reading.

