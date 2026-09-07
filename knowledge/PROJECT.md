---
title: Miracle Bringer — project
type: overview
layer: store
tags: [overview]
---

# Miracle Bringer

## What this is

A NeoForge 1.21.1 mod: once in a while, a lethal blow to a player is
answered with a blessing -- full health, golden hearts, ten seconds under
which nothing harms them -- and a visitor with a halo who appears before
them and rises out of sight when it ends. Rusty's idea, 2026-09-06, built
to the same bar as the guns: specs, rep invariants, a pure layer with
tests, a gametest gate, a photo booth for the end-to-end.

## Shape

Three source sets, one direction of dependency:

- `domain` -- lethality, the odds, the cooldown, what the blessing lets
  through, the visitor's movement. JDK only.
- `main` -- `MiracleHandler` reads the game into the rules at the
  pre-damage event and applies what they say; the visitor entity, its
  renderer with the halo layer, the effect, the memory attachment, config.
- `gametest` -- a mod of its own: the gametests in config batches, and the
  booth.

Decisions: the trigger is the pre-damage event, before absorption
(`D-0001`); `/kill` is exempt both ways (`D-0002`); golden hearts raise the
absorption cap with a lasting modifier (`D-0003`); the visitor is a mindless
mob moved by position (`D-0004`).

## How it is verified

`./gradlew check`: 21 plain-JUnit tests against `domain`, and eleven
gametests on a headless server, among them a sweep of every damage type in
the registry. `./gradlew runPhotoBooth` films the whole event.

## Depends on

Nothing beyond NeoForge.

## License

AGPL-3.0-or-later, copyright Rusty Shackleford and nfx. The visitor's skin
is not this mod's work: see the README's Provenance.
