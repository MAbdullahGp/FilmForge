# FilmForge
[![Ask DeepWiki](https://devin.ai/assets/askdeepwiki.png)](https://deepwiki.com/MAbdullahGp/FilmForge)

FilmForge is a desktop movie browsing application that mimics modern streaming services. It features a rich Java Swing GUI for the user interface and a powerful C++ backend that handles all data processing and logic. The application leverages a variety of fundamental data structures to deliver features like fast search, personalized recommendations, trending movies, and user authentication.

## Features
- **User Authentication**: Secure login and registration system using a Hash Table.
- **Categorized Browsing**: Movies are displayed in a Netflix-style, horizontally-scrollable format, categorized by genre.
- **Trending Movies**: A "Trending" section showcases the most-viewed movies, managed by a Max Heap.
- **Advanced Search**: A highly efficient search engine built on a Hash-based Inverted Index for instant results.
- **Sorting**: Users can sort the entire movie library by name (using Quick Sort) or by view count (using Merge Sort).
- **Watch History**: Each user's watch history is tracked and displayed using a Doubly Linked List.
- **Personalized Recommendations**: A graph-based algorithm analyzes a user's watch history to suggest movies from their most-watched genres.
- **Responsive UI**: A modern, dark-themed interface built with Java Swing.

## Tech Stack & Architecture

FilmForge uses a decoupled architecture where the frontend (Java) communicates with the backend (C++) via command-line calls.

*   **Frontend**: `Java Swing` for the graphical user interface.
*   **Backend**: `C++` for all core logic, data management, and algorithm implementations.
*   **Data Persistence**: Plain `.txt` files (`movies.txt`, `users.txt`, `history.txt`) act as the database.

### Core Data Structures
The C++ backend is a showcase of various data structures, each chosen for its specific strengths:

| Header File | Data Structure | Purpose |
| :--- | :--- | :--- |
| `AuthHashTable.h` | **Hash Table** | Manages user credentials for O(1) average time complexity on login and registration lookups. |
| `SearchEngine.h` | **Hash Map (Inverted Index)** | Provides the primary search functionality by mapping keywords (titles, genres, etc.) to movies for near-instant search results. |
| `SearchTrie.h` | **Trie** | Implemented as a secondary search mechanism for efficient prefix-based (autocomplete) searches. |
| `TrendingHeap.h` | **Max Heap** | Identifies and displays the top 10 most-viewed movies. |
| `MovieAVL.h` | **AVL Tree** | Stores all movies in a self-balancing binary search tree, ensuring that loading and displaying all movies is efficient and sorted by ID. |
| `HistoryDLL.h` | **Doubly Linked List** | Stores a user's viewing history, allowing for O(1) insertion of recently watched items. |
| `RecGraph.h` | **Graph (Adjacency via Logic)**| Builds a genre-preference model from a user's history to provide personalized movie recommendations. |
| `sorting.h` | **Quick & Merge Sort** | Implements sorting algorithms to organize movies by name (Quick Sort) or views (Merge Sort). |

## How It Works

1.  The `FilmForgeGUI.java` application presents the user interface.
2.  When a user performs an action (e.g., clicks "Search"), the Java backend does not perform the logic itself.
3.  Instead, it executes the compiled C++ program (`filmforge.exe`) with specific command-line arguments. For example: `filmforge.exe search The Dark Knight`.
4.  The `main.cpp` program parses these arguments, routes the command to the appropriate data structure or function, and performs the required operation.
5.  The C++ program then prints the results to the standard output (`stdout`).
6.  The Java application captures this output, parses the text, and dynamically updates the GUI to display the results to the user.

## Setup and Usage

To run this project, you will need a C++ compiler (like G++) and a Java Development Kit (JDK).

### 1. Compile the C++ Backend

Navigate to the project's root directory in your terminal and run the following command to compile the C++ source code. This will create an executable named `filmforge.exe`.

```bash
g++ main.cpp -o filmforge.exe
```

### 2. Compile the Java Frontend

In the same directory, compile the Java GUI source code.

```bash
javac FilmForgeGUI.java
```

### 3. Run the Application

Execute the compiled Java program. The Java GUI will launch, and it will automatically call the C++ executable in the background as needed.

```bash
java FilmForgeGUI
```

**Note:** Ensure that `filmforge.exe`, `movies.txt`, `users.txt`, `history.txt`, and `intro.gif` are all present in the same directory as the compiled Java classes for the application to function correctly.