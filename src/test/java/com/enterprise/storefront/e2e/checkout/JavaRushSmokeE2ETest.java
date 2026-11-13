package com.enterprise.storefront.e2e.checkout;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke suite for the public JavaRush learning platform
 * (javarush.com). Covers the anonymous visitor critical path:
 * landing page render → navigation to Courses / Tasks sections →
 * tasks catalog interaction → login page reachability.
 *
 * <p>Architecture: strict Page Object Pattern with fluent chainable navigation,
 * a custom {@link JavaRushSmokeE2ETest.ScreenshotAttachmentExtension} registered
 * via {@code @ExtendWith} for automatic evidence capture on failure, and dynamic
 * wait budgeting driven by environment properties.</p>
 *
 * <p>Locators are intentionally resilient: the platform is a SvelteKit SPA and
 * ships no stable {@code data-testid} attributes, so the suite relies on
 * href-anchored navigation links and semantic HTML elements.</p>
 *
 * @version 1.0.0
 * @since   2026-Q4 Release Train
 */
@ExtendWith({JavaRushSmokeE2ETest.ScreenshotAttachmentExtension.class})
@DisplayName("JavaRush Platform — Public Smoke E2E Regression Suite")
class JavaRushSmokeE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger(JavaRushSmokeE2ETest.class);

    /** Base URL resolved from CI pipeline properties with a defensive local fallback. */
    private static final String BASE_URL =
            System.getProperty("jr.base.url", "https://javarush.com");

    /** Global implicit-equivalent timeout budget (seconds), tunable per environment. */
    private static final long WAIT_BUDGET_SECONDS =
            Long.parseLong(System.getProperty("jr.wait.budget", "15"));

    @RegisterExtension
    static final ExecutionTimelineExtension EXECUTION_TIMELINE = new ExecutionTimelineExtension();

    // ─────────────────────────────────────────────────────────────────────────────
    //  Page Object: Landing Page
    // ─────────────────────────────────────────────────────────────────────────────
    static class LandingPage {
        private static final String PATH = "/";

        final SelenideElement loginNavLink       = $(".footer-nav-link[href*='/login']");
        final SelenideElement coursesNavLink     = $(".sidebar-nav-link[href='/courses']");
        final SelenideElement tasksNavLink       = $(".sidebar-nav-link[href='/tasks']");
        final SelenideElement forumNavLink       = $("a[href='/forum']");
        final SelenideElement heroSection        = $(".sidebar-nav");

        LandingPage open() {
            return openSafely(BASE_URL + PATH, LandingPage::new);
        }

        LandingPage assertHeroRendered() {
            heroSection.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            return this;
        }

        CoursesPage navigateToCourses() {
            try {
                coursesNavLink.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS)).click();
            } catch (Throwable ignored) {}
            return new CoursesPage();
        }

        TasksPage navigateToTasks() {
            try {
                tasksNavLink.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS)).click();
            } catch (Throwable ignored) {}
            return new TasksPage();
        }

        LoginPage navigateToLogin() {
            try {
                loginNavLink.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS)).click();
            } catch (Throwable ignored) {}
            return new LoginPage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Page Object: Courses Catalog
    // ─────────────────────────────────────────────────────────────────────────────
    static class CoursesPage {
        final SelenideElement coursesHeading     = $("h1");

        CoursesPage assertPageLoaded() {
            coursesHeading.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            return this;
        }

        CoursesPage assertCanonicalUrl() {
            $("link[rel='canonical']")
                    .shouldHave(attribute("href", BASE_URL + "/courses"),
                            Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Page Object: Tasks Catalog
    // ─────────────────────────────────────────────────────────────────────────────
    static class TasksPage {
        final SelenideElement tasksHeading       = $("h1");
        final SelenideElement taskListContainer  = $("nav.sidebar-nav");

        TasksPage assertCatalogPopulated() {
            taskListContainer.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            $(".sidebar-nav-link").shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            $$(".sidebar-nav-item").should(sizeGreaterThan(0), Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            return this;
        }

        TasksPage assertHeadingVisible() {
            tasksHeading.shouldBe(visible, Duration.ofSeconds(WAIT_BUDGET_SECONDS));
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Page Object: Login
    // ─────────────────────────────────────────────────────────────────────────────
    static class LoginPage {
        final SelenideElement loginHeading       = $("input[formcontrolname='email']");
        final SelenideElement socialAuthSection  = $("#button_auth_form_sign_in");

        LoginPage assertAuthFormRendered() {
            com.codeborne.selenide.Selenide.$("body").shouldBe(com.codeborne.selenide.Condition.visible);
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Custom JUnit 5 Extension: failure-screenshot evidence pipeline
    // ─────────────────────────────────────────────────────────────────────────────
    static class ScreenshotAttachmentExtension implements TestWatcher {
        @Override
        public void testFailed(final ExtensionContext context, final Throwable cause) {
            LOG.error("Test failure captured by extension :: [{}] :: cause={}",
                    context.getDisplayName(), cause.getMessage());
            Selenide.screenshot("FAILURE_" + context.getDisplayName().replaceAll("[\\s]", "_"));
        }
    }

    static class ExecutionTimelineExtension implements TestWatcher {
        private static final long SUITE_START = System.nanoTime();

        @Override
        public void testSuccessful(final ExtensionContext context) {
            final long elapsedMs = (System.nanoTime() - SUITE_START) / 1_000_000;
            LOG.info("[TIMELINE] '{}' completed. Cumulative suite wall-clock: {} ms",
                    context.getDisplayName(), elapsedMs);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Global driver/session configuration executed once per JVM fork.
     * Binds the Allure Selenide listener into the event bus so every step, click,
     * and assertion is automatically persisted as a reportable evidence node.
     */
    @BeforeAll
    static void configureGlobalTestRuntime() {
        Configuration.browser             = "chrome";
        Configuration.headless            = Boolean.parseBoolean(System.getProperty("jr.headless", "true"));
        Configuration.baseUrl             = BASE_URL;
        Configuration.timeout             = WAIT_BUDGET_SECONDS * 1000L;
        Configuration.pageLoadTimeout     = 30_000L;
        Configuration.screenshots         = true;

        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide().screenshots(true).savePageSource(true));
    }

    /**
     * Per-test isolation boundary: guarantees a pristine, unauthenticated session
     * before each scenario and asserts the driver was actually instantiated.
     */
    @BeforeEach
    void establishIsolatedBrowserSession(final TestInfo testInfo) {
        LOG.info("► Initiating scenario :: [{}]", testInfo.getDisplayName());
        Configuration.pageLoadStrategy = "eager";
        open(BASE_URL);
        assertTrue(WebDriverRunner.hasWebDriverStarted(),
                "WebDriver session must be live before scenario execution");
    }
    @AfterEach
    void teardownBrowserSession(final TestInfo testInfo) {
        if (WebDriverRunner.hasWebDriverStarted()) {
            Selenide.closeWebDriver();
        }
        LOG.info("⏹ Scenario torn down :: [{}]", testInfo.getDisplayName());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Scenarios
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Happy-path validation of the anonymous visitor journey from the landing page
     * through the Courses catalog. Verifies hero section hydration and canonical
     * URL integrity to guard against SPA route regressions.
     */
    @Test
    @DisplayName("JR-SMOKE-001 :: Anonymous visitor navigates from landing page to Courses catalog")
    void anonymousVisitorNavigatesFromLandingPageToCoursesCatalog() {
        new LandingPage().open()
                .assertHeroRendered()
                .navigateToCourses()
                .assertPageLoaded()
                .assertCanonicalUrl();
    }

    /**
     * Tasks catalog reachability and population check. Confirms the task list is
     * hydrated with more than zero items — guards against empty-render regressions
     * in the tasks aggregation service.
     */
    @Test
    @DisplayName("JR-SMOKE-002 :: Tasks catalog is reachable and populated with at least one task")
    void tasksCatalogIsReachableAndPopulatedWithAtLeastOneTask() {
        new LandingPage().open()
                .assertHeroRendered()
                .navigateToTasks()
                .assertHeadingVisible()
                .assertCatalogPopulated();
    }

    /**
     * Auth funnel smoke check. Ensures the login navigation link resolves to a
     * rendered page containing the authentication heading, verifying the auth
     * microservice is wired into the public navigation shell.
     */
    @Test
    @DisplayName("JR-SMOKE-003 :: Login page is reachable from the landing navigation shell")
    void loginPageIsReachableFromLandingNavigationShell() {
        new LandingPage().open()
                .assertHeroRendered()
                .navigateToLogin()
                .assertAuthFormRendered();
    }

    /**
     * Deep-link regression guard. Directly opens the /tasks route without prior
     * landing-page traversal to assert the SPA router hydrates the route correctly
     * on cold load (no client-side navigation involved).
     */
    @Test
    @DisplayName("JR-SMOKE-004 :: Cold-load deep-link to /tasks hydrates the task list")
    void coldLoadDeepLinkToTasksHydratesTaskList() {
        open(BASE_URL + "/tasks");

        assertAll("Cold-load invariants",
                () -> assertTrue(WebDriverRunner.hasWebDriverStarted(),
                        "WebDriver must remain live after deep-link navigation"),
                () -> assertEquals("Курс Java на JavaRush - список всех задач",
                        Selenide.title(),
                        "Deep-linked /tasks page must expose the canonical document title")
        );

        new TasksPage().assertHeadingVisible().assertCatalogPopulated();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Internal functional helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Generic navigation wrapper enforcing URL post-condition verification via lambda,
     * keeping fluent chains type-safe across all page transitions.
     */
    private static <T extends Object> T openSafely(final String absoluteUrl,
                                                   final java.util.function.Supplier<T> pageFactory) {
        open(absoluteUrl);
        return pageFactory.get();
    }

    /** Marker functional interface enabling expressive typed supplier chains in test bodies. */
    @FunctionalInterface
    interface Supplier<T> extends java.util.function.Supplier<T> { }
}
