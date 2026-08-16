# Lambda Inspector

[![Build](https://github.com/OurGiant/lambda-inspector/actions/workflows/build.yml/badge.svg)](https://github.com/OurGiant/lambda-inspector/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/OurGiant/lambda-inspector?label=Release)](https://github.com/OurGiant/lambda-inspector/releases/latest)
[![License: MIT](https://img.shields.io/github/license/OurGiant/lambda-inspector)](LICENSE)
[![Java 24](https://img.shields.io/badge/Java-24-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Platforms](https://img.shields.io/badge/platform-Linux%20%7C%20macOS%20%7C%20Windows-blue)](#build)

A Java Swing desktop application for browsing and test-invoking AWS Lambda functions. Connects using local AWS profiles, in the same style as [dynamodb-client](https://github.com/OurGiant/dynamodb-client): an active-profile-aware connection dialog rather than a config file to hand-edit.

## Features

- **Active-profile-aware connection dialog**: Select an AWS profile and region, with a Test Connection check against STS showing Active/Inactive status and the resolved account ID
- **Function browsing**: Paginated table of the connected profile's Lambda functions (name, runtime, memory, timeout, last modified), 50 at a time via a Load More button, with a Refresh action
- **End-of-life runtime flagging**: The Runtime column is colored against AWS's published Lambda runtime deprecation schedule — red for deprecated, amber for approaching end of life within AWS's 180-day notice window — visible across the whole list at a glance
- **Function configuration detail**: Double-click a row (or select it and click View Details) for a read-only view of description, handler, memory/timeout, architecture, IAM role, layers, VPC config, and environment variables (values masked by default, with a reveal toggle, since Lambda env vars often hold secrets). A deprecated/approaching-EOL runtime shows a call-to-action banner with the deprecation date and a link to AWS's runtime docs. Sourced entirely from the same `ListFunctions` call that populates the grid — no extra AWS API call
- **Triggers**: The config detail view also shows what can invoke the function, loaded in the background — poll-based event source mappings (SQS, DynamoDB Streams, Kinesis, ...) via `ListEventSourceMappings`, and push-based triggers (S3, API Gateway, EventBridge, SNS, ...) parsed from the function's resource-based policy via `GetPolicy`. A function with no resource-based policy at all is normal (no push-based triggers), not shown as an error
- **Test-invoke**: Select a function and click Invoke for an editable JSON payload (validated client-side before the call is made), then see the status code, cold-start indicator, duration/billed-duration/memory used, and response payload (pretty-printed if JSON) inline. Duration/billed-duration/cold-start are parsed from the invocation's execution log tail, not a second API call. A function error (e.g. an unhandled exception) surfaces as a banner, same visual treatment as the config detail view's EOL warning
- **Persistent settings**: Remembers the last-used AWS profile and region between sessions
- **FlatLaf theming**: Switchable Light/Dark/IntelliJ themes via the View menu
- **Help > About**: App version, copyright, and a manual/silent (non-blocking) check for newer GitHub releases

Planned (see [Scope](#scope)):

- Tail CloudWatch Logs for a selected function

## Scope

This is a **browse-and-invoke** tool, not a deployment tool. It never creates, updates, or deletes a Lambda function's code or configuration — the only mutating AWS action it performs is invoking a function on request, which is inherently something the function itself may do (e.g. write to a database), not something this app does directly. This mirrors dynamodb-client's "browse, don't mutate destructively" ethos, just without even the record-delete capability dynamodb-client has.

## Prerequisites

- Java 24 or higher
- AWS credentials configured at `~/.aws/credentials`
- Network access to AWS Lambda / CloudWatch Logs

## IAM Permissions

The connected AWS profile needs at least the following actions:

- `sts:GetCallerIdentity` — verifying a profile's credentials are active and showing its account ID in the window title
- `lambda:ListFunctions` — function browsing and configuration detail (the `ListFunctions` response already includes everything the detail view shows — no separate `GetFunction` call or permission is needed)
- `lambda:InvokeFunction` — test-invoke; this is the app's only mutating AWS action (see [Scope](#scope))
- `lambda:ListEventSourceMappings` — poll-based triggers (SQS, DynamoDB Streams, Kinesis, ...) in the config detail view's Triggers section
- `lambda:GetPolicy` — push-based triggers (S3, API Gateway, EventBridge, SNS, ...) in the same section, read from the function's resource-based policy
- `logs:FilterLogEvents`, `logs:DescribeLogStreams` — log tailing (planned)

This list will grow as the planned features above land; each addition is documented here alongside the code, not just implemented silently (see `.claude/skills/ship-issue/SKILL.md`).

## Build

```bash
mvn clean package
```

Produces `target/lambda-inspector-all.jar`.

## Run

```bash
java -jar target/lambda-inspector-all.jar
```

On launch, a connection dialog prompts for an AWS profile and region. These values are saved for subsequent runs.

## Project Structure

```
src/main/java/com/ourgiant/lambda/inspector/
├── Main.java               # Entry point
├── AppPreferences.java     # java.util.prefs wrapper (last profile/region, update-notified version)
├── ThemeManager.java       # FlatLaf theme selection
├── model/                  # Plain data types (ProfileActivity, FunctionSummary, ...)
├── core/                   # Swing-free domain logic (AWS profile/region resolution, connection
│                           # messages, ListFunctions/Invoke request building and response
│                           # mapping, EOL runtime classification, invoke log parsing/payload
│                           # validation/response formatting, event-source-mapping and
│                           # resource-policy trigger parsing) - no javax.swing.* dependency
├── gui/                    # MainWindow, AboutDialog, FunctionDetailDialog, InvokeDialog,
│                           # RuntimeStatusCellRenderer, and all Swing wiring - depends
│                           # one-way on core/model
└── util/                   # Shared helpers with no business meaning of their own
                            # (AppVersion, UpdateChecker, HttpClientFactory, NetworkFetchException)
```

## Dependencies

- **AWS SDK for Java v2**: Lambda and CloudWatch Logs clients, authentication, region resolution
- **FlatLaf** (+ intellij-themes, extras): application theming
- **SLF4J + Logback**: logging
- **Jackson (jackson-databind)**: parsing the GitHub releases API response for the About dialog's update check, and the test-invoke payload editor's JSON validation/response pretty-printing
- **JUnit 5 + Mockito**: testing

## License

See LICENSE file for details.
