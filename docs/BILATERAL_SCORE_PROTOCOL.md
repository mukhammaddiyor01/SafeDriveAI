# Unified eye score — 2026-11-11

SD-03/SD-05/SD-06. Recorded before new held-out inference.

The previous s011/s013 test is development data from now on, not fresh test.
Reserve previously unused s000 and s002 for the new test. s014 aliases s002
and is excluded from training. Audit every local manifest and image hash.
Use fixed 256 MiB archive prefixes, complete PNG entries only. Annotated eye
features only; no copying one eye's label to the other. Automatic pose labels
remain estimates, not ground-truth camera angles.

Use existing s007/s009/s010/s011/s012/s013 extracted features for training.
Reserve deterministic SHA-256/seed42 strata by view AND open/closed label,
4 train images per 1 test image in each view. Within each partition retain
minority eye states first (up to half of the selected count), then fill from
the remaining deterministic rank. Exact 80/20 applies to each view overall,
not each label: available train right-closed frames are scarce. This reservation
clarification is recorded before new test inference. Do not select by predicted correctness.
Require both labels in each side before fitting; report coverage separately.
Train the same two-feature PyTorch logistic model for fixed 400 epochs,
Adam .025, train-only normalization and class weights. Fixed threshold .50.
Export a single eye-open probability; screen and closed-eye timer use the
same probability. A 1 500 ms sustained score below .50 triggers the alarm;
unknown evidence does not count as open or closed.

Accept only if each side has >=5 closed and >=20 open, closed recall >=80%,
open false-positive rate <=15%, positive blink and negative gap coefficient.
Additionally report 35–45° samples and require >=5 closed in EACH near-edge
side with the same recall/FPR requirements before claiming that dataset band.
If coverage/quality fails, do not call ±45° complete or tune on these test results.
Actual phone camera angle acceptance is a separate live test.

Source annotations: https://zenodo.org/records/3685316
Face crops: https://zenodo.org/records/2529036


## Frozen run result

7 584 train / 1 896 previously unused test images (exact 80/20), 400 epochs.
Closed recall 602/605 = 99.50%; open false positives 264/1291 = 20.45%.
Left-side false positives were 56.43%; the 35–45° right group had no samples.
The checkpoint failed the predeclared criteria and was NOT integrated.
No threshold/checkpoint was adjusted using this result. s000/s002 are now
historically used and cannot serve as fresh subjects for another experiment.
[Full metrics and provenance](experiments/bilateral-score-2026-11-11.json).

The session-local reference introduced after live debugging is a distinct
heuristic; these population-model numbers do not validate that heuristic.
