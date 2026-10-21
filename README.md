# QA Java Lab

[![GitHub Actions](https://github.com/antondorovs/qa-java-automation/actions/workflows/tests.yml/badge.svg)](https://github.com/antondorovs/qa-java-automation/actions/workflows/tests.yml)
[![GitLab CI](https://gitlab.com/antondorovs/qa-java-automation/badges/main/pipeline.svg)](https://gitlab.com/antondorovs/qa-java-automation/-/pipelines)

Java tests for the [DummyJSON product API](https://dummyjson.com/docs/products),
the [SauceDemo storefront](https://www.saucedemo.com/), and an isolated PostgreSQL
database. Source code is maintained on [GitHub](https://github.com/antondorovs/qa-java-automation)
and [GitLab](https://gitlab.com/antondorovs/qa-java-automation).

## Test coverage

- **API:** product retrieval, pagination, categories, empty search results,
  simulated create/update/delete, missing products and unknown routes. Tests
  check status codes, JSON content type and response fields using Java records.
- **UI:** login, invalid credentials, locked account, cart changes, price sorting,
  checkout totals and required customer fields. Each test starts a fresh browser.
- **Database:** insert/select with a join, update, delete, unique emails, foreign
  keys, positive totals and allowed statuses. Each test rolls back its transaction.

These are three independent suites. PostgreSQL is not the backend of either
public service. DummyJSON does not persist writes; the API tests also check that
stored data stays unchanged. Public API/UI availability can affect test results.

## Technology Stack

| Area | Tools |
| --- | --- |
| Language and build | Java 21, Gradle Wrapper |
| Test runner | JUnit 5, parameterized tests and tags |
| API | REST Assured, Jackson |
| UI | Selenide, Chrome, Page Objects |
| Database | PostgreSQL 17, JDBC, Testcontainers |
| Reports | Allure Report, Gradle HTML and JUnit XML |
| Execution | Docker, GitHub Actions, GitLab CI |

Dependency versions are pinned in `build.gradle`; the Wrapper distribution has
a SHA-256 checksum.

## Project Structure

```text
src/test/java/io/github/antondorovs/qa/
  api/          HTTP client, request/response specifications and API tests
  ui/           Browser lifecycle, tests and pages/
  db/           JDBC fixtures and PostgreSQL tests
  models/       Request and response records
  config/       Properties and environment configuration
  utils/        JSON fixture reader
  testdata/     Data created for database tests
src/test/resources/
  testdata/     JSON request fixtures
  db/           SQL schema
  *.properties Test defaults and Allure settings
.github/workflows/tests.yml
.gitlab-ci.yml
compose.yaml    Optional Selenium Chrome service
```

## Setup and run

Install JDK 21 and set `JAVA_HOME` to its directory. API tests need internet access;
local UI tests also need Chrome. DB tests need a running Docker Desktop (Linux
containers) or Docker Engine. No local PostgreSQL installation is required.

```bash
git clone https://github.com/antondorovs/qa-java-automation.git
cd qa-java-automation
./gradlew testClasses
```

The Wrapper downloads Gradle and the dependencies. On Windows PowerShell, replace
`./gradlew` with `.\gradlew.bat` in every command.

| Run | Command |
| --- | --- |
| API tests | `./gradlew apiTest` |
| UI tests, headless Chrome | `./gradlew uiTest` |
| DB tests | `./gradlew dbTest` |
| All tests once | `./gradlew test` |
| UI with a visible browser | `./gradlew uiTest -Dheadless=false` |
| One test class | `./gradlew uiTest --tests '*CheckoutTest'` |

Test tasks always execute, even when the source code has not changed. Testcontainers
starts PostgreSQL on an available port, loads `db/schema.sql`, and removes its
container after the suite. Docker being unavailable fails the DB suite.

Selenium normally resolves ChromeDriver automatically. If the driver download is
blocked, download a matching driver from [Chrome for Testing](https://googlechromelabs.github.io/chrome-for-testing/)
and add its directory to `PATH`, or use the Docker browser below.

## Configuration

Priority: JVM properties, environment variables, ignored `local.properties` in
the project root, then `src/test/resources/test.properties`.

| Property | Environment variable | Default |
| --- | --- | --- |
| `api.baseUrl` | `API_BASE_URL` | `https://dummyjson.com` |
| `ui.baseUrl` | `UI_BASE_URL` | `https://www.saucedemo.com` |
| `browser` | `BROWSER` | `chrome` |
| `headless` | `HEADLESS` | `true` |
| `ui.timeout` | `UI_TIMEOUT` | `10000` milliseconds |
| `selenide.remote` | `SELENIDE_REMOTE` | Empty: local browser |

For example, put `headless=false` in `local.properties`. Chrome is the verified
browser. SauceDemo's published test accounts are test data, not production
credentials. Database credentials belong only to the disposable container.

## Docker browser

```bash
docker compose up -d --wait browser
./gradlew uiTest -Dselenide.remote=http://localhost:4444
docker compose down
```

The browser endpoint is bound to localhost. Gradle runs on the host; database
tests manage their own container independently of Compose.

## Reports

```bash
./gradlew allureReport
./gradlew allureServe
```

These commands build/open the results of the last test invocation without running
tests again. Allure results are cleared once at the start of each invocation that
runs tests. To collect all layers, use `./gradlew test` or
`./gradlew apiTest uiTest dbTest` in one invocation.

Allure includes HTTP requests/responses and Selenide steps, with screenshots and
page source for failed Selenide checks. Raw results are in `build/allure-results/`;
the report is in `build/reports/allure-report/allureReport/`. Gradle HTML reports
are in `build/reports/tests/`. All generated files stay outside Git.

## CI

GitHub Actions runs separate API, UI and DB jobs on pushes to `main`, pull requests
and manual runs. Each uses Java 21 and the same Wrapper commands as local runs.
UI uses headless Chrome; Testcontainers uses the runner's Docker engine.

GitLab CI runs the same suites on `main`, merge requests and manual pipelines.
UI connects to a Selenium service. DB uses a Docker-in-Docker service and requires
a runner that supports privileged Docker services, such as a compatible GitLab
hosted Linux runner. No shared database or production secrets are required.

Both pipelines retain Allure, HTML and JUnit results for 14 days, including failed
runs. Test failures fail the job; assertion failures are not automatically retried.

After cloning from GitHub, add the second remote once:

```bash
git remote add gitlab https://gitlab.com/antondorovs/qa-java-automation.git
```

After a reviewed commit, publish the same history to both remotes:

```bash
git push origin main
git push gitlab main
```
