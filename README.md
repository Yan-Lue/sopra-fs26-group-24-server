# UWatch - Interactive Movie Finder

This production was conducted during the Software Practical Course at the Department of Informatics at the University of Zurich, during Spring Term 2026. The scope was to build a web-based application that uses at least one external API and features a collaborative, real-time user experience. The application is called UWatch and is designed to be an interactive movie finder, that can be used by a group of friends in order to find a movie to watch, that suits everybody's taste. The key functionality is to present several movies - based on filters chosen by the host of session - to the participating users and let them decide whether to like or dislike a respective movie. In the end a final scoreboard for every movie is presented, together with additional similar recommendations.

---

## High-Level Components

The server is structured into four primary layers to ensure a clean separation of concerns:

1. **Rest Controllers:** Handle incoming HTTP requests from the React client.
    - _Reference:_ [`UserController.java`](https://github.com/Yan-Lue/sopra-fs26-group-24-server/blob/main/src/main/java/ch/uzh/ifi/hasel/sopra/controller/UserController.java) - Delegates requests concerning user registration, login, and profile updates.
2. **Service Layer:** Contains the core business logic, including all functionality about users.
    - _Reference:_ [`UserService.java`](https://github.com/Yan-Lue/sopra-fs26-group-24-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs26/service/UserService.java) - Manages and coordinates the core logic of user features such as registration and login.
3. **Domain Models:** Represents the data structures for Users, Movies, and Groups.
    - _Reference:_ [`User.java`](https://github.com/Yan-Lue/sopra-fs26-group-24-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs26/entity/User.java) - Defines the user entity and its relationship to the database.
4. **Repository Layer:** Handles database access via JPA.
    - _Example:_ [`UserRepository.java`](https://github.com/Yan-Lue/sopra-fs26-group-24-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs26/repository/UserRepository.java)

This is a top-down approach that starts at the top with the Rest Controllers that receive HTTP requests, converts DTOs into entities through the DTOMapper, and passes them on to the Services Layer for processing. The Services Layer processes the core business logic using the Domain Models (JPA entities) that make up the database structure. Lastly, the Repositories Layer is responsible for saving and retrieving those entities from the database by the services.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

#### Technical Prerequisites

Ensure the following are installed before running the project:

- **Java 17** – [Download here](https://www.oracle.com/java/technologies/downloads/#java17)
- **Gradle** – [Download here](https://gradle.org/install/)

```bash
# Verify your installations
java -version
gradle -version
```

#### Additional Prerequisites

This project uses an external API called [`Tmdb`](https://developer.themoviedb.org/reference/intro/getting-started). To retrieve movies, suggestions and movie information. In order to access this API, and [`API-key`](https://developer.themoviedb.org/docs/getting-started) is required and has to be stored in an .env file (Make sure to add it to the .gitignore!)

The external database is hosted on Supabase. In order to test the setup locally, make sure to create an `.env` file in the root of the project for local development:

```
DB_HOST=localhost
DB_PORT=5432
DB_USER=the-respective-user
DB_PASSWORD=the-respective-user
DB_NAME=the-respective-user
```

The setup of the database is then done automatically. Please reach out, in order to receive the necessary credentials as well as to be added to the supabase project.

### Installing

1. **Clone the repository**

```bash
git clone https://github.com/Yan-Lue/sopra-fs26-group-24-server.git
```

2. **Open the project in your IDE**

    Download an IDE of your choice:
    - [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) _(recommended — free educational license available [here](https://www.jetbrains.com/community/education/#students))_
    - [VS Code](https://code.visualstudio.com/)

    **IntelliJ:** File → Open → select the project folder → import as a Gradle project → right-click `build.gradle` → Run Build

    **VS Code:** Install these extensions:
    - `vmware.vscode-spring-boot`
    - `vscjava.vscode-spring-initializr`
    - `vscjava.vscode-spring-boot-dashboard`
    - `vscjava.vscode-java-pack`

### Development Mode

1. **Build the project**

```bash
./gradlew build        # macOS/Linux
./gradlew.bat build    # Windows
```

2. **Run the server**

```bash
./gradlew bootRun
```

The server will be available at `http://localhost:8080`.

3. **Run tests**

```bash
./gradlew test
```

To skip tests on every change:

```bash
./gradlew build -x test
```

For specific test cases concerning controller, service, etc.:

```bash
./gradlew test --tests "<path-to-test>.<TestClassName>"

# Example:
./gradlew test --tests "ch.uzh.ifi.hase.soprafs26.controller.UserControllerTest"
```

## Deployment

The application is deployed using **Google Cloud** for the server and **Supabase** for the production database. The app is containerized with **Docker**.

The production environment uses Supabase (PostgreSQL) instead of the H2 in-memory database used in development. Make sure the relevant environment variables (database URL, credentials) are configured in your cloud environment before deploying.

## Built With

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Postgres](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Google Cloud](https://img.shields.io/badge/Google_Cloud-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?style=for-the-badge&logo=supabase&logoColor=white)

## Versioning

We use milestone-based versioning (M1, M2, M3, ...).
For the versions available, see the [tags on this repository](https://github.com/Yan-Lue/sopra-fs26-group-24-server).

## Roadmap

New features that could be added to contribute to our project: 
- Possibility to watch movie-trailers. Either provided as a link on the results page and/ or by directly embedding in vote-round. 
- Redirect all player to the new round when the host starts a new round. 
- Search for users and view their profile, add users to friends list, quick invite friends to lobby.

## Authors

- **Yannic Lüthi** - _Owner_ - [Yan-Lue](https://github.com/Yan-Lue)
- **Danilo Ruggieri** - _Collaborator_ - [daniloruggieri](https://github.com/daniloruggieri)
- **Noël Schneuwly** - _Collaborator_ - [noelschneuwly](https://github.com/noelschneuwly)
- **Elia Lehmann** - _Collaborator_ - [grootcod](https://github.com/grootcod)
- **Janik Altmann** - _Collaborator_ - [jaltma](https://github.com/jaltma)

## License

This project is licensed under the Apache License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

- Many thanks to all contributors
