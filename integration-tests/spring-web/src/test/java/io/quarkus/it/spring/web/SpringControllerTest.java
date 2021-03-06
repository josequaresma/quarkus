package io.quarkus.it.spring.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class SpringControllerTest {

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        String path = "/api/securedMethod";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, 200,
                Optional.of("accessibleForAdminOnly"));
    }

    @Test
    public void testAllowedForAdminOrViewer() {
        String path = "/api/allowedForUserOrViewer";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("aurea", "auri"), path, 403, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 200,
                Optional.of("allowedForUserOrViewer"));
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("george", "geo"), path, 200,
                Optional.of("allowedForUserOrViewer"));
    }

    @Test
    public void testWithAlwaysFalseChecker() {
        String path = "/api/withAlwaysFalseChecker";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("george", "geo"), path, 403, Optional.empty());
    }

    @Test
    public void testPreAuthorizeOnController() {
        String path = "/api/preAuthorizeOnController";
        assertForAnonymous(path, 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, 200,
                Optional.of("preAuthorizeOnController"));
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("aurea", "auri"), path, 200,
                Optional.of("preAuthorizeOnController"));
    }

    @Test
    public void shouldAccessAllowed() {
        assertForAnonymous("/api/accessibleForAllMethod", 200, Optional.of("accessibleForAll"));
        assertForUsers("/api/accessibleForAllMethod", 200, Optional.of("accessibleForAll"));
    }

    @Test
    public void shouldRestrictAccessOnClass() {
        assertForAnonymous("/api/restrictedOnClass", 401, Optional.empty());
        assertForUsers("/api/restrictedOnClass", 200, Optional.of("restrictedOnClass"));
    }

    @Test
    public void shouldFailToAccessRestrictedOnClass() {
        assertForAnonymous("/api/restrictedOnMethod", 401, Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), "/api/restrictedOnMethod", 403,
                Optional.empty());
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), "/api/restrictedOnMethod", 200,
                Optional.of("restrictedOnMethod"));
    }

    private void assertForAnonymous(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given(), path, status, content);
    }

    private void assertForUsers(String path, int status, Optional<String> content) {
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("stuart", "test"), path, status, content);
        assertStatusAndContent(RestAssured.given().auth().preemptive().basic("scott", "jb0ss"), path, status, content);
    }

    private void assertStatusAndContent(RequestSpecification request, String path, int status, Optional<String> content) {
        ValidatableResponse validatableResponse = request.when().get(path)
                .then()
                .statusCode(status);
        content.ifPresent(text -> validatableResponse.body(Matchers.equalTo(text)));
    }

    @Test
    public void testJsonResult() {
        RestAssured.when().get("/greeting/json/hello").then()
                .contentType("application/json")
                .body(containsString("hello"));
    }

    @Test
    public void testJsonResultFromResponseEntity() {
        RestAssured.when().get("/greeting/re/json/hello").then()
                .contentType("application/json")
                .body(containsString("hello"));
    }

    @Test
    public void testJsonResult2() {
        RestAssured.when().get("/greeting/json/hello?suffix=000").then()
                .contentType("application/json")
                .body(containsString("hello000"));
    }

    @Test
    public void testInvalidJsonInputAndResult() {
        RestAssured.given().contentType("application/json").body("{\"name\":\"\"}").post("/greeting/person").then()
                .statusCode(400);
    }

    @Test
    public void testJsonInputAndResult() {
        RestAssured.given().contentType("application/json").body("{\"name\":\"George\"}").post("/greeting/person").then()
                .contentType("application/json")
                .body(containsString("hello George"));
    }

    @Test
    public void testFirstResponseStatusHoldingException() {
        RestAssured.when().get("/exception/first").then()
                .contentType("text/plain")
                .body(containsString("first"))
                .statusCode(500);
    }

    @Test
    public void testSecondResponseStatusHoldingException() {
        RestAssured.when().get("/exception/second").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(503);
    }

    @Test
    public void testExceptionHandlerVoidReturnType() {
        RestAssured.when().get("/exception/void").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(400);
    }

    @Test
    public void testExceptionHandlerWithoutResponseStatusOnExceptionOrMethod() {
        RestAssured.when().get("/exception/unannotated").then()
                .contentType("text/plain")
                .body(isEmptyString())
                .statusCode(204);
    }

    @Test
    public void testExceptionHandlerResponseEntityType() {
        RestAssured.when().get("/exception/responseEntity").then()
                .contentType("application/json")
                .body(containsString("bad state"), containsString("responseEntity"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testExceptionHandlerResponseEntityTypeFromVoidReturningMethod() {
        RestAssured.when().get("/exception/responseEntityFromVoidReturningMethod").then()
                .contentType("application/json")
                .body(containsString("bad state"), containsString("responseEntity"))
                .statusCode(402)
                .header("custom-header", "custom-value");
    }

    @Test
    public void testExceptionHandlerPojoEntityType() {
        RestAssured.when().get("/exception/pojo").then()
                .contentType("application/json")
                .body(containsString("hello from error"))
                .statusCode(417);
    }

    @Test
    public void testControllerMethodWithVoidReturnTypeAndExceptionHandlerPojoEntityType() {
        RestAssured.when().get("/exception/pojoWithVoidReturnType").then()
                .contentType("application/json")
                .body(containsString("hello from error"))
                .statusCode(417);
    }

    @Test
    public void testRestControllerWithoutRequestMapping() {
        RestAssured.when().get("/hello").then()
                .body(containsString("hello"));
    }

    @Test
    public void testResponseEntityWithIllegalArgumentException() {
        RestAssured.when().get("/exception/re").then()
                .contentType("application/json")
                .body(containsString("hello from error"))
                .statusCode(402);
    }
}
