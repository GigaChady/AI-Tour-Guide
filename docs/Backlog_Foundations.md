# AI Tour Guide / Tour Companion

## 🧭 Project Overview

AI Tour Guide is a context-aware, AI-powered mobile application that generates real-time audio narration about the user’s surroundings while walking through a city.

Instead of playing pre-recorded guides, the system dynamically creates personalized stories based on:

* user location (GPS),
* user preferences,
* walking context (speed, time spent, previous content).

The goal is to provide a **hands-free, immersive exploration experience** similar to a live human guide.

---

# 🎯 MVP (Minimum Viable Product)

## Goal

Validate the core hypothesis:

> *“Does real-time, AI-generated narration improve the experience of exploring a city?”*

## Scope

The MVP includes:

* 📍 GPS location tracking
* 🎛 Selection of basic user preferences (e.g., history / architecture / fun facts)
* 🤖 Narrative generation using LLM
* 🔊 Text-to-Speech (TTS) conversion
* ▶️ Automatic narration triggering during movement

## Excluded from MVP

* ❌ Route planning
* ❌ Advanced navigation
* ❌ POI recommendation system
* ❌ Social features
* ❌ Long-term personalization

## Core Experience

> *“The AI tells you about the city while you walk.”*

---

# 💡 UVP (Unique Value Proposition)

**For travelers exploring a city**
who want to discover interesting stories without constantly looking at their phone,

**AI Tour Companion** is a mobile AI guide
that narrates surroundings in real time.

Unlike traditional audio guides or route-based apps,
**it dynamically generates contextual, flowing stories while walking**,
filtered by user preferences.

---

# 👤 User Stories (Short Form)

## 🎒 Persona 1: Budget Traveler (Student)

* As a traveler, I want to receive recommendations of free or low-cost attractions, so that I can explore without exceeding my budget.
* As a user, I want to receive suggestions of nearby places, so that I can explore spontaneously.
* As a user, I want recommendations tailored to my interests (history, architecture, culture), so that I visit places that truly interest me.
* As a user, I want to hear short and engaging stories about places, so that I better understand the city.
* As a user, I want to explore without following a fixed route, so that I can move freely.

---

## 👨 Persona 2: Casual User (Low-tech)

* As a user, I want minimal initial configuration, so that I don’t waste time setting up the app.
* As a user, I want the app to work in the background or with the screen off, so that I don’t have to look at my phone.
* As a user, I want narration via TTS, so that the experience is natural and smooth.
* As a user, I want narration length to adapt to time spent in a location, so that it feels natural.
* As a user, I want a continuous narration system, so that I don’t have to manually trigger content.

---

## 👨‍👩‍👧 Persona 3: Parent (Family Traveler)

* As a user, I want to plan a trip in advance, so that I save time.
* As a user, I want to modify the route dynamically, so that I can adapt to changing conditions.
* As a user, I want to preview route distance and duration, so that I can plan better.
* As a user, I want a trip summary, so that I can share it with others.
* As a user, I want an intuitive interface, so that the app is easy to use.

### For more - check /product
---

# 🧩 Features (RICE Prioritization)

> RICE = (Reach × Impact × Confidence) / Effort

| Feature                                              | Reach (%) | Impact (0–5) | Confidence (0–1) | Effort (h) | RICE Score | Priority |
|------------------------------------------------------|----------|--------------|------------------|------------|------------|----------|
| Efficient TTS Configuration                          | 90       | 4            | 0.85             | 8          | 38.25      | MUST     |
| User Location Detection                              | 100      | 5            | 0.85             | 12         | 35.42      | MUST     |
| Minimal Mobile App Configuration with User data      | 85       | 4            | 0.9              | 20         | 15.30      | MUST     |
| Interest-Based Narrative Generation                  | 95       | 5            | 0.8              | 28         | 13.57      | MUST     |
| Map Visualization of user's loaction and Background  | 70       | 5            | 0.9              | 24         | 13.25      | MUST     |
| Location-Based Narrative Generation                  | 95       | 5            | 0.8              | 30         | 12.67      | MUST     |
| Dynamic Real-Time Narrative Generation               | 100      | 5            | 0.7              | 50         | 7.00       | MUST     |
| Optional App Customization with Darkmode             | 60       | 3            | 0.9              | 12         | 10.00      | MUST     |

-------------------------------------------------  FIRST RELEASE CUTOFF  -----------------------------------------------------------------

| Feature                                              | Reach (%) | Impact (0–5) | Confidence (0–1) | Effort (h) | RICE Score | Priority |
|------------------------------------------------------|----------|--------------|------------------|------------|------------|----------|
| Automatic Route Detection                            | 75       | 4            | 0.75             | 25         | 9.00       | SHOULD   |
| Speed-Adaptive Narration                             | 80       | 3            | 0.85             | 24         | 8.50       | SHOULD   |
| Background Mode (Screen Off)                         | 75       | 3            | 0.6              | 20         | 6.75       | SHOULD   |
| Route Preview                                        | 65       | 2            | 0.7              | 30         | 3.03       | SHOULD   |


---

## 🔧 Architecture 

* For system architecture and dataflow see /architecture

# 🏁 Summary

This document defines the **product vision, scope, and priorities** for the AI Tour Guide project.
It serves as a foundation for backlog creation, sprint planning, and implementation.

---
