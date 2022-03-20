# Testcontainers with Podman

- [What is Testcontainers](#12282a77ac3d643b9aab1cd2b5a9d890)
- [Why replace Docker with Podman?](#91e6b8d6653cf3b608a92a82a0b936a4)
- [Install Podman](#47e011156251a64be7e2f051a619bdf8)
    - [Linux](#edc9f0a5a5d57797bf68e37364743831)
        - [Ubuntu 20.04](#73611f9a837b7a25dad3a9c5d1a98658)
        - [Ubuntu 20.10 and newer](#5222cd09a77e66bf72fbeffe66ec0212)
    - [MacOS](#0a5b7edb55b772c60bfa8af868b679cf)
- [Verify that Podman is installed correctly](#baaa758ba23f3fdec680766b40707565)
- [Enable the Podman service](#a9364df70e9d77df7fac335dce5ced5b)
    - [Linux](#edc9f0a5a5d57797bf68e37364743831)
    - [MacOS](#0a5b7edb55b772c60bfa8af868b679cf)
- [Configure Graldle build script](#df9e8b308e46c444c10a2341aee0c2e7)
- [Create a base test class](#ea52478914f0bb02dcec80d5d34aea4b)
- [Implement test classes](#c4a8219764d12a6b9e080181577545d7)

<!-- Table of contents is made with https://github.com/evgeniy-khist/markdown-toc -->

## <a id="12282a77ac3d643b9aab1cd2b5a9d890"></a>What is Testcontainers

> [Testcontainers](https://www.testcontainers.org/) is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

With Testcontainers, your JUnit tests can use PostgreSQL to run in a Docker container instead of an embedded H2
Database.

## <a id="91e6b8d6653cf3b608a92a82a0b936a4"></a>Why replace Docker with Podman?

Docker changed the Docker Desktop terms in 2021. Docker Desktop is not free for everyone anymore:

* [Docker is Updating and Extending Our Product Subscriptions](https://www.docker.com/blog/updating-product-subscriptions/)
* [Do the New Terms of Docker Desktop Apply If You Don’t Use the Docker Desktop UI?](https://www.docker.com/blog/do-the-new-terms-of-docker-desktop-apply-if-you-dont-use-the-docker-desktop-ui/)

On Linux you can still use the Docker CLI and Docker Engine for free. On Windows you could install and run Docker CLI
and Engine inside [WSL2](https://docs.microsoft.com/en-us/windows/wsl/) (Windows Subsystem for Linux). On MacOS you can
install Docker CLI and Engine inside a virtual machine.

Docker can be replaced with an open-source alternative called [Podman](https://podman.io/) maintained by
the [containers](https://github.com/containers) organization.

> Podman is a daemonless container engine for developing, managing, and running OCI Containers on your Linux System. Containers can either be run as root or in rootless mode. Simply put: `alias docker=podman`.

This example shows how to use Podman with [Testcontainers](https://www.testcontainers.org/) in Java projects that use
Gradle on Ubuntu Linux and MacOS (both x86_64 and Apple silicon).

## <a id="47e011156251a64be7e2f051a619bdf8"></a>Install Podman

### <a id="edc9f0a5a5d57797bf68e37364743831"></a>Linux

See https://podman.io/getting-started/installation#linux-distributions

#### <a id="73611f9a837b7a25dad3a9c5d1a98658"></a>Ubuntu 20.04

Set up the **stable** repository and install the podman package:

```bash
source /etc/os-release
echo "deb https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/ /" | sudo tee /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list
curl -L https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/Release.key | sudo apt-key add -
sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get -y install podman
```

Verify the installation:

```bash
podman info
```

#### <a id="5222cd09a77e66bf72fbeffe66ec0212"></a>Ubuntu 20.10 and newer

The podman package is available in the official repositories for Ubuntu 20.10 and newer.

Install the podman package:

```bash
sudo apt-get -y update
sudo apt-get -y install podman
```

Verify the installation:

```bash
podman info
```

### <a id="0a5b7edb55b772c60bfa8af868b679cf"></a>MacOS

See https://podman.io/getting-started/installation#macos

Podman is a tool for running Linux containers. Podman includes a command, `podman machine` that automatically manages
Linux VM’s on MacOS.

Install Podman Machine and Remote Client:

```bash
brew install podman
```

Start the Podman-managed VM:

```bash
podman machine init
podman machine start
```

Verify the installation:

```bash
podman info
```

## <a id="baaa758ba23f3fdec680766b40707565"></a>Verify that Podman is installed correctly

Run the `busybox` or other image to verify that Podman is installed correctly:

```bash
podman run --rm busybox echo "hello-world"
```

If you have the following error:

```
Error: short-name "busybox" did not resolve to an alias and no unqualified-search registries are defined in "/etc/containers/registries.conf"
```

Configure `unqualified-search-registries`:

```bash
echo "unqualified-search-registries = [\"docker.io\"]" | sudo tee -a /etc/containers/registries.conf
```

See https://www.redhat.com/sysadmin/container-image-short-names

## <a id="a9364df70e9d77df7fac335dce5ced5b"></a>Enable the Podman service

Testcontainers library communicates with Podman using socket file.

### <a id="edc9f0a5a5d57797bf68e37364743831"></a>Linux

Start Podman service for a regular user (rootless) and make it listen to a socket:

```bash
systemctl --user enable --now podman.socket
```

Check the Podman service status:

```bash
systemctl --user status podman.socket
```

Check the socket file exists:

```bash
ls -la /run/user/$UID/podman/podman.sock
```

### <a id="0a5b7edb55b772c60bfa8af868b679cf"></a>MacOS

Podman socket file `/run/user/1000/podman/podman.sock` can be found inside the Podman-managed Linux VM. A local socket
on MacOS can be forwarded to a remote socket on Podman-managed VM using SSH tunneling.

The port of the Podman-managed VM can be found with the command `podman system connection list --format=json`.

Install [jq](https://stedolan.github.io/jq/) to parse JSON:

```bash
brew install jq
```

Create a shell alias to forward local socket `/tmp/podman.sock` to remote socket `/run/user/1000/podman/podman.sock`:

```bash
echo "alias podman-sock=\"rm -f /tmp/podman.sock && ssh -i ~/.ssh/podman-machine-default -p \$(podman system connection list --format=json | jq '.[0].URI' | sed -E 's|.+://.+@.+:([[:digit:]]+)/.+|\1|') -L'/tmp/podman.sock:/run/user/1000/podman/podman.sock' -N core@localhost\"" >> ~/.zprofile
source ~/.zprofile
```

Open SSH tunnel:

```bash
podman-sock
```

Make sure SSH tunnel is open before executing tests using Testcontainers.

## <a id="df9e8b308e46c444c10a2341aee0c2e7"></a>Configure Graldle build script

[`build.gradle`](build.gradle)

```groovy
test {
    OperatingSystem os = DefaultNativePlatform.currentOperatingSystem;
    if (os.isLinux()) {
        def uid = ["id", "-u"].execute().text.trim()
        environment "DOCKER_HOST", "unix:///run/user/$uid/podman/podman.sock"
    } else if (os.isMacOsX()) {
        environment "DOCKER_HOST", "unix:///tmp/podman.sock"
    }
    environment "TESTCONTAINERS_RYUK_DISABLED", "true"
}
```

Set `DOCKER_HOST` environment variable to Podman socket file depending on the operating system.

Disable Ryuk with the environment variable `TESTCONTAINERS_RYUK_DISABLED`.

> [Moby Ryuk](https://github.com/testcontainers/moby-ryuk) helps you to remove containers/networks/volumes/images by given filter after specified delay.

Ryuk is a technology for Docker and doesn't support Podman. See https://github.com/testcontainers/moby-ryuk/issues/23

Testcontainers library uses Ruyk to remove containers. Instead of relying on Ryuk to remove implicitly containers, we
will explicitly remove containers with a JVM shutdown hook:

```java
Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
```

## <a id="ea52478914f0bb02dcec80d5d34aea4b"></a>Create a base test class

It is useful to define a container that is only started once for all (or several) test classes. Starting a database
container for each test class is a big overhead.

Containers should be JVM singletons and not a Spring singletons. Sometimes Spring can't reuse an already existing
context, for example
when [`@MockBean`](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/mock/mockito/MockBean.html)
or [`@DirtiesContext`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/annotation/DirtiesContext.html)
are used. This means you get multiple Spring contexts in tests.

Testcontainers library supports singleton containers pattern.
See https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers

Create a base test class and implement a singleton container pattern.

[`BaseIntegrationTest.java`](src/test/java/com/example/podman/testcontainers/BaseIntegrationTest.java)

```java

@SpringBootTest
public abstract class BaseIntegrationTest {

    public static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final GenericContainer<?> REDIS;

    static {
        POSTGRES = createPostgresContainer();
        REDIS = createRedisContainer();
    }

    private static PostgreSQLContainer<?> createPostgresContainer() {
        PostgreSQLContainer<?> postgres =
                new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag("14-alpine"));
        postgres.start();
        Runtime.getRuntime().addShutdownHook(new Thread(postgres::stop));
        return postgres;
    }

    private static GenericContainer<?> createRedisContainer() {
        GenericContainer<?> redis =
                new GenericContainer<>(DockerImageName.parse("redis").withTag("6-alpine"))
                        .withExposedPorts(REDIS_PORT);
        redis.start();
        Runtime.getRuntime().addShutdownHook(new Thread(redis::stop));
        return redis;
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", BaseIntegrationTest::getPostgresR2bcUrl);
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", REDIS::getContainerIpAddress);
        registry.add("spring.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
    }

    private static String getPostgresR2bcUrl() {
        return "r2dbc:postgresql://"
                + POSTGRES.getContainerIpAddress()
                + ":"
                + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/"
                + POSTGRES.getDatabaseName();
    }
}
```

The singleton container is started only once when the base class is loaded. The container can then be used by all
inheriting test classes. At the end of the tests the JVM shutdown hook will take care of stopping the singleton
container.

[`@DynamicPropertySource`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/DynamicPropertySource.html)
annotation and its supporting infrastructure allows properties from Testcontainers based tests to be exposed easily to
Spring integration tests.

## <a id="c4a8219764d12a6b9e080181577545d7"></a>Implement test classes

Implement test classes by inheriting the base
class [`BaseIntegrationTest.java`](src/test/java/com/example/podman/testcontainers/BaseIntegrationTest.java).

[`PersonRepositoryTests`](src/test/java/com/example/podman/testcontainers/PersonRepositoryTests.java)

```java

@Slf4j
class PersonRepositoryTests extends BaseIntegrationTest {

    @Autowired
    PersonRepository personRepository;

    @Test
    void shouldFindByLastName() {
        personRepository.save(new Person(null, "Harry", "Callahan")).subscribe();

        personRepository
                .findByLastName("Callahan")
                .doOnNext(person -> log.info("Person found with findByLastName(\"Callahan\"): {}", person))
                .as(StepVerifier::create)
                .expectNextMatches(
                        person ->
                                person.id() != null
                                        && "Harry".equals(person.firstName())
                                        && "Callahan".equals(person.lastName()))
                .verifyComplete();
    }
}
```

[`RedisIncrementTests`](src/test/java/com/example/podman/testcontainers/RedisIncrementTests.java)

```java
public class RedisIncrementTests extends BaseIntegrationTest {

    @Autowired
    ReactiveRedisTemplate<String, Long> redisTemplate;

    @Test
    void shouldIncrementKey() {
        Flux.range(0, 3)
                .flatMap(i -> redisTemplate.opsForValue().increment("mykey"))
                .as(StepVerifier::create)
                .expectNext(1L)
                .expectNext(2L)
                .expectNext(3L)
                .verifyComplete();
    }
}
```