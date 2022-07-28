# Integration and Acceptance Testing in Spring

## Learning Goals

- Create integration tests in Spring.
- Define acceptance testing.

## Introduction

Now that we've seen how to create unit testing that is completely independent of
the Spring Framework, there are 2 additional levels of testing we need to
explore.

## Integration Testing

We will start with basic integration testing, which will allow us to test
whether our endpoints are properly exposed, but will stop short of testing
**all** the infrastructure that the Spring Framework provides.

Let's start by creating a new Test class - this time, we will add `Integration`
to the name of the test class to differentiate it from our previous `UnitTest`
class:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hello() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid Message")));
    }
}
```

As before, the initial version of our test is intentionally written to fail by
checking for the wrong return from our controller method.

But before we run this test, let's inspect what it does:

- `@WebMvcTest` annotation:
  - We are asking the Spring Framework to initialize its Web Context **only**
    and we're asking that Web Context to only include this specific controller.
    This is helpful because it a) does not initialize other aspect of Spring
    Framework (database connections, ...) and b) does not initialize other
    controllers - in a real-life application, you may have a large number of
    controllers associated with your application context.
- With the `@WebMvcTest` annotation, we get a bean that can get autowired by
  Spring to give us access to a MockMvc instance that we can then use to make
  actual `http` calls to our end points
- You'll notice that we are chaining multiple calls on the `mockMvc` instance we
  created - this makes it easier and more readable than assigning the return
  value of each method call and then using that value to make the next method
  call
  - The `perform()` method lets us pass in an `http` verb, along with the
    appropriate parameters for that call. In this case, we are asking for a
    `GET` request to be executed and we pass in the URL to which it should be
    submitted. Spring will then take that URL and find the endpoint we have
    defined in the `hello()` method of our controller
  - The `andDo(print())` call asks `mockMvc` to results of the `perform()` call
    to the console - we can use this to diagnose potential issues.
  - The `andExpect(status().isOk())` call tells mockMvc that we want an `HTTP`
    status code of `200` to be returned as a result of the `perform()` call
  - The `andExpect(content().string(containsString("Invalid Message")))` tells
    mockMvc that we want the content of the response of the `perform()` call to
    contain the string "Invalid Message".

Note: `containsString()` is a tricky assertion to use for testing because it
will return `true` (and therefore the corresponding test will pass) even if the
text does not match **exactly**. This can be very helpful in cases where we only
care about part of the message, but should not be substituted for cases where we
actually need the strings to be exactly the same. In a later example, we will
use the `equalTo()` assertion for a precise match.

Running this integration test in its current form will give the following error:

![Integration Test Error](spring-testing-integration-test-error.png)

Which indicates 2 problems:

1. As expected, the String returned by the endpoint does not match the string we
   told the test to expect
2. But there is also another issue: the actual string returned is also not the
   "Hello <parameter-value>" that we expected , it's actually "Hello null",
   which indicates an issue with how our parameter is passed in

This second issue shows the value of integration testing vs unit testing. Our
unit test passes because the `hello(String name)` method is able to correctly
receive the `name` parameter and use it to construct its response. However, this
integration test clearly shows that the `name` parameter is not correctly being
received by the `hello(String name)` method when that method is called as an
endpoint by the Spring Framework. We wouldn't have found this problem until we
manually tested the endpoint if we didn't have this integration test.

The actual issue here is that we need an annotation to tell the Spring Framework
how to get the value of the `name` parameter from an incoming web request, so we
need to change the `hello()` method as follows:

```java
package com.flatiron.spring.FlatironSpring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello(@RequestParam(name = "targetName", defaultValue = "Stephanie") String name) {
        return String.format("Hello %s", name);
    }

}
```

We've added the `RequestParam` annotation to the `String name` parameter, which
tells Spring to look for a parameter in the incoming web request with the name
`targetName` and set the variable `name` to the value of that variable. It also
tells Spring to use "Stephanie" as the default value for that parameter if it's
not passed into the web request.

Note: we are intentionally using a different name for the web request parameter
name from the name of the variable in the `hello()` method signature, just to
emphasize that they do not need to be the same, and they are indeed 2 different
things that are defined in different places, and that the value of `targetName`
is read Spring and used to initialize `name`.

With that change, we can now run our integration test again, and see it fail
with a "better" error message this time:

![Integration Test Better Error](spring-testing-integration-test-better-error.png)

Our integration test is still not sending a parameter in, but since we're now
defaulting the value of `name` to "Stephanie", the string we're getting back no
longer has `null` in it, and instead has "Stephanie" in it. This is the valid
behavior when no parameter is passed in through the web request.

Let's modify our integration test to recognize that this is desired behavior:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGreetDefault() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello Stephanie")));
    }
}
```

This test will pass, which is great. But our test suite is incomplete, as we
have not yet tested passing in an actual value for the name that should be
greeted. Let's do that now:

```java
    @Test
    void shouldGreetByName() throws Exception {
        String greetingName = "Jamie";
        mockMvc.perform(get("/hello")
                .param("targetName", greetingName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello " + greetingName)));
    }
```

Here is what we've done differently:

- We have changed the `perform()` call to chain it with a `param()` call
- The `param()` call allows us to set up request parameter on the request
- Here we are telling the request that it should have a request parameter named
  `targetName` and that its value should be the value of the `greetingName`
  variable

Note that we are using 3 different variable names to refer to basically the same
thing, i.e. the name that we want the `hello()` method to use in its greeting.
In a normal application, all these variables would have the same name to make it
easier for people to follow the code. We're making them different names here
intentionally, however, so that it is as clear as possible where each variable
is used and how the corresponding values make their way through the Spring
Framework.

### Start Integration Testing

We have now gotten to the point where the web part of the Spring Framework is
exercised with our latest test, which gives us confidence that our endpoints are
exposed properly and that we are able to get data to them as we expected.

The next step is to initialize the entire Spring Framework and make a request as
we were truly coming from an external client.

Since our current endpoint doesn't actually integrate with anything else, we
will start by adding some functionality to it, so we can look at a slightly more
complicated scenario.

#### Dad Jokes

Let's extend our existing `hello()` endpoint by making it return a random Dad
joke in addition to the greeting it currently returns. Don't worry, we don't
have to come up with our own lame but somehow still funny jokes for this
exercise - we will use an existing API from `https://icanhazdadjoke.com/api`.
This is a free API that doesn't require authentication, so it will be give us a
simple (and fun) example to work with.

You can test the joke API with this simple command:

```
curl https://icanhazdadjoke.com/
```

It will return a random, but always kind of lame, joke:

```
Velcro… What a rip-off.
```

We could implement the functionality to interface with this API inside of our
existing `hello()` method, but that would a) make this one method be responsible
for more than one thing and b) consequently make it more difficult to unit test.
So instead, we will create a new service class that will be responsible for
interfacing with the API:

```java
package com.flatiron.spring.FlatironSpring;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class JokeService {
    public String getDadJoke() {
        return null;
    }
}
```

The `@Service` annotation associated with the class defines this class as a
service component within the Spring Framework.

Note that this current implementation doesn't actually interface with our target
API. Before we implement that functionality, let's first introduce the service
into our `hello()` method in our controller class and update our unit test
accordingly:

```java
@RestController
public class HelloController {

    private JokeService jokeService;

    public HelloController(JokeService jokeService) {
        this.jokeService = jokeService;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(name = "targetName", defaultValue = "Stephanie") String name) {
        String greeting = "Hello " + name;
        greeting += "<br/>";
        greeting += "Dad joke of the moment: " + jokeService.getDadJoke();
        return greeting;
    }

}
```

Here is a breakdown of the updates to the controller:

1. We create a private instance variable `jokeService` to get access to the
   service's methods
2. Since our `JokeService` class has the `@Service` annotation, the Spring
   framework will take care of passing in an instance of the joke service into
   the `HelloController()` constructor
3. We use the `jokeService` object to get the random joke and build a return
   String that includes it

Now we need to update our Unit test because it's still trying to build an
instance of the `HelloController` class without passing it a `JokeService`
instance. Here is the updated unit test:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class HelloControllerUnitTest {

    @Test
    void shouldReturnGreeting() {
        JokeService jokeService = Mockito.mock(JokeService.class);
        String dadJoke = "Did you hear about the new restaurant on the moon? The food is great, " +
                "but there’s just no atmosphere.";
        HelloController helloController = new HelloController(jokeService);
        when(jokeService.getDadJoke()).thenReturn(dadJoke);
        String name = "Jamie";
        String expected = "Hello " + name + "<br/>" +
                "Dad joke of the moment: " +
                dadJoke;
        String actual = helloController.hello(name);
        assertEquals(expected, actual);
    }
}
```

Let's break it down:

1. We define a `jokeService` object of type `JokeService`
2. We use `Mockito` to mock the behavior of the `getDadJoke()` method in that
   service (more on this below)
3. We set values for the `actual` and `expected` variables and assert that they
   match to complete our unit test

`Mockito` is a framework that lets us "mock" the behavior of methods that the
method we are testing are dependent on. This is important for unit testing
because we are only interested in testing the behavior of one method, not the
behavior of the other methods that this one method might depend on.

In our example, we want to test the behavior of the `hello()` method, not the
behavior of the `getDadJoke()` method that the `hello()` method depends on.
Mockito lets us create a "fake" service and hard code what we want the
`getDadJoke()` method to return, so that we know that no matter what happens to
the actual implementation of that method , our unit test will not break:

1. `Mockito.mock()` allows us to specify the class for which we need to create
   an instance, in this case it's `JokeService.class`
2. `when().thenReturn()`: this construct allows us to tell `Mockito` what to
   return when a specific method of our mock object is called - this is what
   hardcodes that specific response for every time that method is called

There are a couple of crucial things to note here:

1. The `Mockito` framework is able to substitute a "mock"/"fake" implementation
   of the `JokeService` in the instance of the `HelloController` we are using in
   our unit test because we have defined `JokeService` as a Spring Framework
   service and the Spring Framework is the one responsible for initializing the
   service. This is dependency injection in action, and is very important for
   managing the complexity of larger systems.
2. Remember that our actual `getDadJoke()` method in our actual `JokeService`
   class does not actually do anything (yet). This is a great example of unit
   testing in action - the unit testing we are currently focused on,
   `shouldReturnGreeting()` does not, and should not, care about the
   implementation of the `getDadJoke()` method. The fact that we can make this
   unit test before we even implement that service method is proof that our unit
   test is properly isolated from any of the dependencies of the method it
   tests.

You should now be able to run your unit test and have it pass.

Let's now turn our attention to our existing integration test. It does not
directly instantiate the controller since it actually uses our `mockMvc` object
to make a request into the endpoint and exercise the Spring Framework to let it
deliver that request to the controller. This means it will compile successfully.
However, it will not run because the Spring Framework will look for a
`JokeService` to initialize the application with, but cannot find one in this
version of the integration test:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGreetDefault() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello Stephanie")));
    }

    @Test
    void shouldGreetByName() throws Exception {
        String greetingName = "Jamie";
        mockMvc.perform(get("/hello")
                .param("targetName", greetingName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello " + greetingName)));
    }
}
```

Running the version above as is will give you a long output where you will find
an error message to this effect:

```java
No qualifying bean of type 'com.flatiron.spring.FlatironSpring.JokeService' available: expected at least 1 bean which qualifies as autowire candidate.
```

Let's add a private member variable for the `JokeService`:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    // adding a JokeService here
    @MockBean
    private JokeService jokeService;

    @Test
    void shouldGreetDefault() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello Stephanie")));
    }

    @Test
    void shouldGreetByName() throws Exception {
        String greetingName = "Jamie";
        mockMvc.perform(get("/hello")
                .param("targetName", greetingName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Hello " + greetingName)));
    }
}
```

We can now run the integration test and it will fail with a different message
that indicates that the output of the `hello()` method is no longer what we
expected it to be before:

```java
Response content
Expected: "Hello Stephanie"
     but: was "Hello Stephanie<br/>Dad joke of the moment: null"
java.lang.AssertionError: Response content
Expected: "Hello Stephanie"
     but: was "Hello Stephanie<br/>Dad joke of the moment: null"
```

Since the Joke API is a) an API we do not control and b) an API that returns a
random joke every time we call it, we do not want to test its actual return
value, even in this integration test. So we will use the `containsString()`
matcher instead of the `equalTo()` to make sure we have what we need in the
response, ignoring the rest of the return value:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelloController.class)
class HelloControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JokeService jokeService;

    @Test
    void shouldGreetDefault() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello Stephanie")));
    }

    @Test
    void shouldGreetByName() throws Exception {
        String greetingName = "Jamie";
        mockMvc.perform(get("/hello")
                .param("targetName", greetingName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello " + greetingName)));
    }
}
```

You should have now noticed that we still have a potential gap, which is that
all the tests we've written so far could pass even if the `getDadJoke()` method
never actually interfaced with the Dad Joke API.

Let's fix that with an integration test of the Joke Service:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JokeServiceIntegrationTest {

    @Test
    void shouldReturnRandomDadJoke() {
        JokeService jokeService = new JokeService();
        String firstRandomDadJoke = jokeService.getDadJoke();
        assertThat(firstRandomDadJoke).isNotNull();
        String secondRandomDadJoke = jokeService.getDadJoke();
        assertThat(secondRandomDadJoke).isNotNull();
        assertThat(firstRandomDadJoke).isNotEqualTo(secondRandomDadJoke);
    }
}
```

Let's break it down:

1. Create this test the same way you've created previous tests by generating a
   new test for the `JokeService` class
2. Since the jokes are random, we cannot test for a specific value back from the
   service, so we do 2 things instead:
   1. Test that the return value from the joke service is not null
   2. Make sure that we don't get the same joke on 2 consecutive calls, ensuring
      that the joke is indeed "random"

Note: with a true random service, it's possible that the same return value could
be returned from 2 consecutive calls. If we wanted to guard against this, we
could conditionally make a third call if the 2 first calls resulted in the same
value being returned. The more consecutive calls we make, the less likely they
are to all return the same value.

Running this test with our current implementation of the `getDadJoke()` method
will fail, since it always returns null:

```java
public class JokeService {
    public String getDadJoke() {
        return null;
    }
}
```

Note: as discussed and seen before, this is a great way to make sure your
integration test is actually testing what you want it to test - it should fail
when your method doesn't yet do what you need it to do.

Let's now implement the actual interface with the Joke API:

```java
public class JokeService {
    public String getDadJoke() {
        String apiURL = "https://icanhazdadjoke.com/";
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(apiURL, DadJoke.class).joke;

        return result;
    }
}

class DadJoke {
  public String id;
  public String joke;
  public String status;
}
```

Let's examine this code:

1. We use the `RestTemplate` class to make a request to the URL for the Joke API
2. We use the `getForObject()` method and tell it to take the return of the call
   to the URL and convert its `JSON` return to a `Java` object
3. We define the `Java` object as a simple `POJO` that has 3 properties that
   match the `JSON` that the API returns
4. The `getForObject` method takes care of converting `JSON` to `Java` and
   returns an object of type `DadJoke`
5. We can then take the `joke` property of the `DadJoke` object and return it to
   the caller

Note: this is not a "unit" test because it actually lets the real service (not
mocked) make a request to the real API and tests the actual response (albeit not
the actual precise value, for the reasons we discussed)

You should now be able to rerun your integration test and have it pass
successfully.

## Acceptance (end-to-end) testing in Spring

For end-to-end "acceptance" testing, we want to validate that our functionality
works in just the same way as our users will end up using it. This means
different things for different types of functionality and applications, but in
the case of our API endpoints, it means we want to initialize the entire Spring
Framework:

```java
package com.flatiron.spring.FlatironSpring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGreetDefault() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello Stephanie")));
    }

    @Test
    void shouldGreetByName() throws Exception {
        String greetingName = "Jamie";
        mockMvc.perform(get("/hello")
                        .param("targetName", greetingName))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello " + greetingName)));
    }
}
```

The main differences between this "Acceptance" test and our earlier
"Integration" test are:

1. We are now initializing the entire Spring Framework with the annotation
   `@SpringBootTest`, which means the request we make through these test methods
   will actually go through all the layers of the framework, as if they were
   coming from an actual external client
2. We have asked Spring to auto-wire the `mockMvc` variable we'll be using to
   make the `http` request into our controller
3. We do not mock the actual service, and instead will be using the real service
   that Spring initializes for the controller

## Compare run times

Now that we have a set of Unit, Integration and Acceptance tests, let's run all
our tests together and compare run times. In my environment, the results are as
follows:

![Compare Test Runtimes](spring-testing-compare-runtimes.png)

As you can see, the unit tests are the "cheapest" to run (in terms of time that
it takes for them to run), then the integration tests are second and then the
acceptance tests.

As we move up on the testing pyramid, tests are more and more costly to run, and
therefore should be a) run less frequently because they cost the developers more
time, b) be less numerous, because the more of them there are, the longer the
whole test suite will need to run and c) cover less scenarios/permutations of
input for the same reasons.

This is why we try to cover as much functionality as possible with our unit
tests and focus our integration and acceptance tests on the specific integration
points that our unit tests could not (and should not) cover.

## Conclusion

We have learned how to learn integration testing in this lesson. Now we can make
sure our Spring application is working correctly and can make changes with
confidence since we can test them immediately afterwards.
