---
name: verify
description: How to build, launch, and drive Lambda Inspector to verify a Swing UI change actually works on this dev setup. Use before trusting the generic verify-java-swing skill's screenshot step; read that skill too for the underlying techniques (modal-dialog deadlock, synthetic MouseEvent dispatch, process safety).
---

# Verifying Lambda Inspector

This is the project-specific companion to the generic `verify-java-swing`
skill (techniques) and `java-swing-project-setup` (build/structure
standard this project follows). Read those first — this file is what to
actually type for *this* project.

## Build here, run there

Maven only exists in the Docker container, not on the host:

```bash
docker exec festive_bardeen bash -c "cd /projects/OHI/lambda-inspector && mvn -q package -DskipTests"
```

If `festive_bardeen` doesn't respond, find the current container:
`docker ps -a --format '{{.Names}} {{.Status}} {{.Image}}'` and
`docker start <name>` if stopped — the name can drift across sessions.

`/projects` is bind-mounted from the host's `~/projects`, so the jar lands
at `target/lambda-inspector-all.jar`, visible on the host. The container is
headless (no `DISPLAY`) — run the jar on the **host**, not inside the
container, or it dies at `JFrame` construction with `HeadlessException`.

```bash
java -jar target/lambda-inspector-all.jar
```

Main class: `com.ourgiant.lambda.inspector.Main`.

## First-run state

AWS profile/region and the last-notified update version are stored via
`java.util.prefs` at `/com/ourgiant/lambda/inspector/gui` (see
`AppPreferences`). Tests never touch `~/.aws` directly — `AwsProfiles`
reads from the directory named by the `lambda.inspector.awsConfigDir`
system property when set (surefire sets it to
`target/test-aws-config`; see `pom.xml`).

## Screenshots work here

Confirmed 2026-08-15: `Robot.createScreenCapture(...)` returns a real,
non-black capture on this dev host (DISPLAY `:1`) — unlike the Wayland
sandboxes the generic `verify-java-swing` skill warns about, this is a
real X11 desktop session. Don't assume black-screenshot fallback is
needed here without checking first.

## Component lookup gotcha: editable JComboBox contains its own JTextField

The connection dialog's profile field is an editable `JComboBox<String>`
whose live editor component is itself a `JTextField`, added as a child
of the combo box. A naive "find the Nth `JTextField` in the dialog"
reflection walk will find the combo's internal editor before the
standalone region `JTextField` sibling, silently writing into the wrong
field. Skip recursing into `JComboBox` internals when searching for any
other component type.
