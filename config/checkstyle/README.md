# Checkstyle configuration

This directory contains the source-style policy for Drone Photo Service.

## Principles

- Prefer rules that identify objective readability or correctness problems.
- Do not require comments that merely restate private field names.
- Do not require `final` on every method parameter.
- Constructor parameters may use the same names as fields when assignments use
  `this.field = field`.
- Source lines should normally remain within 120 characters.
- Numeric literals should become named constants when they represent shared or
  non-obvious domain rules; this is handled through review rather than a
  blanket Checkstyle rule.
- Suppressions require a documented project-specific reason.

## Permanent and audit rules

The main `checkstyle.xml` file contains only rules adopted as permanent project
policy. In addition to formatting and general correctness checks, the project
currently enforces class finality where construction prevents subclassing,
field visibility, method Javadocs, and explicit default handling in switch
statements.

Potentially useful but noisy checks may be enabled temporarily for a focused
audit. `MagicNumber`, `ParameterNumber`, and `FinalParameters` have been
reviewed and are not part of the permanent rule set:

- Numeric literals are reviewed according to their meaning; local pixel
  dimensions, font sizes, and explicit test values do not automatically need
  named constants.
- Parameter counts are treated as design-review signals rather than absolute
  violations, especially for snapshots and other data carriers.
- Parameters are not required to be declared `final` throughout the project.

Temporary audit modules should be removed after the review rather than left as
commented-out entries in `checkstyle.xml`.

## Suppressions

The `suppressions.xml` file is reserved for narrow, justified exceptions to
rules that remain active in `checkstyle.xml`. It is not a list of disabled or
rejected rules. A rule that is not project policy should simply be absent from
the main configuration.

Each suppression should be limited to the smallest practical scope and should
include a comment explaining why the normal rule is inappropriate at that
location. Suppressions should not be used merely to preserve an unresolved
violation or to hide all findings from a noisy rule.

## Running Checkstyle

Generate the HTML report:

```maven
mvn checkstyle:checkstyle
```
    

Run the build-enforced check:

```maven
mvn verify
```

Generated reports and caches under `target/` are not committed.
