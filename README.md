# SUSTech CS110 Project

## Environment

**java JDK**: openjdk-22 (Oracle OpenJDK 22.0.2)

**javafx-fxml**: 22.0.1

**javafx-controls**: 22.0.1

**maven**: 3.8.5

## List of files

- **Application.java**: The main entry point to the application, which is responsible for launching the game.

- **Controller.java**: Handles interactions and events in the JavaFX user interface to ensure responsiveness to user actions.

- **Server.java**: The main entry point to server, which is responsible for communicating with players.

- **board.fxml**: A prototype layout file for the game board, defining the visual elements of the user interface.

- **resources**: Store the image assets and user data needed for the game board.

## Game logic

- **Game Start**: Allow the user to select game options, such as board size, number of players, etc., to set the initial state of the game.

- **Operational Effectiveness**: Monitor every move taken by the user, verify that they are legitimate (e.g., beyond the board boundaries), and update the board status.

- **Game Over**: Notifies the user that the game has ended, including a winner's message or a loser's hint.
