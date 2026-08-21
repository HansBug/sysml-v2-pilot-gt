# Project Instructions

## Purpose

This repository is a thin wrapper around the pinned official SysML v2 Pilot implementation. The official Pilot parser is the ground truth.

## Rules

- Keep `upstream/SysML-v2-Pilot-Implementation` pinned to an explicit commit.
- Do not copy or fork the official grammar into this repository.
- Do not add ANTLR grammar, parser, or differential-test logic here.
- Keep the CLI and Java API small and stable.
- Preserve source offsets, hidden tokens, diagnostics, and official grammar entry names in serialized output.
- Update README examples and tests when the public CLI/API changes.
- Run `./mvnw verify` before declaring a change ready.

## Style

Use Java 21, ordinary Maven plugins, and concise comments only where they explain an official-Pilot integration detail. Prefer API behavior that can be consumed by both humans and scripts.

The project license is EPL-2.0, matching the official Pilot repository.
