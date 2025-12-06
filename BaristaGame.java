import java.io.*;
import java.util.*;

/**
 * BARISTA MILK TEA SIMULATOR (Fixed Layout Edition)
 * - Colors disabled to prevent layout breakage on Windows CMD.
 * - Width constrained to 100 chars to prevent wrapping.
 * - Clean clearing method used.
 */
public class BaristaGame {

    public static void main(String[] args) {
        try {
            GameManager gameManager = new GameManager();
            gameManager.start();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    //            RENDER SYSTEM (THE BOX)
    // ==========================================
    
    static class RenderSystem {
        private static final int WIDTH = 100; // Reduced to 100 to prevent terminal wrapping
        private static final int HEIGHT = 30; // Fixed height
        
        // Border Characters
        private static final String TOP_LEFT = "╔";
        private static final String TOP_RIGHT = "╗";
        private static final String BOTTOM_LEFT = "╚";
        private static final String BOTTOM_RIGHT = "╝";
        private static final String HORIZONTAL = "═";
        private static final String VERTICAL = "║";

        public void drawFrame(List<String> rawContent, String prompt) {
            clearScreen();
            
            // 1. Process content (Word Wrap)
            List<String> processedContent = new ArrayList<>();
            for (String line : rawContent) {
                if (line.length() > WIDTH - 4) {
                    processedContent.addAll(wrapText(line, WIDTH - 4));
                } else {
                    processedContent.add(line);
                }
            }
            
            // 2. Calculate Vertical Padding
            // We reserve 3 lines at the bottom for the input area if a prompt exists
            int contentAreaHeight = HEIGHT - 2; 
            int contentHeight = processedContent.size();
            
            // Center the content vertically within the available space
            int totalPadding = Math.max(0, contentAreaHeight - contentHeight);
            int paddingTop = totalPadding / 2;
            int paddingBottom = totalPadding - paddingTop;

            // 3. Draw Top Border
            System.out.print(TOP_LEFT);
            for (int i = 0; i < WIDTH - 2; i++) System.out.print(HORIZONTAL);
            System.out.println(TOP_RIGHT);

            // 4. Draw Content Area
            // Top Padding
            for (int i = 0; i < paddingTop; i++) printEmptyLine();

            // Content
            for (String line : processedContent) printCenteredLine(line);
            
            // Bottom Padding
            for (int i = 0; i < paddingBottom; i++) printEmptyLine();
            
            // 5. Draw Bottom Border
            System.out.print(BOTTOM_LEFT);
            for (int i = 0; i < WIDTH - 2; i++) System.out.print(HORIZONTAL);
            System.out.println(BOTTOM_RIGHT);
            
            // 6. Draw Prompt Below
            if (prompt != null && !prompt.isEmpty()) {
                System.out.println(prompt);
                System.out.print("> ");
            }
        }

        private void printEmptyLine() {
            System.out.print(VERTICAL);
            for(int i=0; i<WIDTH-2; i++) System.out.print(" ");
            System.out.println(VERTICAL);
        }

        private void printCenteredLine(String text) {
            int visibleLen = text.length(); // No ANSI codes to worry about now
            int padding = (WIDTH - 2 - visibleLen) / 2;
            int rightPadding = WIDTH - 2 - visibleLen - padding;

            System.out.print(VERTICAL);
            for(int i=0; i<padding; i++) System.out.print(" ");
            System.out.print(text);
            for(int i=0; i<rightPadding; i++) System.out.print(" ");
            System.out.println(VERTICAL);
        }

        private List<String> wrapText(String text, int maxWidth) {
            List<String> lines = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder currentLine = new StringBuilder();
            
            for (String word : words) {
                if (currentLine.length() + word.length() + 1 <= maxWidth) {
                    currentLine.append(word).append(" ");
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word).append(" ");
                }
            }
            if (currentLine.length() > 0) lines.add(currentLine.toString());
            return lines;
        }

        public void clearScreen() {
            // "Dirty" clear that works on all systems by scrolling
            for(int i=0; i<50; i++) System.out.println();
        }
    }

    // ==========================================
    //            CONTROLLER LAYER
    // ==========================================

    static class GameManager {
        private Player currentPlayer;
        private DataManager dataManager;
        private UserInterface ui;
        private GameStatistics gameStats;
        private Leaderboard leaderboard;
        private AchievementTracker achievementTracker;
        private AnimationManager animationManager;
        private StoryManager storyManager;
        
        private int currentDay;
        private boolean gameInProgress;
        private Random random;

        private static final int DAYS_PER_GAME = 7;
        private static final int CUSTOMERS_PER_DAY = 5;
        
        private final String[] CUSTOMER_NAMES = {"Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George", "Zoe"};
        private final String[] DRINK_TYPES = {"Milk Tea", "Taro Milk Tea", "Matcha Milk Tea", "Strawberry Boba", "Brown Sugar Boba"};

        public GameManager() {
            this.dataManager = new DataManager();
            this.ui = new UserInterface();
            this.gameStats = new GameStatistics();
            this.leaderboard = new Leaderboard(dataManager);
            this.achievementTracker = new AchievementTracker();
            this.animationManager = new AnimationManager(ui);
            this.storyManager = new StoryManager(ui);
            this.random = new Random();
            this.currentDay = 1;
        }

        public void start() {
            animationManager.playIntroCutscene();
            boolean running = true;

            while (running) {
                int choice = ui.showMainMenu();
                switch (choice) {
                    case 1: startNewGame(); break;
                    case 2: handleUserProfile(); break;
                    case 3: ui.showStatistics(currentPlayer != null ? currentPlayer : new Player("Guest")); break;
                    case 4: ui.showTutorial(); break;
                    case 5: ui.showLeaderboard(leaderboard.getEntries()); break;
                    case 6: ui.showAchievements(achievementTracker.getAchievements()); break;
                    case 7: ui.showCredits(); break;
                    case 8: running = false; ui.showExitMessage(); break;
                    default: ui.showMessage("Invalid Option!");
                }
            }
        }

        public void startNewGame() {
            if (currentPlayer == null) {
                ui.showMessage("Please log in or create a profile first!");
                handleUserProfile();
                if (currentPlayer == null) return;
            }

            currentDay = 1;
            gameStats.resetDaily();
            gameInProgress = true;
            
            storyManager.showGameStartCutscene(currentPlayer.getUsername());
            runGameLoop();
        }

        private void runGameLoop() {
            while (currentDay <= DAYS_PER_GAME && gameInProgress) {
                animationManager.playDayTransition(currentDay);
                
                int dailyScore = 0;
                int ordersMade = 0;
                int ordersMissed = 0;
                
                Queue<Customer> queue = generateDailyCustomers();
                
                while (!queue.isEmpty()) {
                    Customer customer = queue.poll();
                    
                    animationManager.showCustomerArrival(customer);
                    
                    boolean success = serveCustomer(customer);
                    
                    if (success) {
                        ordersMade++;
                        int score = customer.getOrder().calculateScore();
                        if (customer.isVip()) score *= 1.5;
                        dailyScore += score;
                        animationManager.showHappyCustomer();
                    } else {
                        ordersMissed++;
                        animationManager.showUnhappyCustomer();
                    }
                    
                    if(success) currentPlayer.addScore(dailyScore);
                }
                
                currentPlayer.incrementDaysPlayed();
                ui.showDailySummary(currentDay, ordersMade, ordersMissed, dailyScore);
                achievementTracker.checkAchievements(currentPlayer);
                try { dataManager.savePlayer(currentPlayer); } catch (Exception e) {}
                currentDay++;
            }
            
            endGame();
        }

        private boolean serveCustomer(Customer customer) {
            Order order = customer.getOrder();
            
            // Show interaction with prompt
            ui.renderer.drawFrame(Arrays.asList(
                "NEW CUSTOMER ARRIVED!",
                "",
                "Name: " + customer.getName() + (customer.isVip() ? " [VIP]" : ""),
                "Greeting: \"" + customer.getGreeting() + "\"",
                "",
                "ORDER: " + order.getDrinkName(),
                "RECIPE: " + String.join(", ", order.getIngredients()),
                ""
            ), "Type ingredients (comma separated):");
            
            long startTime = System.currentTimeMillis();
            String input = ui.scanner.nextLine();
            long endTime = System.currentTimeMillis();
            
            if (input.equalsIgnoreCase("exit")) {
                gameInProgress = false;
                return false;
            }
            
            double timeSeconds = (endTime - startTime) / 1000.0;
            boolean success = order.checkCorrectness(input);
            order.complete(success, timeSeconds);
            
            ui.showOrderResult(success, order);
            return success;
        }

        private Queue<Customer> generateDailyCustomers() {
            Queue<Customer> queue = new LinkedList<>();
            for (int i = 0; i < CUSTOMERS_PER_DAY; i++) {
                boolean isVip = random.nextDouble() < 0.2;
                String name = CUSTOMER_NAMES[random.nextInt(CUSTOMER_NAMES.length)];
                Customer c = new Customer(name, isVip);
                String drink = DRINK_TYPES[random.nextInt(DRINK_TYPES.length)];
                c.setOrder(new Order(drink));
                queue.add(c);
            }
            return queue;
        }

        private void endGame() {
            animationManager.playEndingCutscene(currentPlayer.calculateRating());
            leaderboard.addEntry(currentPlayer.getUsername(), currentPlayer.getTotalScore());
            try { 
                dataManager.savePlayer(currentPlayer); 
                dataManager.saveLeaderboard(leaderboard.getScoresMap());
            } catch (Exception e) {}
            gameInProgress = false;
        }

        public void handleUserProfile() {
            int choice = ui.showLoginMenu();
            if (choice == 1) login();
            else if (choice == 2) signup();
        }

        private void login() {
            String username = ui.getTextInput("Enter Username:");
            try {
                Player p = dataManager.loadPlayer(username);
                if (p != null) {
                    currentPlayer = p;
                    ui.showMessage("Welcome back, " + username + "!");
                } else {
                    ui.showMessage("User not found.");
                }
            } catch (Exception e) { ui.showMessage("Error loading profile."); }
        }

        private void signup() {
            String username = ui.getTextInput("New Username:");
            currentPlayer = new Player(username);
            try {
                dataManager.savePlayer(currentPlayer);
                ui.showMessage("Profile created!");
            } catch (Exception e) { ui.showMessage("Error saving profile."); }
        }
    }

    // ==========================================
    //               MODEL LAYER
    // ==========================================

    static class Player implements Serializable {
        private static final long serialVersionUID = 1L;
        private String username;
        private int totalScore;
        private int gamesPlayed;
        private int daysPlayed;
        
        public Player(String username) { this.username = username; this.totalScore = 0; }
        public void addScore(int score) { this.totalScore += score; }
        public void incrementDaysPlayed() { this.daysPlayed++; }
        public String getUsername() { return username; }
        public int getTotalScore() { return totalScore; }
        public int getGamesPlayed() { return gamesPlayed; }
        public int calculateRating() {
            if (daysPlayed == 0) return 1;
            int avg = totalScore / daysPlayed;
            return avg > 500 ? 5 : avg > 300 ? 4 : avg > 150 ? 3 : 2;
        }
    }

    static class Customer {
        private String name;
        private boolean isVip;
        private Order order;
        public Customer(String name, boolean isVip) { this.name = name; this.isVip = isVip; }
        public void setOrder(Order order) { this.order = order; }
        public Order getOrder() { return order; }
        public String getName() { return name; }
        public boolean isVip() { return isVip; }
        public String getGreeting() { return isVip ? "I expect perfection." : "Hi! Can I get a drink?"; }
    }

    static class Order {
        private String drinkName;
        private List<String> ingredients;
        private boolean correct;
        private double timeTaken;

        public Order(String drinkName) {
            this.drinkName = drinkName;
            this.ingredients = generateIngredients(drinkName);
        }

        private List<String> generateIngredients(String drink) {
            List<String> list = new ArrayList<>();
            list.add("Tea"); list.add("Milk");
            if (drink.contains("Taro")) list.add("Taro");
            if (drink.contains("Matcha")) list.add("Matcha");
            if (drink.contains("Strawberry")) list.add("Strawberry");
            if (drink.contains("Sugar")) list.add("Brown Sugar");
            if (drink.contains("Boba")) list.add("Tapioca");
            return list;
        }

        public boolean checkCorrectness(String input) {
            if (input == null || input.isEmpty()) return false;
            String[] parts = input.split(",");
            List<String> provided = new ArrayList<>();
            for (String p : parts) provided.add(p.trim().toLowerCase());
            for (String req : ingredients) {
                boolean found = false;
                for (String p : provided) {
                    if (p.contains(req.toLowerCase())) { found = true; break; }
                }
                if (!found) return false;
            }
            return true;
        }

        public void complete(boolean correct, double timeTaken) {
            this.correct = correct;
            this.timeTaken = timeTaken;
        }

        public int calculateScore() {
            if (!correct) return 0;
            return (timeTaken < 5.0) ? 150 : 100;
        }
        public String getDrinkName() { return drinkName; }
        public List<String> getIngredients() { return ingredients; }
    }

    static class GameStatistics implements Serializable {
        private static final long serialVersionUID = 1L;
        public void resetDaily() {}
    }

    static class AchievementTracker {
        private List<String> unlocked = new ArrayList<>();
        public void checkAchievements(Player p) {
            if (p.getTotalScore() > 1000 && !unlocked.contains("High Roller")) unlocked.add("High Roller");
        }
        public List<String> getAchievements() { return unlocked; }
    }

    static class Leaderboard {
        private Map<String, Integer> scores = new HashMap<>();
        public Leaderboard(DataManager dm) { try { scores = dm.loadLeaderboard(); } catch(Exception e){} }
        public void addEntry(String name, int score) { scores.put(name, score); }
        public List<Map.Entry<String, Integer>> getEntries() {
            List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
            list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            return list;
        }
        public Map<String, Integer> getScoresMap() { return scores; }
    }

    // ==========================================
    //               VIEW LAYER
    // ==========================================

    static class UserInterface {
        private Scanner scanner = new Scanner(System.in);
        private RenderSystem renderer = new RenderSystem();

        public int showMainMenu() {
            List<String> content = new ArrayList<>();
            content.add("");
            content.add("BARISTA SIMULATOR");
            content.add("");
            content.add("1. Start New Game");
            content.add("2. Login / Register");
            content.add("3. View Statistics");
            content.add("4. Tutorial");
            content.add("5. Leaderboard");
            content.add("6. Achievements");
            content.add("7. Credits");
            content.add("8. Exit");
            content.add("");
            
            renderer.drawFrame(content, "Select an option [1-8]");
            return getNumericInput(1, 8);
        }
        
        public int showLoginMenu() {
            List<String> content = Arrays.asList(
                "",
                "USER PROFILE",
                "",
                "1. Login",
                "2. Register",
                ""
            );
            renderer.drawFrame(content, "Select Option [1-2]");
            return getNumericInput(1, 2);
        }

        // Note: Customer Interaction handled directly in GameManager to access logic variables easily
        // for input loops

        public void showOrderResult(boolean success, Order o) {
            List<String> content = new ArrayList<>();
            content.add(success ? "ORDER SUCCESS!" : "ORDER FAILED!");
            content.add("");
            if(success) {
                content.add("Score Earned: " + o.calculateScore());
                content.add("Customer is happy!");
            } else {
                content.add("You missed an ingredient.");
                content.add("Customer left angry.");
            }
            content.add("");
            renderer.drawFrame(content, "Press Enter to continue...");
            scanner.nextLine();
        }

        public void showDailySummary(int day, int made, int missed, int score) {
            List<String> content = Arrays.asList(
                "DAY " + day + " COMPLETE",
                "",
                "Orders Made: " + made,
                "Orders Missed: " + missed,
                "Total Score: " + score,
                ""
            );
            renderer.drawFrame(content, "Press Enter for next day...");
            scanner.nextLine();
        }

        public void showMessage(String msg) {
            renderer.drawFrame(Arrays.asList("", msg, ""), "Press Enter...");
            scanner.nextLine();
        }
        
        public void showStatistics(Player p) {
            renderer.drawFrame(Arrays.asList("PLAYER STATS", "Name: " + p.getUsername(), "Score: " + p.getTotalScore(), ""), "Press Enter...");
            scanner.nextLine();
        }
        
        public void showTutorial() {
            renderer.drawFrame(Arrays.asList("TUTORIAL", "1. Read Order", "2. Type ingredients separated by commas", "3. Press Enter", ""), "Press Enter...");
            scanner.nextLine();
        }
        
        public void showLeaderboard(List<Map.Entry<String, Integer>> entries) {
            List<String> content = new ArrayList<>();
            content.add("LEADERBOARD");
            content.add("");
            for(var e : entries) content.add(e.getKey() + " : " + e.getValue());
            content.add("");
            renderer.drawFrame(content, "Press Enter...");
            scanner.nextLine();
        }
        
        public void showAchievements(List<String> list) {
            List<String> content = new ArrayList<>();
            content.add("ACHIEVEMENTS");
            for(String s : list) content.add("* " + s);
            content.add("");
            renderer.drawFrame(content, "Press Enter...");
            scanner.nextLine();
        }
        
        public void showCredits() { showMessage("Created by Java Barista Team."); }
        public void showExitMessage() { showMessage("Thanks for playing!"); }
        
        public String getTextInput(String prompt) {
            // Draw an empty frame with just the prompt at the bottom
            renderer.drawFrame(Arrays.asList(""), prompt);
            return scanner.nextLine();
        }

        public int getNumericInput(int min, int max) {
            while (true) {
                try {
                    String in = scanner.nextLine();
                    int i = Integer.parseInt(in);
                    if (i >= min && i <= max) return i;
                } catch (Exception e) {}
                // If invalid, just wait for next input (or you could redraw)
                System.out.print("> ");
            }
        }
    }

    static class AnimationManager {
        private RenderSystem renderer = new RenderSystem();
        private UserInterface ui;
        
        public AnimationManager(UserInterface ui) { this.ui = ui; }
        
        public void playIntroCutscene() {
            List<String> frame = Arrays.asList(
                "   ___           _     _         ",
                "  | _ ) __ _ _ _(_)__| |_ __ _   ",
                "  | _ \\/ _` | '_| (_-<  _/ _` |  ",
                "  |___/\\__,_|_| |_/__/\\__\\__,_|  ",
                "",
                "The Ultimate Barista Simulator",
                "",
                "Loading..."
            );
            renderer.drawFrame(frame, "");
            delay(2000);
        }
        
        public void showCustomerArrival(Customer c) {
            List<String> frame = Arrays.asList(
                "The door opens...",
                "",
                "      ( ^_^ )      ",
                "       / | \\       ",
                "        / \\        ",
                "",
                c.getName() + " walks in."
            );
            renderer.drawFrame(frame, "");
            delay(1000);
        }
        
        public void showHappyCustomer() {
            List<String> frame = Arrays.asList(
                "",
                "   \\(^o^)/   ",
                "Thank you!!",
                ""
            );
            renderer.drawFrame(frame, "");
            delay(1000);
        }
        
        public void showUnhappyCustomer() {
            List<String> frame = Arrays.asList(
                "",
                "   (>_<)   ",
                "This isn't what I ordered!",
                ""
            );
            renderer.drawFrame(frame, "");
            delay(1000);
        }
        
        public void playDayTransition(int day) {
            renderer.drawFrame(Arrays.asList("", "", "DAY " + day, "", "The sun rises..."), "");
            delay(1500);
        }
        
        public void playEndingCutscene(int stars) {
            renderer.drawFrame(Arrays.asList("GAME OVER", "", "Rating: " + stars + " Stars", "", "Thank you for playing!"), "");
            delay(2000);
        }

        private void delay(int ms) { try { Thread.sleep(ms); } catch(Exception e) {} }
    }

    static class StoryManager {
        private RenderSystem renderer = new RenderSystem();
        private UserInterface ui;
        
        public StoryManager(UserInterface ui) { this.ui = ui; }

        public void showGameStartCutscene(String name) {
            List<String> content = Arrays.asList(
                "Welcome, " + name + ".",
                "You have just opened your first shop.",
                "The rent is due in 7 days.",
                "Make the best tea in the city.",
                ""
            );
            renderer.drawFrame(content, "Press Enter to open shop...");
            ui.scanner.nextLine();
        }
        public void showDayIntro(int day) {
            // Handled by AnimationManager
        }
    }

    // ==========================================
    //               DATA LAYER
    // ==========================================

    static class DataManager {
        private static final String PLAYER_FILE = "barista_players.dat";
        private static final String LEADERBOARD_FILE = "barista_leaderboard.dat";

        @SuppressWarnings("unchecked")
        public Player loadPlayer(String username) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PLAYER_FILE))) {
                Map<String, Player> map = (Map<String, Player>) ois.readObject();
                return map.get(username);
            } catch (Exception e) { return null; }
        }

        @SuppressWarnings("unchecked")
        public void savePlayer(Player p) throws IOException {
            Map<String, Player> map = new HashMap<>();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PLAYER_FILE))) {
                map = (Map<String, Player>) ois.readObject();
            } catch (Exception e) {}
            map.put(p.getUsername(), p);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PLAYER_FILE))) {
                oos.writeObject(map);
            }
        }

        @SuppressWarnings("unchecked")
        public Map<String, Integer> loadLeaderboard() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(LEADERBOARD_FILE))) {
                return (Map<String, Integer>) ois.readObject();
            } catch (Exception e) { return new HashMap<>(); }
        }

        public void saveLeaderboard(Map<String, Integer> map) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LEADERBOARD_FILE))) {
                oos.writeObject(map);
            } catch (Exception e) {}
        }
    }
}