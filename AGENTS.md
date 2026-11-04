# SafeDriveAI development workflow

## Read before working

- Read [project brief](docs/PROJECT_BRIEF.md), [development rules](docs/DEVELOPMENT_RULES.md),
  and [current status](docs/PROJECT_STATUS.md) before planning implementation.
- Use [architecture](docs/ARCHITECTURE.md) for existing technical decisions and
  [verification](docs/VERIFICATION.md) for dated evidence. Open dataset/device guides when relevant.
- Check the working tree and recent commits before editing; preserve existing user changes.

## Keep the project on course

- Follow the user's SafeDriveAI brief: Flutter Android UI, Kotlin camera/background service,
  Python/OpenCV/Pandas/YOLO/PyTorch AI tooling, LLM voice assistance, MVC, and USB-installable APK.
- Connect each task to a requirement ID in PROJECT_BRIEF. Keep changes within the current request.
- Preserve the face/eye/head/neck and multiview goals. Current frontal ML Kit support is a
  prototype limitation, not a replacement for the remaining requirements.
- Keep code readable and preserve MVC boundaries. Views render, controllers coordinate,
  and domain models make monitoring decisions. Follow DEVELOPMENT_RULES for implementation details.
- Keep alarm detection independent of network and LLM responses. Represent unavailable evidence
  explicitly rather than reporting the driver as alert.
- Separate planned, implemented, and verified behavior. Never equate synthetic smoke tests
  with real dataset accuracy, emulator checks with Galaxy A51 checks, or a fallback with a real LLM run.
- Update PROJECT_STATUS when implementation status or next steps change; record new verification
  evidence with its actual date. Do not mark untested features complete.
- Explicit user instructions take precedence over these project documents. Update the affected
  brief/rules/status when the user changes direction so the documents do not preserve an obsolete decision.


## Commits

- Use only `feat:` and `fix:` prefixes with a short, concrete English description.
- Make one small, cohesive logical change per commit. Do not collect unrelated features into a large commit.
- Keep tests and directly related documentation with the behavior they verify or explain.
- Verify the affected behavior before committing. Document device or dataset checks that cannot be performed.
- Commit completed increments as work progresses, then push the reviewed commits at the end of the task.
- Treat historical splitting as an explicitly requested exception; preserve existing work and use an exact force-with-lease when rewriting a published branch.
