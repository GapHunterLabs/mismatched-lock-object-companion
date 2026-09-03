# Mismatched Lock Object Companion

Flags a `synchronized` block whose lock object is PROVEN to never be
the same as another lock guarding the same field elsewhere in the
class.

## Why it exists

Broken mutual exclusion that looks like real thread-safety but isn't
-- SEI CERT LCK06-J/LCK00-J. FindBugs has related detectors ("lock
does not guard", instance-vs-static lock mismatches) -- bytecode/CI
only, not an inline IDE inspection. No dedicated Marketplace plugin
found.

## Why built this way

- **Real alias analysis, proving NON-identity** -- the opposite
  direction from this catalog's own `deadlock-lock-order-companion`
  (which proves two locks ARE the same object to find a cycle). Here,
  the goal is to prove two lock references are NEVER the same object.
- **The strongest identity guarantee short of full points-to
  analysis**: both candidate lock fields must be `final`, each
  initialized to its OWN separate `new` expression. Two different
  `new` expressions can never produce the same reference -- confirmed
  as the same reasoning real alias-analysis research tools (Chord) use
  for final fields.
- **A false positive here would be worse than a missed finding** --
  declaring two objects "provably different" when they might actually
  be the same is the one mistake this mechanism can never make, which
  is why the check is restricted to the single shape where the
  guarantee is absolute.

## v0.1 scope — stated honestly, not exhaustively

- Only an unqualified lock reference (`synchronized(lockA)`, implicit
  `this`) -- `synchronized(other.lockA)` is out of scope.
- Only a field's OWN direct initializer
  (`private final Object lockA = new Object();`) -- a field assigned
  in a constructor body is never treated as provably distinct.

## Usage

Open a Java class with two `synchronized` blocks guarding the same
field but using two different `final`, `new`-initialized lock fields
-- the second (mismatching) lock expression shows a warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
