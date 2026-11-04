# SafeDriveAI development workflow

Keep code readable and preserve the existing MVC boundaries in Flutter, Kotlin, and Python.

## Commits

- Use only `feat:` and `fix:` prefixes with a short, concrete English description.
- Make one small, cohesive logical change per commit. Do not collect unrelated features into a large commit.
- Keep tests and directly related documentation with the behavior they verify or explain.
- Verify the affected behavior before committing. Document device or dataset checks that cannot be performed.
- Commit completed increments as work progresses, then push the reviewed commits at the end of the task.
- Treat historical splitting as an explicitly requested exception; preserve existing work and use an exact force-with-lease when rewriting a published branch.
